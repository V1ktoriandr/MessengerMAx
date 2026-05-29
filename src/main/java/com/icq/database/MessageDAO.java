package com.icq.database;

import com.icq.model.ChatMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {
    private final DatabaseManager databaseManager;

    public MessageDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(ChatMessage message) {
        String sql = "INSERT INTO messages(sender, receiver, message, send_time) VALUES(?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, message.getSender());
            statement.setString(2, message.getReceiver());
            statement.setString(3, message.getMessage());
            statement.setString(4, message.getSendTime().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot save message", e);
        }
    }

    public List<ChatMessage> findConversation(String firstUser, String secondUser) {
        List<ChatMessage> messages = new ArrayList<>();
        String sql = """
                SELECT id, sender, receiver, message, send_time
                FROM messages
                WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?)
                ORDER BY datetime(send_time), id
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, firstUser);
            statement.setString(2, secondUser);
            statement.setString(3, secondUser);
            statement.setString(4, firstUser);
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
        String sql = "SELECT id, sender, receiver, message, send_time FROM messages ORDER BY datetime(send_time), id";
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

    private ChatMessage read(ResultSet resultSet) throws SQLException {
        return new ChatMessage(
                resultSet.getInt("id"),
                resultSet.getString("sender"),
                resultSet.getString("receiver"),
                resultSet.getString("message"),
                LocalDateTime.parse(resultSet.getString("send_time").replace(" ", "T"))
        );
    }
}
