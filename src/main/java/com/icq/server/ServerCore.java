package com.icq.server;

import com.icq.database.DatabaseManager;
import com.icq.database.MessageDAO;
import com.icq.database.UserDAO;
import com.icq.model.ChatMessage;
import com.icq.protocol.Message;
import com.icq.protocol.XMLMessageBuilder;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
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
    private final MessageDAO messageDAO;
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
        this.messageDAO = new MessageDAO(databaseManager);
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

    public void process(Message message) {
        switch (message.getType()) {
            case "chat" -> processChat(message);
            case "history_request" -> sendHistory(message.getFrom(), message.getTo());
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
        messageDAO.save(chatMessage);
        dialogs.add(dialogKey(chatMessage.getSender(), chatMessage.getReceiver()));
        messageLogger.accept(chatMessage);
        dialogsListener.accept(sorted(dialogs));

        String xml = XMLMessageBuilder.build(Message.chat(
                chatMessage.getSender(),
                chatMessage.getReceiver(),
                chatMessage.getMessage(),
                chatMessage.getSendTime()
        ));
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
