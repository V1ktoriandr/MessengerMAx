package com.icq.server;

import com.icq.protocol.Message;
import com.icq.protocol.XMLMessageBuilder;
import com.icq.protocol.XMLMessageParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ServerCore serverCore;
    private PrintWriter writer;
    private String username;

    public ClientHandler(Socket socket, ServerCore serverCore) {
        this.socket = socket;
        this.serverCore = serverCore;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            String line;
            while ((line = reader.readLine()) != null) {
                Message message = XMLMessageParser.parse(line);
                if ("connect".equals(message.getType())) {
                    username = message.getUser();
                    boolean accepted = serverCore.registerClient(username, this);
                    if (!accepted) {
                        send(error("Username is empty or already online"));
                        break;
                    }
                } else if ("disconnect".equals(message.getType())) {
                    break;
                } else {
                    serverCore.process(message);
                }
            }
        } catch (Exception e) {
            send(error("Connection error: " + e.getMessage()));
        } finally {
            close();
            serverCore.unregisterClient(username);
        }
    }

    public synchronized void send(String xml) {
        if (writer != null) {
            writer.println(xml);
        }
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private String error(String text) {
        Message message = new Message();
        message.setType("error");
        message.setText(text);
        return XMLMessageBuilder.build(message);
    }
}
