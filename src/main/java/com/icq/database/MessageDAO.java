package com.icq.database;

import com.icq.model.ChatMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {
    private final DatabaseManager databaseManager;

    public MessageDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public int save(ChatMessage message) {
        String sql = "INSERT INTO messages(sender, receiver, message, send_time, message_type, file_path, file_name, file_size, duration) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, message.getSender());
            statement.setString(2, message.getReceiver());
            statement.setString(3, message.getMessage());
            statement.setString(4, message.getSendTime().toString());
            statement.setString(5, message.getMessageType());
            statement.setString(6, message.getFilePath());
            statement.setString(7, message.getFileName());
            statement.setLong(8, message.getFileSize());
            statement.setInt(9, message.getDuration());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot save message", e);
        }
    }

    public List<ChatMessage> findConversation(String firstUser, String secondUser) {
        List<ChatMessage> messages = new ArrayList<>();
        String sql = """
                SELECT id, sender, receiver, message, send_time, message_type, file_path, file_name, file_size, duration
                FROM messages
                WHERE ((sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?))
                AND NOT EXISTS (
                    SELECT 1 FROM hidden_dialogs
                    WHERE username = ? AND companion = CASE WHEN sender = ? THEN receiver ELSE sender END
                )
                ORDER BY datetime(send_time), id
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, firstUser);
            statement.setString(2, secondUser);
            statement.setString(3, secondUser);
            statement.setString(4, firstUser);
            statement.setString(5, firstUser);
            statement.setString(6, firstUser);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    messages.add(read(resultSet));
                }
            }
            return messages;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load conversation", e);
        }
    }

    public List<ChatMessage> findAll() {
        List<ChatMessage> messages = new ArrayList<>();
        String sql = "SELECT id, sender, receiver, message, send_time, message_type, file_path, file_name, file_size, duration FROM messages ORDER BY datetime(send_time), id";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                messages.add(read(resultSet));
            }
            return messages;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load messages", e);
        }
    }

    public void hideDialogForUser(String username, String companion) {
        String sql = "INSERT OR IGNORE INTO hidden_dialogs(username, companion) VALUES(?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, companion);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot hide dialog", e);
        }
    }

    public void deleteConversation(String firstUser, String secondUser) {
        String sql = "DELETE FROM messages WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, firstUser);
            statement.setString(2, secondUser);
            statement.setString(3, secondUser);
            statement.setString(4, firstUser);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot delete conversation", e);
        }
    }

    public void deleteMessage(int id) {
        String sql = "DELETE FROM messages WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot delete message", e);
        }
    }

    private ChatMessage read(ResultSet resultSet) throws SQLException {
        return new ChatMessage(
                resultSet.getInt("id"),
                resultSet.getString("sender"),
                resultSet.getString("receiver"),
                resultSet.getString("message"),
                LocalDateTime.parse(resultSet.getString("send_time").replace(" ", "T")),
                resultSet.getString("message_type"),
                resultSet.getString("file_path"),
                resultSet.getString("file_name"),
                resultSet.getLong("file_size"),
                resultSet.getInt("duration")
        );
    }
}
