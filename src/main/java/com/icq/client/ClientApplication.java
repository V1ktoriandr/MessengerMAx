package com.icq.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("/fxml/login.fxml"));
        Scene scene = new Scene(loader.load(), 440, 360);
        scene.getStylesheets().add(ClientApplication.class.getResource("/css/style.css").toExternalForm());
        stage.setTitle("ICQ Client");
        stage.setScene(scene);
        stage.setMinWidth(420);
        stage.setMinHeight(340);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
