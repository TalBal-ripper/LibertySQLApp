package com.example.libertyappsql.controller;

import com.example.libertyappsql.launcher.Launcher;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;

public class MainController {
    @FXML private Button createButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    @FXML void initialize() {
        createButton.setOnAction(event -> openEditDialog("Створення запису"));
        editButton.setOnAction(event -> openEditDialog("Редагування запису"));
        deleteButton.setOnAction(event -> openDeleteDialog());
    }

    private void openEditDialog(String title) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("/com/example/libertyappsql/RecordEditDialog.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            Stage dialogStage = new Stage();
            dialogStage.setTitle(title);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner((Stage) createButton.getScene().getWindow());
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openDeleteDialog() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("/com/example/libertyappsql/ConfirmDeleteDialog.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Підтвердження видалення");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner((Stage) deleteButton.getScene().getWindow());
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}