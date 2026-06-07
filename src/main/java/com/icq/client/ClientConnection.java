package com.icq.client;

import com.icq.protocol.Message;
import com.icq.protocol.XMLMessageBuilder;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

public class ClientConnection {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;
    private Consumer<Message> messageConsumer;
    private Consumer<String> errorConsumer;

    public void connect(String host, String username) throws IOException {
        this.username = username;
        socket = new Socket(host, 5000);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        send(XMLMessageBuilder.build(Message.connect(username)));
    }

    public void login(String host, String username, String password) throws IOException {
        this.username = username;
        socket = new Socket(host, 5000);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        send(XMLMessageBuilder.build(Message.login(username, password)));
        Message response = readResponse();
        if (!"auth_success".equals(response.getType())) {
            closeSocket();
            throw new IOException(response.getText() == null ? "Authentication failed" : response.getText());
        }
        if (response.getUser() != null && !response.getUser().isBlank()) {
            this.username = response.getUser();
        }
    }

    public static void register(String host, String username, String password, String avatarPath) throws IOException {
        register(host, username, password, avatarPath, null);
    }

    public static void register(String host, String username, String password, String avatarPath, String email) throws IOException {
        try (Socket socket = new Socket(host, 5000);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
            writer.println(XMLMessageBuilder.build(Message.register(username, password, avatarPath, email)));
            Message response = com.icq.protocol.XMLMessageParser.parse(reader.readLine());
            if (!"register_success".equals(response.getType())) {
                throw new IOException(response.getText() == null ? "Registration failed" : response.getText());
            }
        }
    }

    public void startReceiver() throws IOException {
        Thread thread = new Thread(new MessageReceiver(reader, socket, messageConsumer, errorConsumer), "message-receiver-" + username);
        thread.setDaemon(true);
        thread.start();
    }

    public void sendChat(String to, String text) {
        send(XMLMessageBuilder.build(Message.chat(username, to, text, LocalDateTime.now())));
    }

    public void sendGroupChat(int groupId, String text) {
        send(XMLMessageBuilder.build(Message.groupChat(username, groupId, text, LocalDateTime.now())));
    }

    public void createGroup(String name, String avatarPath, List<String> members) {
        send(XMLMessageBuilder.build(Message.groupCreate(username, name, avatarPath, members)));
    }

    public void leaveGroup(int groupId) {
        Message message = new Message();
        message.setType("group_leave");
        message.setFrom(username);
        message.setGroupId(groupId);
        send(XMLMessageBuilder.build(message));
    }

    public void removeGroupMember(int groupId, String member) {
        Message message = new Message();
        message.setType("group_remove_member");
        message.setFrom(username);
        message.setUser(member);
        message.setGroupId(groupId);
        send(XMLMessageBuilder.build(message));
    }

    public void requestHistory(String companion) {
        send(XMLMessageBuilder.build(Message.historyRequest(username, companion)));
    }

    public void sendTyping(String to) {
        send(XMLMessageBuilder.build(Message.typing(username, to)));
    }

    public void sendVoice(String to, Path wavFile, int duration) throws IOException {
        String data = Base64.getEncoder().encodeToString(Files.readAllBytes(wavFile));
        send(XMLMessageBuilder.build(Message.voice(username, to, wavFile.toAbsolutePath().toString(), data, duration, LocalDateTime.now())));
    }

    public void sendFile(String to, Path file) throws IOException {
        String data = Base64.getEncoder().encodeToString(Files.readAllBytes(file));
        String fileName = file.getFileName().toString();
        String type = isImage(fileName) ? "image" : "file";
        send(XMLMessageBuilder.build(Message.file(username, to, file.toAbsolutePath().toString(), fileName, data, Files.size(file), type, LocalDateTime.now())));
    }

    public void deleteDialogLocal(String companion) {
        Message message = new Message();
        message.setType("delete_dialog_local");
        message.setFrom(username);
        message.setTo(companion);
        send(XMLMessageBuilder.build(message));
    }

    public void deleteDialogForAll(String companion) {
        Message message = new Message();
        message.setType("delete_dialog_all");
        message.setFrom(username);
        message.setTo(companion);
        send(XMLMessageBuilder.build(message));
    }

    public void deleteMessageForAll(String companion, int messageId) {
        Message message = new Message();
        message.setType("delete_message_all");
        message.setFrom(username);
        message.setTo(companion);
        message.setMessageId(messageId);
        send(XMLMessageBuilder.build(message));
    }

    public void updateProfile(String newUsername, String avatarPath, String email, String newPassword) {
        Message message = new Message();
        message.setType("profile_update");
        message.setFrom(username);
        message.setNewUsername(newUsername);
        message.setAvatarPath(avatarPath);
        message.setEmail(email);
        message.setPassword(newPassword);
        send(XMLMessageBuilder.build(message));
        if (newUsername != null && !newUsername.isBlank()) {
            username = newUsername;
        }
    }

    public static void requestPasswordReset(String host, String email) throws IOException {
        sendOneShot(host, message -> {
            message.setType("password_reset_request");
            message.setEmail(email);
        }, "password_reset_code_sent");
    }

    public static void confirmPasswordReset(String host, String email, String code, String password) throws IOException {
        sendOneShot(host, message -> {
            message.setType("password_reset_confirm");
            message.setEmail(email);
            message.setCode(code);
            message.setPassword(password);
        }, "password_reset_success");
    }

    public void disconnect() {
        try {
            if (writer != null) {
                send(XMLMessageBuilder.build(Message.disconnect(username)));
            }
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ignored) {
            }
        }
    }

    private Message readResponse() throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("Server closed connection");
        }
        return com.icq.protocol.XMLMessageParser.parse(line);
    }

    private void closeSocket() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private synchronized void send(String xml) {
        if (writer != null) {
            writer.println(xml);
        }
    }

    public String getUsername() {
        return username;
    }

    private static void sendOneShot(String host, java.util.function.Consumer<Message> customizer, String successType) throws IOException {
        try (Socket socket = new Socket(host, 5000);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
            Message request = new Message();
            customizer.accept(request);
            writer.println(XMLMessageBuilder.build(request));
            Message response = com.icq.protocol.XMLMessageParser.parse(reader.readLine());
            if (!successType.equals(response.getType())) {
                throw new IOException(response.getText() == null ? "Operation failed" : response.getText());
            }
        }
    }

    private boolean isImage(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".bmp");
    }

    public void setMessageConsumer(Consumer<Message> messageConsumer) {
        this.messageConsumer = messageConsumer;
    }

    public void setErrorConsumer(Consumer<String> errorConsumer) {
        this.errorConsumer = errorConsumer;
    }
}
