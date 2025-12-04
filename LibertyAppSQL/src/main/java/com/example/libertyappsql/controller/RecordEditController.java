package com.example.libertyappsql.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class RecordEditController {

    @FXML private VBox mainContainer;
    @FXML private GridPane fieldsGrid;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Label titleLabel;

    private Map<String, Object> existingRow;
    private String primaryKey;
    private Map<String, Control> fieldControls = new HashMap<>();
    private LinkedHashMap<String, Integer> columnTypes;

    private Map<String, Object> result = null;

    @FXML
    private void initialize() {
        saveButton.setOnAction(e -> save());
        cancelButton.setOnAction(e -> close());
    }

    public void setPrimaryKey(String pk) {
        this.primaryKey = pk;
    }

    public void setColumnTypes(LinkedHashMap<String, Integer> columnTypes) {
        this.columnTypes = columnTypes;
        buildDynamicFields();
    }

    private void buildDynamicFields() {
        if (columnTypes == null || fieldsGrid == null) return;

        fieldsGrid.getChildren().clear();
        fieldControls.clear();

        int row = 0;
        for (Map.Entry<String, Integer> entry : columnTypes.entrySet()) {
            String columnName = entry.getKey();
            int sqlType = entry.getValue();

            Label label = new Label(columnName + ":");
            label.setStyle("-fx-text-fill: white; -fx-font-family: monospace; -fx-font-size: 14px;");
            fieldsGrid.add(label, 0, row);

            Control control = createControlForType(columnName, sqlType);
            fieldControls.put(columnName, control);
            fieldsGrid.add(control, 1, row);

            row++;
        }
    }
    private Control createControlForType(String columnName, int sqlType) {
        Control control;
        if (columnName.equals(primaryKey)) {
            TextField tf = new TextField();
            tf.setEditable(false);
            tf.setPromptText("Генерується автоматично");
            tf.setStyle("-fx-background-color: rgba(255,255,255,0.1); " +
                    "-fx-border-color: rgba(255,255,255,0.3); " +
                    "-fx-text-fill: #cccccc; " +
                    "-fx-background-radius: 10; -fx-border-radius: 10;");
            control = tf;
        }
        else if (sqlType == Types.DATE || sqlType == Types.TIMESTAMP) {
            DatePicker dp = new DatePicker();
            dp.setPromptText("Виберіть дату");
            dp.setStyle("-fx-background-color: rgba(255,255,255,0.1); " +
                    "-fx-border-color: rgba(255,255,255,0.3); " +
                    "-fx-background-radius: 10; -fx-border-radius: 10;");
            control = dp;
        }
        else if (sqlType == Types.INTEGER || sqlType == Types.BIGINT ||
                sqlType == Types.SMALLINT || sqlType == Types.TINYINT) {
            TextField tf = new TextField();
            tf.setPromptText("Введіть число");
            styleTextField(tf);
            tf.textProperty().addListener((obs, old, newVal) -> {
                if (newVal != null && !newVal.matches("-?\\d*")) {
                    tf.setText(old);
                }
            });
            control = tf;
        }
        else if (sqlType == Types.DECIMAL || sqlType == Types.DOUBLE ||
                sqlType == Types.FLOAT || sqlType == Types.NUMERIC) {
            TextField tf = new TextField();
            tf.setPromptText("Введіть число");
            styleTextField(tf);
            tf.textProperty().addListener((obs, old, newVal) -> {
                if (newVal != null && !newVal.matches("-?\\d*\\.?\\d*")) {
                    tf.setText(old);
                }
            });
            control = tf;
        }
        else if (sqlType == Types.LONGVARCHAR || sqlType == Types.CLOB) {
            TextArea ta = new TextArea();
            ta.setPrefRowCount(3);
            ta.setPromptText("Введіть текст");
            ta.setStyle("-fx-background-color: rgba(255,255,255,0.1); " +
                    "-fx-border-color: rgba(255,255,255,0.3); " +
                    "-fx-text-fill: white; " +
                    "-fx-prompt-text-fill: rgba(255,255,255,0.6); " +
                    "-fx-background-radius: 10; -fx-border-radius: 10;");
            control = ta;
        }
        else {
            TextField tf = new TextField();
            tf.setPromptText("Введіть значення");
            styleTextField(tf);
            control = tf;
        }

        return control;
    }

    private void styleTextField(TextField tf) {
        tf.setStyle("-fx-background-color: rgba(255,255,255,0.1); " +
                "-fx-border-color: rgba(255,255,255,0.3); " +
                "-fx-text-fill: white; " +
                "-fx-prompt-text-fill: rgba(255,255,255,0.6); " +
                "-fx-background-radius: 10; -fx-border-radius: 10;");
    }

    public void loadData(Map<String, Object> row) {
        this.existingRow = row;

        if (row == null || fieldControls.isEmpty()) return;

        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();

            Control control = fieldControls.get(columnName);
            if (control == null) continue;

            if (control instanceof TextField) {
                ((TextField) control).setText(value != null ? value.toString() : "");
            }
            else if (control instanceof TextArea) {
                ((TextArea) control).setText(value != null ? value.toString() : "");
            }
            else if (control instanceof DatePicker && value instanceof Date) {
                ((DatePicker) control).setValue(((Date) value).toLocalDate());
            }
        }
    }
    private void save() {
        Map<String, Object> out = new HashMap<>();

        for (Map.Entry<String, Control> entry : fieldControls.entrySet()) {
            String columnName = entry.getKey();
            Control control = entry.getValue();
            if (columnName.equals(primaryKey)) {
                if (control instanceof TextField) {
                    String text = ((TextField) control).getText();
                    if (!text.isBlank()) {
                        try {
                            out.put(columnName, Integer.parseInt(text));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                continue;
            }

            Object value = null;

            if (control instanceof TextField) {
                String text = ((TextField) control).getText();
                if (!text.isBlank()) {
                    Integer sqlType = columnTypes.get(columnName);
                    if (sqlType != null) {
                        if (sqlType == Types.INTEGER || sqlType == Types.BIGINT ||
                                sqlType == Types.SMALLINT || sqlType == Types.TINYINT) {
                            try {
                                value = Integer.parseInt(text);
                            } catch (NumberFormatException e) {
                                value = text;
                            }
                        } else if (sqlType == Types.DECIMAL || sqlType == Types.DOUBLE ||
                                sqlType == Types.FLOAT || sqlType == Types.NUMERIC) {
                            try {
                                value = Double.parseDouble(text);
                            } catch (NumberFormatException e) {
                                value = text;
                            }
                        } else {
                            value = text;
                        }
                    } else {
                        value = text;
                    }
                }
            }
            else if (control instanceof TextArea) {
                String text = ((TextArea) control).getText();
                if (!text.isBlank()) value = text;
            }
            else if (control instanceof DatePicker) {
                LocalDate ld = ((DatePicker) control).getValue();
                if (ld != null) value = Date.valueOf(ld);
            }

            if (value != null) {
                out.put(columnName, value);
            }
        }

        this.result = out;
        close();
    }

    public Map<String, Object> getResult() {
        return result;
    }

    private void close() {
        ((Stage) cancelButton.getScene().getWindow()).close();
    }
}
