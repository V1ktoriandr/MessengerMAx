package com.icq.server;

import com.icq.database.DatabaseManager;
import com.icq.database.GroupDAO;
import com.icq.database.MessageDAO;
import com.icq.database.PasswordResetDAO;
import com.icq.database.UserDAO;
import com.icq.database.VoiceMessageDAO;
import com.icq.model.ChatMessage;
import com.icq.model.GroupChat;
import com.icq.model.VoiceMessage;
import com.icq.protocol.Message;
import com.icq.protocol.XMLMessageBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ServerCore {
    public static final int PORT = 5000;

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final Set<String> dialogs = ConcurrentHashMap.newKeySet();
    private final UserDAO userDAO;
    private final GroupDAO groupDAO;
    private final MessageDAO messageDAO;
    private final VoiceMessageDAO voiceMessageDAO;
    private final PasswordResetDAO passwordResetDAO;
    private final AuthService authService;
    private final MailService mailService = new MailService();
    private final Consumer<String> eventLogger;
    private final Consumer<ChatMessage> messageLogger;
    private final Consumer<List<String>> onlineUsersListener;
    private final Consumer<List<String>> dialogsListener;

    private volatile boolean running;
    private ServerSocket serverSocket;

    public ServerCore(Consumer<String> eventLogger,
                      Consumer<ChatMessage> messageLogger,
                      Consumer<List<String>> onlineUsersListener,
                      Consumer<List<String>> dialogsListener) {
        DatabaseManager databaseManager = new DatabaseManager();
        this.userDAO = new UserDAO(databaseManager);
        this.groupDAO = new GroupDAO(databaseManager);
        this.messageDAO = new MessageDAO(databaseManager);
        this.voiceMessageDAO = new VoiceMessageDAO(databaseManager);
        this.passwordResetDAO = new PasswordResetDAO(databaseManager);
        this.authService = new AuthService(userDAO);
        this.eventLogger = eventLogger;
        this.messageLogger = messageLogger;
        this.onlineUsersListener = onlineUsersListener;
        this.dialogsListener = dialogsListener;
        restoreDialogsFromDatabase();
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        executorService.submit(this::acceptLoop);
    }

    private void acceptLoop() {
        try (ServerSocket socket = new ServerSocket(PORT)) {
            serverSocket = socket;
            eventLogger.accept("Server started on port " + PORT);
            while (running) {
                Socket clientSocket = socket.accept();
                eventLogger.accept("New socket connection: " + clientSocket.getRemoteSocketAddress());
                executorService.submit(new ClientHandler(clientSocket, this));
            }
        } catch (IOException e) {
            if (running) {
                eventLogger.accept("Server error: " + e.getMessage());
            }
        }
    }

    public synchronized boolean registerClient(String username, ClientHandler handler) {
        if (username == null || username.isBlank() || clients.containsKey(username)) {
            return false;
        }
        userDAO.saveIfAbsent(username);
        clients.put(username, handler);
        eventLogger.accept(username + " connected");
        broadcastUsers();
        return true;
    }

    public boolean registerUser(String username, String password, String avatarPath, String email) {
        boolean created = authService.register(username, password, avatarPath, email);
        eventLogger.accept(created ? username + " registered" : "Registration failed for " + username);
        if (created) {
            mailService.sendRegistrationMail(email, username);
        }
        return created;
    }

    public synchronized boolean loginClient(String username, String password, ClientHandler handler) {
        if (username == null || username.isBlank() || clients.containsKey(username)) {
            return false;
        }
        if (!authService.authenticate(username, password)) {
            eventLogger.accept("Login failed for " + username);
            return false;
        }
        String resolvedUsername = authService.resolveUsername(username);
        handler.setUsername(resolvedUsername);
        clients.put(resolvedUsername, handler);
        eventLogger.accept(resolvedUsername + " logged in");
        return true;
    }

    public void broadcastState() {
        broadcastUsers();
        broadcastProfiles();
    }

    public void process(Message message) {
        switch (message.getType()) {
            case "chat" -> processChat(message);
            case "voice" -> processVoice(message);
            case "file" -> processFile(message);
            case "group_create" -> createGroup(message);
            case "group_chat" -> processGroupChat(message);
            case "group_leave" -> leaveGroup(message);
            case "group_remove_member" -> removeGroupMember(message);
            case "typing" -> sendTo(message.getTo(), XMLMessageBuilder.build(message));
            case "history_request" -> sendHistory(message.getFrom(), message.getTo());
            case "delete_dialog_local" -> hideDialog(message);
            case "delete_dialog_all" -> deleteDialogForAll(message);
            case "delete_message_all" -> deleteMessageForAll(message);
            case "profile_update" -> updateProfile(message);
            case "password_reset_request" -> requestPasswordReset(message);
            case "password_reset_confirm" -> confirmPasswordReset(message);
            default -> eventLogger.accept("Unsupported message type: " + message.getType());
        }
    }

    private void processChat(Message message) {
        ChatMessage chatMessage = new ChatMessage(
                message.getFrom(),
                message.getTo(),
                message.getText(),
                message.getTime() == null ? LocalDateTime.now() : message.getTime()
        );
        int id = messageDAO.save(chatMessage);
        dialogs.add(dialogKey(chatMessage.getSender(), chatMessage.getReceiver()));
        messageLogger.accept(chatMessage);
        dialogsListener.accept(sorted(dialogs));

        Message outbound = Message.chat(
                chatMessage.getSender(),
                chatMessage.getReceiver(),
                chatMessage.getMessage(),
                chatMessage.getSendTime()
        );
        outbound.setMessageId(id);
        String xml = XMLMessageBuilder.build(outbound);
        sendTo(chatMessage.getReceiver(), xml);
        sendTo(chatMessage.getSender(), xml);
    }

    private void sendHistory(String firstUser, String secondUser) {
        ClientHandler handler = clients.get(firstUser);
        if (handler != null) {
            handler.send(XMLMessageBuilder.history(messageDAO.findConversation(firstUser, secondUser)));
            eventLogger.accept("History sent to " + firstUser + " for dialog with " + secondUser);
        }
    }

    private void processVoice(Message message) {
        try {
            LocalDateTime time = message.getTime() == null ? LocalDateTime.now() : message.getTime();
            String filePath = saveVoiceFile(message, time);
            ChatMessage chatMessage = ChatMessage.voice(message.getFrom(), message.getTo(), filePath, message.getDuration(), time);
            int id = messageDAO.save(chatMessage);
            voiceMessageDAO.save(new VoiceMessage(0, message.getFrom(), message.getTo(), filePath, message.getDuration(), time));
            dialogs.add(dialogKey(message.getFrom(), message.getTo()));
            messageLogger.accept(chatMessage);
            dialogsListener.accept(sorted(dialogs));

            Message outbound = Message.voice(message.getFrom(), message.getTo(), filePath, message.getFileData(), message.getDuration(), time);
            outbound.setMessageId(id);
            String xml = XMLMessageBuilder.build(outbound);
            sendTo(message.getTo(), xml);
            sendTo(message.getFrom(), xml);
        } catch (Exception e) {
            eventLogger.accept("Voice message error: " + e.getMessage());
        }
    }

    private void processFile(Message message) {
        try {
            LocalDateTime time = message.getTime() == null ? LocalDateTime.now() : message.getTime();
            String filePath = saveBinaryFile(message, time, "files");
            ChatMessage chatMessage = ChatMessage.file(
                    message.getFrom(),
                    message.getTo(),
                    filePath,
                    message.getFileName(),
                    message.getFileSize(),
                    message.getMessageType() == null || message.getMessageType().isBlank() ? "file" : message.getMessageType(),
                    time
            );
            int id = messageDAO.save(chatMessage);
            dialogs.add(dialogKey(message.getFrom(), message.getTo()));
            messageLogger.accept(chatMessage);
            dialogsListener.accept(sorted(dialogs));

            Message outbound = Message.file(message.getFrom(), message.getTo(), filePath, message.getFileName(),
                    message.getFileData(), message.getFileSize(), chatMessage.getMessageType(), time);
            outbound.setMessageId(id);
            String xml = XMLMessageBuilder.build(outbound);
            sendTo(message.getTo(), xml);
            sendTo(message.getFrom(), xml);
        } catch (Exception e) {
            eventLogger.accept("File message error: " + e.getMessage());
        }
    }

    private void createGroup(Message message) {
        int groupId = groupDAO.createGroup(message.getGroupName(), message.getGroupAvatarPath(), message.getFrom(), message.getMembers());
        eventLogger.accept(message.getFrom() + " created group " + message.getGroupName());
        Message notice = new Message();
        notice.setType("group_invite");
        notice.setGroupId(groupId);
        notice.setGroupName(message.getGroupName());
        notice.setGroupAvatarPath(message.getGroupAvatarPath());
        String xml = XMLMessageBuilder.build(notice);
        for (String member : groupDAO.findMembers(groupId)) {
            sendTo(member, xml);
            sendGroupsForUser(member);
        }
    }

    private void processGroupChat(Message message) {
        LocalDateTime time = message.getTime() == null ? LocalDateTime.now() : message.getTime();
        ChatMessage chatMessage = new ChatMessage(message.getFrom(), "group:" + message.getGroupId(), message.getText(), time);
        int id = groupDAO.saveGroupMessage(message.getGroupId(), chatMessage);
        Message outbound = Message.groupChat(message.getFrom(), message.getGroupId(), message.getText(), time);
        outbound.setMessageId(id);
        String xml = XMLMessageBuilder.build(outbound);
        for (String member : groupDAO.findMembers(message.getGroupId())) {
            sendTo(member, xml);
        }
    }

    private void leaveGroup(Message message) {
        groupDAO.removeMember(message.getGroupId(), message.getFrom());
        sendGroupsForUser(message.getFrom());
    }

    private void removeGroupMember(Message message) {
        String member = message.getUser();
        groupDAO.removeMember(message.getGroupId(), member);
        sendGroupsForUser(member);
    }

    private void sendGroupsForUser(String username) {
        sendTo(username, XMLMessageBuilder.groups(groupDAO.findGroupsForUser(username)));
    }

    private String saveVoiceFile(Message message, LocalDateTime time) throws IOException {
        Path directory = Path.of("voice_messages");
        Files.createDirectories(directory);
        String safeName = (message.getFrom() + "_" + message.getTo() + "_" + time.toString())
                .replaceAll("[^a-zA-Z0-9._-]", "_") + ".wav";
        Path target = directory.resolve(safeName);
        Files.write(target, Base64.getDecoder().decode(message.getFileData()));
        return target.toAbsolutePath().toString();
    }

    private String saveBinaryFile(Message message, LocalDateTime time, String folder) throws IOException {
        Path directory = Path.of(folder);
        Files.createDirectories(directory);
        String name = message.getFileName() == null ? "file.bin" : message.getFileName();
        String safeName = (message.getFrom() + "_" + message.getTo() + "_" + time + "_" + name)
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = directory.resolve(safeName);
        Files.write(target, Base64.getDecoder().decode(message.getFileData()));
        return target.toAbsolutePath().toString();
    }

    private void hideDialog(Message message) {
        messageDAO.hideDialogForUser(message.getFrom(), message.getTo());
        sendTo(message.getFrom(), XMLMessageBuilder.build(status("dialog_deleted", message.getTo(), "Dialog hidden")));
    }

    private void deleteDialogForAll(Message message) {
        messageDAO.deleteConversation(message.getFrom(), message.getTo());
        Message notice = status("dialog_deleted", message.getTo(), "Dialog deleted");
        notice.setFrom(message.getFrom());
        notice.setTo(message.getTo());
        String xml = XMLMessageBuilder.build(notice);
        sendTo(message.getFrom(), xml);
        sendTo(message.getTo(), xml);
    }

    private void deleteMessageForAll(Message message) {
        messageDAO.deleteMessage(message.getMessageId());
        String xml = XMLMessageBuilder.build(message);
        sendTo(message.getFrom(), xml);
        sendTo(message.getTo(), xml);
    }

    private void updateProfile(Message message) {
        String newName = message.getNewUsername() == null || message.getNewUsername().isBlank() ? message.getFrom() : message.getNewUsername();
        boolean ok = authService.updateProfile(message.getFrom(), newName, message.getAvatarPath(), message.getEmail(), message.getPassword());
        Message response = status(ok ? "profile_updated" : "profile_error", ok ? newName : message.getFrom(), ok ? "Profile updated" : "Cannot update profile");
        sendTo(message.getFrom(), XMLMessageBuilder.build(response));
        if (ok) {
            ClientHandler handler = clients.remove(message.getFrom());
            if (handler != null) {
                clients.put(newName, handler);
                handler.setUsername(newName);
            }
            broadcastState();
        }
    }

    public Message requestPasswordReset(Message message) {
        String code = String.valueOf((int) (100000 + Math.random() * 900000));
        passwordResetDAO.saveCode(message.getEmail(), code, LocalDateTime.now().plusMinutes(15));
        mailService.sendPasswordResetCode(message.getEmail(), code);
        Message response = status("password_reset_code_sent", message.getEmail(), "Verification code sent");
        if (message.getFrom() != null) {
            sendTo(message.getFrom(), XMLMessageBuilder.build(response));
        }
        return response;
    }

    public Message confirmPasswordReset(Message message) {
        boolean ok = passwordResetDAO.verifyCode(message.getEmail(), message.getCode())
                && authService.updatePasswordByEmail(message.getEmail(), message.getPassword());
        Message response = status(ok ? "password_reset_success" : "password_reset_error", message.getEmail(), ok ? "Password changed" : "Invalid or expired code");
        if (message.getFrom() != null) {
            sendTo(message.getFrom(), XMLMessageBuilder.build(response));
        }
        return response;
    }

    private Message status(String type, String user, String text) {
        Message message = new Message();
        message.setType(type);
        message.setUser(user);
        message.setText(text);
        return message;
    }

    private void sendTo(String username, String xml) {
        ClientHandler handler = clients.get(username);
        if (handler != null) {
            handler.send(xml);
        }
    }

    public void unregisterClient(String username) {
        if (username != null && clients.remove(username) != null) {
            eventLogger.accept(username + " disconnected");
            broadcastUsers();
        }
    }

    private void broadcastUsers() {
        List<String> online = new ArrayList<>(clients.keySet());
        Collections.sort(online);
        String xml = XMLMessageBuilder.users(online);
        clients.values().forEach(client -> client.send(xml));
        onlineUsersListener.accept(online);
    }

    private void broadcastProfiles() {
        String xml = XMLMessageBuilder.profiles(userDAO.findAll());
        clients.values().forEach(client -> client.send(xml));
        clients.keySet().forEach(this::sendGroupsForUser);
    }

    public void stop() {
        running = false;
        clients.values().forEach(ClientHandler::close);
        executorService.shutdownNow();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public List<ChatMessage> allMessages() {
        return messageDAO.findAll();
    }

    private void restoreDialogsFromDatabase() {
        for (ChatMessage message : messageDAO.findAll()) {
            dialogs.add(dialogKey(message.getSender(), message.getReceiver()));
        }
        dialogsListener.accept(sorted(dialogs));
    }

    private String dialogKey(String first, String second) {
        return first.compareToIgnoreCase(second) <= 0 ? first + " - " + second : second + " - " + first;
    }

    private List<String> sorted(Set<String> values) {
        return new ArrayList<>(new TreeSet<>(values));
    }
}
