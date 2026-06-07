package com.icq.protocol;

import com.icq.model.ChatMessage;
import com.icq.model.GroupChat;
import com.icq.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Message {
    private String type;
    private String user;
    private String from;
    private String to;
    private String text;
    private String password;
    private String avatarPath;
    private String email;
    private String code;
    private String newUsername;
    private String filePath;
    private String fileName;
    private String fileData;
    private String messageType;
    private long fileSize;
    private int messageId;
    private int groupId;
    private String groupName;
    private String groupAvatarPath;
    private int duration;
    private LocalDateTime time;
    private final List<String> users = new ArrayList<>();
    private final List<String> members = new ArrayList<>();
    private final List<User> profiles = new ArrayList<>();
    private final List<GroupChat> groups = new ArrayList<>();
    private final List<ChatMessage> history = new ArrayList<>();

    public static Message connect(String user) {
        Message message = new Message();
        message.setType("connect");
        message.setUser(user);
        return message;
    }

    public static Message login(String user, String password) {
        Message message = new Message();
        message.setType("login");
        message.setUser(user);
        message.setPassword(password);
        return message;
    }

    public static Message register(String user, String password, String avatarPath) {
        return register(user, password, avatarPath, null);
    }

    public static Message register(String user, String password, String avatarPath, String email) {
        Message message = new Message();
        message.setType("register");
        message.setUser(user);
        message.setPassword(password);
        message.setAvatarPath(avatarPath);
        message.setEmail(email);
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

    public static Message voice(String from, String to, String filePath, String fileData, int duration, LocalDateTime time) {
        Message message = new Message();
        message.setType("voice");
        message.setFrom(from);
        message.setTo(to);
        message.setText("Voice message");
        message.setFilePath(filePath);
        message.setFileData(fileData);
        message.setDuration(duration);
        message.setTime(time);
        return message;
    }

    public static Message file(String from, String to, String filePath, String fileName, String fileData, long fileSize, String messageType, LocalDateTime time) {
        Message message = new Message();
        message.setType("file");
        message.setFrom(from);
        message.setTo(to);
        message.setText(fileName);
        message.setFilePath(filePath);
        message.setFileName(fileName);
        message.setFileData(fileData);
        message.setFileSize(fileSize);
        message.setMessageType(messageType);
        message.setTime(time);
        return message;
    }

    public static Message typing(String from, String to) {
        Message message = new Message();
        message.setType("typing");
        message.setFrom(from);
        message.setTo(to);
        return message;
    }

    public static Message groupCreate(String from, String groupName, String avatarPath, List<String> members) {
        Message message = new Message();
        message.setType("group_create");
        message.setFrom(from);
        message.setGroupName(groupName);
        message.setGroupAvatarPath(avatarPath);
        message.getMembers().addAll(members);
        return message;
    }

    public static Message groupChat(String from, int groupId, String text, LocalDateTime time) {
        Message message = new Message();
        message.setType("group_chat");
        message.setFrom(from);
        message.setGroupId(groupId);
        message.setText(text);
        message.setTime(time);
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNewUsername() {
        return newUsername;
    }

    public void setNewUsername(String newUsername) {
        this.newUsername = newUsername;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileData() {
        return fileData;
    }

    public void setFileData(String fileData) {
        this.fileData = fileData;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupAvatarPath() {
        return groupAvatarPath;
    }

    public void setGroupAvatarPath(String groupAvatarPath) {
        this.groupAvatarPath = groupAvatarPath;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
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

    public List<String> getMembers() {
        return members;
    }

    public List<User> getProfiles() {
        return profiles;
    }

    public List<GroupChat> getGroups() {
        return groups;
    }

    public List<ChatMessage> getHistory() {
        return history;
    }
}
