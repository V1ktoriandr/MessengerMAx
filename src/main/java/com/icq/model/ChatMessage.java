package com.icq.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatMessage {
    private final int id;
    private final String sender;
    private final String receiver;
    private final String message;
    private final LocalDateTime sendTime;

    public ChatMessage(int id, String sender, String receiver, String message, LocalDateTime sendTime) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.sendTime = sendTime;
    }

    public ChatMessage(String sender, String receiver, String message, LocalDateTime sendTime) {
        this(0, sender, receiver, message, sendTime);
    }

    public int getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getSendTime() {
        return sendTime;
    }

    public String formattedTime() {
        return sendTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
