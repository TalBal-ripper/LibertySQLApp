package com.example.libertyappsql.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class RecordEditController {

    @FXML private TextField idField, firstNameField, lastNameField, emailField, phoneField, addressField;
    @FXML private DatePicker birthDatePicker;
    @FXML private Button saveButton, cancelButton;

    private boolean editMode = false;

    @FXML
    private void initialize() {
        saveButton.setOnAction(e -> save());
        cancelButton.setOnAction(e -> close());
    }

    public void loadData(Object data) {
        editMode = (data != null);
        System.out.println("Завантажено для редагування: " + data);
    }

    private void save() {
        System.out.println("Збереження...");
        close();
    }

    private void close() {
        ((Stage) cancelButton.getScene().getWindow()).close();
    }
}
