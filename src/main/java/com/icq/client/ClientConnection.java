package com.icq.client;

import com.icq.protocol.Message;
import com.icq.protocol.XMLMessageBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.function.Consumer;

public class ClientConnection {
    private Socket socket;
    private PrintWriter writer;
    private String username;
    private Consumer<Message> messageConsumer;
    private Consumer<String> errorConsumer;

    public void connect(String host, String username) throws IOException {
        this.username = username;
        socket = new Socket(host, 5000);
        writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        send(XMLMessageBuilder.build(Message.connect(username)));
    }

    public void startReceiver() throws IOException {
        Thread thread = new Thread(new MessageReceiver(socket, messageConsumer, errorConsumer), "message-receiver-" + username);
        thread.setDaemon(true);
        thread.start();
    }

    public void sendChat(String to, String text) {
        send(XMLMessageBuilder.build(Message.chat(username, to, text, LocalDateTime.now())));
    }

    public void requestHistory(String companion) {
        send(XMLMessageBuilder.build(Message.historyRequest(username, companion)));
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

    private synchronized void send(String xml) {
        if (writer != null) {
            writer.println(xml);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setMessageConsumer(Consumer<Message> messageConsumer) {
        this.messageConsumer = messageConsumer;
    }

    public void setErrorConsumer(Consumer<String> errorConsumer) {
        this.errorConsumer = errorConsumer;
    }
}
