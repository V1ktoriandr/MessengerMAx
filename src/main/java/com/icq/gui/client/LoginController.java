package com.icq.gui.client;

import com.icq.client.ClientApplication;
import com.icq.client.ClientConnection;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    @FXML private TextField serverIpField;
    @FXML private TextField usernameField;
    @FXML private Button connectButton;

    @FXML
    private void initialize() {
        serverIpField.setText("127.0.0.1");
    }

    @FXML
    private void connect() {
        String host = serverIpField.getText().trim();
        String username = usernameField.getText().trim();
        if (host.isBlank() || username.isBlank()) {
            showError("Enter server IP and username.");
            return;
        }

        connectButton.setDisable(true);
        Thread thread = new Thread(() -> {
            try {
                ClientConnection connection = new ClientConnection();
                connection.connect(host, username);
                Platform.runLater(() -> openChat(connection));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    connectButton.setDisable(false);
                    showError("Cannot connect: " + e.getMessage());
                });
            }
        }, "client-connect");
        thread.setDaemon(true);
        thread.start();
    }

    private void openChat(ClientConnection connection) {
        try {
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("/fxml/chat.fxml"));
            Scene scene = new Scene(loader.load(), 980, 680);
            scene.getStylesheets().add(ClientApplication.class.getResource("/css/style.css").toExternalForm());
            ChatController controller = loader.getController();
            controller.setConnection(connection);

            Stage stage = (Stage) connectButton.getScene().getWindow();
            stage.setTitle("ICQ - " + connection.getUsername());
            stage.setScene(scene);
            stage.setMinWidth(820);
            stage.setMinHeight(560);
            stage.setOnCloseRequest(event -> connection.disconnect());
            connection.startReceiver();
        } catch (Exception e) {
            connectButton.setDisable(false);
            showError("Cannot open chat: " + e.getMessage());
        }
    }

    private void showError(String text) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("ICQ");
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }
}
