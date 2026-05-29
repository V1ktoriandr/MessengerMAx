package com.icq.gui.client;

import com.icq.client.ClientConnection;
import com.icq.model.ChatMessage;
import com.icq.protocol.Message;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ChatController {
    @FXML private ListView<String> usersList;
    @FXML private ListView<ChatMessage> messagesList;
    @FXML private TextField messageField;
    @FXML private Label titleLabel;

    private final ObservableList<String> users = FXCollections.observableArrayList();
    private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();
    private ClientConnection connection;
    private String selectedUser;

    @FXML
    private void initialize() {
        usersList.setItems(users);
        messagesList.setItems(messages);
        messagesList.setCellFactory(list -> new MessageCell());
        usersList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedUser = newValue;
            titleLabel.setText(newValue == null ? "Select a user" : "Dialog with " + newValue);
            messages.clear();
            if (newValue != null && connection != null) {
                connection.requestHistory(newValue);
            }
        });
        messageField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage();
            }
        });
    }

    public void setConnection(ClientConnection connection) {
        this.connection = connection;
        connection.setMessageConsumer(message -> Platform.runLater(() -> handleMessage(message)));
        connection.setErrorConsumer(error -> Platform.runLater(() -> titleLabel.setText("Connection error: " + error)));
    }

    @FXML
    private void sendMessage() {
        String text = messageField.getText().trim();
        if (selectedUser == null || text.isBlank()) {
            return;
        }
        connection.sendChat(selectedUser, text);
        messageField.clear();
    }

    private void handleMessage(Message message) {
        switch (message.getType()) {
            case "users" -> {
                String previous = selectedUser;
                users.setAll(message.getUsers().stream()
                        .filter(user -> !user.equals(connection.getUsername()))
                        .toList());
                if (previous != null && users.contains(previous)) {
                    usersList.getSelectionModel().select(previous);
                }
            }
            case "chat" -> {
                ChatMessage chatMessage = new ChatMessage(message.getFrom(), message.getTo(), message.getText(), message.getTime());
                if (belongsToCurrentDialog(chatMessage)) {
                    messages.add(chatMessage);
                    messagesList.scrollTo(messages.size() - 1);
                }
            }
            case "history" -> {
                messages.setAll(message.getHistory());
                if (!messages.isEmpty()) {
                    messagesList.scrollTo(messages.size() - 1);
                }
            }
            case "error" -> titleLabel.setText(message.getText());
            default -> {
            }
        }
    }

    private boolean belongsToCurrentDialog(ChatMessage message) {
        return selectedUser != null
                && ((message.getSender().equals(connection.getUsername()) && message.getReceiver().equals(selectedUser))
                || (message.getSender().equals(selectedUser) && message.getReceiver().equals(connection.getUsername())));
    }

    private class MessageCell extends ListCell<ChatMessage> {
        @Override
        protected void updateItem(ChatMessage item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            boolean own = item.getSender().equals(connection.getUsername());
            Label author = new Label(own ? "You" : item.getSender());
            author.getStyleClass().add("message-author");
            Label text = new Label(item.getMessage());
            text.setWrapText(true);
            Label time = new Label(item.formattedTime());
            time.getStyleClass().add("message-time");

            VBox bubble = new VBox(4, author, text, time);
            bubble.getStyleClass().add(own ? "message-own" : "message-other");
            bubble.setMaxWidth(430);
            bubble.setPadding(new Insets(10, 14, 10, 14));

            HBox wrapper = new HBox(bubble);
            wrapper.setAlignment(own ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            wrapper.setPadding(new Insets(4, 10, 4, 10));
            setGraphic(wrapper);
        }
    }
}
