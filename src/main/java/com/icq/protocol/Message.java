package com.icq.protocol;

import com.icq.model.ChatMessage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Message {
    private String type;
    private String user;
    private String from;
    private String to;
    private String text;
    private LocalDateTime time;
    private final List<String> users = new ArrayList<>();
    private final List<ChatMessage> history = new ArrayList<>();

    public static Message connect(String user) {
        Message message = new Message();
        message.setType("connect");
        message.setUser(user);
        return message;
    }

    public static Message disconnect(String user) {
        Message message = new Message();
        message.setType("disconnect");
        message.setUser(user);
        return message;
    }

    public static Message chat(String from, String to, String text, LocalDateTime time) {
        Message message = new Message();
        message.setType("chat");
        message.setFrom(from);
        message.setTo(to);
        message.setText(text);
        message.setTime(time);
        return message;
    }

    public static Message historyRequest(String from, String to) {
        Message message = new Message();
        message.setType("history_request");
        message.setFrom(from);
        message.setTo(to);
        return message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public List<String> getUsers() {
        return users;
    }

    public List<ChatMessage> getHistory() {
        return history;
    }
}
