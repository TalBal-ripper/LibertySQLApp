package com.example.libertyappsql.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class RecordEditController {

    @FXML private TextField idField, firstNameField, lastNameField, emailField, phoneField, addressField;
    @FXML private DatePicker birthDatePicker;
    @FXML private Button saveButton, cancelButton;

    private Map<String,Object> existingRow;
    private String primaryKey;

    private Map<String,Object> result = null;

    @FXML
    private void initialize() {
        saveButton.setOnAction(e -> save());
        cancelButton.setOnAction(e -> close());
    }

    public void setPrimaryKey(String pk) {
        this.primaryKey = pk;
    }

    public void loadData(Map<String,Object> row) {
        this.existingRow = row;

        if (row == null) return;

        if (row.get("id") != null) idField.setText(row.get("id").toString());
        if (row.get("firstName") != null) firstNameField.setText(row.get("firstName").toString());
        if (row.get("lastName") != null) lastNameField.setText(row.get("lastName").toString());
        if (row.get("email") != null) emailField.setText(row.get("email").toString());
        if (row.get("phone") != null) phoneField.setText(row.get("phone").toString());
        if (row.get("address") != null) addressField.setText(row.get("address").toString());

        if (row.get("birthDate") instanceof Date d)
            birthDatePicker.setValue(d.toLocalDate());
    }

    private void save() {
        Map<String,Object> out = new HashMap<>();

        if (!idField.getText().isBlank())
            out.put("id", Integer.parseInt(idField.getText()));

        out.put("firstName", firstNameField.getText());
        out.put("lastName", lastNameField.getText());
        out.put("email", emailField.getText());
        out.put("phone", phoneField.getText());
        out.put("address", addressField.getText());

        LocalDate bd = birthDatePicker.getValue();
        if (bd != null) out.put("birthDate", Date.valueOf(bd));

        this.result = out;
        close();
    }

    public Map<String,Object> getResult() {
        return result;
    }

    private void close() {
        ((Stage) cancelButton.getScene().getWindow()).close();
    }
}
