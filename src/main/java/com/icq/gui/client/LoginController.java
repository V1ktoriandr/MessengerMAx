package com.icq.gui.client;

import com.icq.client.ClientApplication;
import com.icq.client.ClientConnection;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button connectButton;

    @FXML
    private void connect() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isBlank() || password.isBlank()) {
            showError("Enter login/email and password.");
            return;
        }

        connectButton.setDisable(true);
        Thread thread = new Thread(() -> {
            try {
                ClientConnection connection = new ClientConnection();
                connection.login(DEFAULT_SERVER_HOST, username, password);
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

    @FXML
    private void openRegistration() {
        try {
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("/fxml/register.fxml"));
            Scene scene = new Scene(loader.load(), 520, 560);
            scene.getStylesheets().add(ClientApplication.class.getResource("/css/style.css").toExternalForm());
            Stage stage = new Stage();
            stage.setTitle("Messenger MAX Registration");
            stage.setScene(scene);
            stage.setMinWidth(480);
            stage.setMinHeight(520);
            stage.show();
        } catch (Exception e) {
            showError("Cannot open registration: " + e.getMessage());
        }
    }

    @FXML
    private void openPasswordReset() {
        try {
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("/fxml/password_reset.fxml"));
            Scene scene = new Scene(loader.load(), 460, 420);
            scene.getStylesheets().add(ClientApplication.class.getResource("/css/style.css").toExternalForm());
            Stage stage = new Stage();
            stage.setTitle("Password recovery");
            stage.setScene(scene);
            stage.setMinWidth(420);
            stage.setMinHeight(380);
            stage.show();
        } catch (Exception e) {
            showError("Cannot open password recovery: " + e.getMessage());
        }
    }

    private void openChat(ClientConnection connection) {
        try {
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("/fxml/chat.fxml"));
            Scene scene = new Scene(loader.load(), 1180, 760);
            scene.getStylesheets().add(ClientApplication.class.getResource("/css/style.css").toExternalForm());
            ChatController controller = loader.getController();
            controller.setConnection(connection);

            Stage stage = (Stage) connectButton.getScene().getWindow();
            stage.setTitle("Messenger MAX - " + connection.getUsername());
            stage.setScene(scene);
            stage.setMinWidth(980);
            stage.setMinHeight(640);
            stage.setOnCloseRequest(event -> connection.disconnect());
            connection.startReceiver();
        } catch (Exception e) {
            connectButton.setDisable(false);
            showError("Cannot open chat: " + e.getMessage());
        }
    }

    private void showError(String text) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Messenger MAX");
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }
}
