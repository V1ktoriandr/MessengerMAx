package com.icq.client;

import com.icq.protocol.Message;
import com.icq.protocol.XMLMessageParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class MessageReceiver implements Runnable {
    private final Socket socket;
    private final BufferedReader externalReader;
    private final Consumer<Message> messageConsumer;
    private final Consumer<String> errorConsumer;

    public MessageReceiver(Socket socket, Consumer<Message> messageConsumer, Consumer<String> errorConsumer) {
        this.socket = socket;
        this.externalReader = null;
        this.messageConsumer = messageConsumer;
        this.errorConsumer = errorConsumer;
    }

    public MessageReceiver(BufferedReader reader, Socket socket, Consumer<Message> messageConsumer, Consumer<String> errorConsumer) {
        this.socket = socket;
        this.externalReader = reader;
        this.messageConsumer = messageConsumer;
        this.errorConsumer = errorConsumer;
    }

    @Override
    public void run() {
        try (BufferedReader reader = externalReader == null
                ? new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
                : externalReader) {
            String line;
            while ((line = reader.readLine()) != null) {
                Message message = XMLMessageParser.parse(line);
                if (messageConsumer != null) {
                    messageConsumer.accept(message);
                }
            }
        } catch (Exception e) {
            if (errorConsumer != null && !socket.isClosed()) {
                errorConsumer.accept(e.getMessage());
            }
        }
    }
}
