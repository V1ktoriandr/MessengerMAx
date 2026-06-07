package com.icq.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String JDBC_URL = "jdbc:sqlite:icq.db";

    public DatabaseManager() {
        initializeDatabase();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }

    public void initializeDatabase() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT UNIQUE NOT NULL,
                        password_hash TEXT NOT NULL DEFAULT '',
                        avatar_path TEXT,
                        email TEXT UNIQUE,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        last_login DATETIME
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS messages(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        sender TEXT NOT NULL,
                        receiver TEXT NOT NULL,
                        message TEXT NOT NULL,
                        send_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                        message_type TEXT NOT NULL DEFAULT 'text',
                        file_path TEXT,
                        file_name TEXT,
                        file_size INTEGER DEFAULT 0,
                        duration INTEGER DEFAULT 0
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS voice_messages(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        sender TEXT,
                        receiver TEXT,
                        file_path TEXT,
                        duration INTEGER,
                        send_time DATETIME DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS password_resets(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        email TEXT NOT NULL,
                        code TEXT NOT NULL,
                        expires_at DATETIME NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS hidden_dialogs(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL,
                        companion TEXT NOT NULL,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(username, companion)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS groups(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        avatar_path TEXT,
                        owner TEXT NOT NULL,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS group_members(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        group_id INTEGER NOT NULL,
                        username TEXT NOT NULL,
                        role TEXT DEFAULT 'member',
                        joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(group_id, username)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS group_messages(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        group_id INTEGER NOT NULL,
                        sender TEXT NOT NULL,
                        message TEXT NOT NULL,
                        message_type TEXT DEFAULT 'text',
                        file_path TEXT,
                        file_name TEXT,
                        file_size INTEGER DEFAULT 0,
                        duration INTEGER DEFAULT 0,
                        send_time DATETIME DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            addColumnIfMissing(statement, "users", "password_hash", "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(statement, "users", "avatar_path", "TEXT");
            addColumnIfMissing(statement, "users", "email", "TEXT");
            addColumnIfMissing(statement, "users", "created_at", "DATETIME");
            addColumnIfMissing(statement, "users", "last_login", "DATETIME");
            statement.executeUpdate("UPDATE users SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL");
            addColumnIfMissing(statement, "messages", "message_type", "TEXT NOT NULL DEFAULT 'text'");
            addColumnIfMissing(statement, "messages", "file_path", "TEXT");
            addColumnIfMissing(statement, "messages", "file_name", "TEXT");
            addColumnIfMissing(statement, "messages", "file_size", "INTEGER DEFAULT 0");
            addColumnIfMissing(statement, "messages", "duration", "INTEGER DEFAULT 0");
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot initialize SQLite database", e);
        }
    }

    private void addColumnIfMissing(Statement statement, String table, String column, String definition) throws SQLException {
        try {
            statement.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        } catch (SQLException e) {
            if (!e.getMessage().toLowerCase().contains("duplicate column")) {
                throw e;
            }
        }
    }
}
