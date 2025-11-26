package com.example.libertyappsql.controller;

import com.example.libertyappsql.db.DbConfig;
import com.example.libertyappsql.launcher.Launcher;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    @FXML
    void initialize() {
        loginButton.setOnAction(event -> handleLogin());
    }

    private void handleLogin() {

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty()) {
            errorLabel.setText("Введите имя пользователя.");
            return;
        }

        String url = "jdbc:mysql://localhost:3306/furniture_store_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            Connection conn = DriverManager.getConnection(url, username, password);
            conn.close();

            // УСПЕШНЫЙ ВХОД
            DbConfig.CURRENT_USER = username;
            DbConfig.CURRENT_PASSWORD = password;

            openMainWindow(DbConfig.getDynamicConfig());
            return;

        } catch (SQLException | ClassNotFoundException e) {

            String msg = e.getMessage().toLowerCase();

            // НЕТ СЕРВЕРА
            if (msg.contains("communicat") || msg.contains("connection") || msg.contains("refused")) {
                errorLabel.setText("❌ MySQL сервер не запущен или не установлен.");
                return;
            }

            // БАЗЫ НЕТ
            if (msg.contains("unknown database")) {
                errorLabel.setText("❌ База данных furniture_store_db отсутствует.");
                return;
            }

            // НЕВЕРНЫЙ ПАРОЛЬ / ПОЛЬЗОВАТЕЛЯ НЕТ
            if (msg.contains("access denied")) {
                errorLabel.setText("❌ Неверный логин или пароль.");
                return;
            }

            // ВСЕ ОСТАЛЬНЫЕ СЛУЧАИ
            errorLabel.setText("Ошибка подключения: " + e.getMessage());
        }
    }


    private void openMainWindow(Properties props) {
        try {
            Stage loginStage = (Stage) loginButton.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(
                    Launcher.class.getResource("/com/example/libertyappsql/MainView.fxml")
            );

            Scene scene = new Scene(loader.load(), 1440, 900);
            Stage mainStage = new Stage();
            mainStage.setTitle("Інформаційна Система");
            mainStage.setScene(scene);
            mainStage.show();
            Platform.runLater(loginStage::close);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
