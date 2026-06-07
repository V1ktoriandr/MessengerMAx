package com.icq.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("/fxml/login.fxml"));
        Scene scene = new Scene(loader.load(), 780, 520);
        scene.getStylesheets().add(ClientApplication.class.getResource("/css/style.css").toExternalForm());
        stage.setTitle("Messenger MAX");
        stage.setScene(scene);
        stage.setMinWidth(720);
        stage.setMinHeight(500);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
