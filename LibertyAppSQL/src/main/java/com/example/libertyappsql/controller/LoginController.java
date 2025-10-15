package com.example.libertyappsql.controller;

import com.example.libertyappsql.launcher.Launcher;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    @FXML void initialize() {
        loginButton.setOnAction(event -> handleLogin());
    }

    private void handleLogin() {
        if ("admin".equals(usernameField.getText()) && "admin".equals(passwordField.getText())) {
            openMainWindow();
        } else {
            errorLabel.setText("Невірне ім'я користувача або пароль.");
        }
    }

    private void openMainWindow() {
        try {
            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            currentStage.close();

            FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("/com/example/libertyappsql/MainView.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            Stage mainStage = new Stage();
            mainStage.setTitle("Інформаційна Система");
            mainStage.setScene(scene);
            mainStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}