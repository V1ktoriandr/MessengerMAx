package com.icq.database;

import com.icq.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.List;

public class UserDAO {
    private final DatabaseManager databaseManager;

    public UserDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void saveIfAbsent(String username) {
        String sql = "INSERT OR IGNORE INTO users(username, password_hash) VALUES(?, '')";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot save user " + username, e);
        }
    }

    public boolean createUser(String username, String passwordHash, String avatarPath) {
        return createUser(username, passwordHash, avatarPath, null);
    }

    public boolean createUser(String username, String passwordHash, String avatarPath, String email) {
        String sql = "INSERT INTO users(username, password_hash, avatar_path, email) VALUES(?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, avatarPath);
            statement.setString(4, email);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getMessage().toLowerCase().contains("unique")) {
                return false;
            }
            throw new IllegalStateException("Cannot create user " + username, e);
        }
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, avatar_path, email, created_at, last_login FROM users WHERE username = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(readUser(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot find user " + username, e);
        }
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, username, password_hash, avatar_path, email, created_at, last_login FROM users WHERE email = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(readUser(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot find user by email", e);
        }
    }

    public boolean updateProfile(String oldUsername, String newUsername, String avatarPath, String email, String passwordHash) {
        String sql = """
                UPDATE users
                SET username = ?, avatar_path = COALESCE(?, avatar_path), email = ?, password_hash = COALESCE(?, password_hash)
                WHERE username = ?
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newUsername);
            statement.setString(2, avatarPath);
            statement.setString(3, email);
            statement.setString(4, passwordHash);
            statement.setString(5, oldUsername);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getMessage().toLowerCase().contains("unique")) {
                return false;
            }
            throw new IllegalStateException("Cannot update profile", e);
        }
    }

    public boolean updatePasswordByEmail(String email, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE email = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setString(2, email);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot update password", e);
        }
    }

    public void updateLastLogin(String username) {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE username = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot update last login", e);
        }
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, password_hash, avatar_path, email, created_at, last_login FROM users ORDER BY username";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(readUser(resultSet));
            }
            return users;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load users", e);
        }
    }

    private User readUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getInt("id"),
                resultSet.getString("username"),
                resultSet.getString("password_hash"),
                resultSet.getString("avatar_path"),
                resultSet.getString("email"),
                parseDate(resultSet.getString("created_at")),
                parseDate(resultSet.getString("last_login"))
        );
    }

    private LocalDateTime parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value.replace(" ", "T"));
    }
}
