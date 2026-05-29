package com.icq.model;

import java.time.LocalDateTime;

public class User {
    private final int id;
    private final String username;
    private final LocalDateTime registrationTime;

    public User(int id, String username, LocalDateTime registrationTime) {
        this.id = id;
        this.username = username;
        this.registrationTime = registrationTime;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getRegistrationTime() {
        return registrationTime;
    }
}
