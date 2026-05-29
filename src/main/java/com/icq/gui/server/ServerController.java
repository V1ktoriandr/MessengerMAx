package com.icq.gui.server;

import com.icq.model.ChatMessage;
import com.icq.server.ServerCore;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ServerController {
    @FXML private ListView<String> eventsList;
    @FXML private ListView<String> messagesList;
    @FXML private ListView<String> dialogsList;
    @FXML private ListView<String> onlineList;
    @FXML private Label statusLabel;

    private final ObservableList<String> events = FXCollections.observableArrayList();
    private final ObservableList<String> messages = FXCollections.observableArrayList();
    private final ObservableList<String> dialogs = FXCollections.observableArrayList();
    private final ObservableList<String> online = FXCollections.observableArrayList();
    private ServerCore serverCore;

    @FXML
    private void initialize() {
        eventsList.setItems(events);
        messagesList.setItems(messages);
        dialogsList.setItems(dialogs);
        onlineList.setItems(online);

        serverCore = new ServerCore(
                this::addEvent,
                this::addMessage,
                users -> Platform.runLater(() -> online.setAll(users)),
                activeDialogs -> Platform.runLater(() -> dialogs.setAll(activeDialogs))
        );
        serverCore.allMessages().forEach(this::addMessage);
        serverCore.start();
        statusLabel.setText("Server is running on port " + ServerCore.PORT);
    }

    @FXML
    private void stopServer() {
        if (serverCore != null) {
            serverCore.stop();
            addEvent("Server stopped");
            statusLabel.setText("Server stopped");
        }
    }

    private void addEvent(String event) {
        Platform.runLater(() -> {
            events.add(event);
            eventsList.scrollTo(events.size() - 1);
        });
    }

    private void addMessage(ChatMessage message) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String row = "%s  %s -> %s: %s".formatted(
                message.getSendTime().format(formatter),
                message.getSender(),
                message.getReceiver(),
                message.getMessage()
        );
        Platform.runLater(() -> {
            messages.add(row);
            messagesList.scrollTo(messages.size() - 1);
        });
    }
}
