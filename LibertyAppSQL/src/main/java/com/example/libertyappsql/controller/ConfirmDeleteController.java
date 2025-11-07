package com.example.libertyappsql.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ConfirmDeleteController {

    @FXML private Label confirmMessageLabel;
    @FXML private Button deleteButton;
    @FXML private Button cancelButton;

    private Runnable onConfirm; // callback

    @FXML
    private void initialize() {
        deleteButton.setOnAction(e -> {
            if (onConfirm != null) onConfirm.run();
            closeWindow();
        });

        cancelButton.setOnAction(e -> closeWindow());
    }

    public void setOnConfirm(Runnable onConfirm) {
        this.onConfirm = onConfirm;
    }

    private void closeWindow() {
        Stage stage = (Stage) deleteButton.getScene().getWindow();
        stage.close();
    }
}
