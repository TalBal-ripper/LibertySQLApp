package com.example.libertyappsql.launcher;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Launcher extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Шлях до FXML тепер починається з кореня папки resources
        FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("/com/example/libertyappsql/LoginView.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Вхід в систему");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String args) {
        launch();
    }
}