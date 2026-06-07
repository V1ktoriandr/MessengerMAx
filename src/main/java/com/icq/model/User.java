package com.icq.model;

import java.time.LocalDateTime;

public class User {
    private final int id;
    private final String username;
    private final String passwordHash;
    private final String avatarPath;
    private final String email;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastLogin;

    public User(int id, String username, String passwordHash, String avatarPath, String email, LocalDateTime createdAt, LocalDateTime lastLogin) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.avatarPath = avatarPath;
        this.email = email;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public String getEmail() {
        return email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
}
