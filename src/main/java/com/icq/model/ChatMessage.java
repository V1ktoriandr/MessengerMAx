package com.icq.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatMessage {
    private final int id;
    private final String sender;
    private final String receiver;
    private final String message;
    private final LocalDateTime sendTime;
    private final String messageType;
    private final String filePath;
    private final String fileName;
    private final long fileSize;
    private final int duration;

    public ChatMessage(int id, String sender, String receiver, String message, LocalDateTime sendTime) {
        this(id, sender, receiver, message, sendTime, "text", null, null, 0, 0);
    }

    public ChatMessage(String sender, String receiver, String message, LocalDateTime sendTime) {
        this(0, sender, receiver, message, sendTime);
    }

    public ChatMessage(int id, String sender, String receiver, String message, LocalDateTime sendTime,
                       String messageType, String filePath, int duration) {
        this(id, sender, receiver, message, sendTime, messageType, filePath, null, 0, duration);
    }

    public ChatMessage(int id, String sender, String receiver, String message, LocalDateTime sendTime,
                       String messageType, String filePath, String fileName, long fileSize, int duration) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.sendTime = sendTime;
        this.messageType = messageType == null ? "text" : messageType;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.duration = duration;
    }

    public static ChatMessage voice(String sender, String receiver, String filePath, int duration, LocalDateTime sendTime) {
        return new ChatMessage(0, sender, receiver, "Voice message", sendTime, "voice", filePath, "voice.wav", 0, duration);
    }

    public static ChatMessage file(String sender, String receiver, String filePath, String fileName, long fileSize, String type, LocalDateTime sendTime) {
        return new ChatMessage(0, sender, receiver, fileName, sendTime, type, filePath, fileName, fileSize, 0);
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

    public String getMessageType() {
        return messageType;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public int getDuration() {
        return duration;
    }

    public boolean isVoice() {
        return "voice".equals(messageType);
    }

    public boolean isFile() {
        return "file".equals(messageType) || "image".equals(messageType);
    }

    public boolean isImage() {
        return "image".equals(messageType);
    }

    public String formattedTime() {
        return sendTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
