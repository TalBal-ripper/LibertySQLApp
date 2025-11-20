package com.example.libertyappsql.controller;

import com.example.libertyappsql.db.DatabaseManager;
import com.example.libertyappsql.db.DbConfig;
import com.example.libertyappsql.util.PrintPreviewDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.print.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class MainController {

    @FXML private ComboBox<String> tableSelector;
    @FXML private TableView<Map<String, Object>> dataTable;
    @FXML private TextField searchField;
    @FXML private TextArea sqlTextArea;

    @FXML private Button createButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button printButton;
    @FXML private Button openSqlFileButton;
    @FXML private Button exportSqlButton;

    private DatabaseManager db;
    private ObservableList<Map<String, Object>> masterData = FXCollections.observableArrayList();


    @FXML
    private void initialize() {
        Properties props = DbConfig.getDynamicConfig();
        db = new DatabaseManager(props);

        openSqlFileButton.setOnAction(e -> chooseAndImportSql());
        exportSqlButton.setOnAction(e -> exportDatabase());
        tableSelector.setOnAction(e -> {
            String t = tableSelector.getValue();
            if (t != null) loadTable(t);
        });

        createButton.setOnAction(e -> openRecordDialog(null));
        editButton.setOnAction(e -> {
            Map<String,Object> sel = dataTable.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("Спочатку виберіть рядок"); return; }
            openRecordDialog(sel);
        });

        deleteButton.setOnAction(e -> {
            Map<String,Object> sel = dataTable.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("Спочатку виберіть рядок"); return; }
            confirmAndDelete(sel);
        });

        printButton.setOnAction(e -> printNode(dataTable));

        searchField.textProperty().addListener((obs, o, n) -> applySearch(n));

        Platform.runLater(() -> refreshTablesList());
    }

    private void applySearch(String query) {
        if (query == null || query.isBlank()) {
            dataTable.setItems(masterData);
            return;
        }

        String q = query.toLowerCase();
        ObservableList<Map<String,Object>> filtered = FXCollections.observableArrayList();
        for (Map<String,Object> row : masterData) {
            for (Object v : row.values()) {
                if (v != null && v.toString().toLowerCase().contains(q)) {
                    filtered.add(row);
                    break;
                }
            }
        }
        dataTable.setItems(filtered);
    }

    private void chooseAndImportSql() {
        if (!DbConfig.isRoot()) {
            showAlert("У вас немає прав для імпорту SQL файлів.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL files", "*.sql"));
        File f = chooser.showOpenDialog((Stage) tableSelector.getScene().getWindow());
        if (f == null) return;

        try {
            db.runSqlScript(f);
            sqlTextArea.setText("Файл виконано успішно: " + f.getName());
            refreshTablesList();
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Помилка при виконанні SQL: " + ex.getMessage());
        }
    }

    private void refreshTablesList() {
        try {
            List<String> tables = db.listTables();
            tableSelector.getItems().clear();
            tableSelector.getItems().addAll(tables);
            if (!tables.isEmpty()) {
                tableSelector.setValue(tables.get(0));
                loadTable(tables.get(0));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Неможливо отримати список таблиць: " + e.getMessage());
        }
    }

    private void loadTable(String table) {
        try (ResultSet rs = db.queryTable(table)) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();

            dataTable.getColumns().clear();

            List<String> colNames = new ArrayList<>();
            for (int i = 1; i <= cols; i++) {
                String col = md.getColumnName(i);
                colNames.add(col);

                TableColumn<Map<String,Object>, Object> tc = new TableColumn<>(col);
                final int colIndex = i;
                tc.setCellValueFactory(cellData -> {
                    Map<String,Object> row = cellData.getValue();
                    Object val = row.get(col);
                    return new javafx.beans.property.SimpleObjectProperty<>(val);
                });

                tc.prefWidthProperty().bind(dataTable.widthProperty().divide(cols));
                dataTable.getColumns().add(tc);
            }

            ObservableList<Map<String,Object>> items = FXCollections.observableArrayList();
            while (rs.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                for (String c : colNames) {
                    Object v = rs.getObject(c);
                    row.put(c, v);
                }
                items.add(row);
            }

            this.masterData.clear();
            this.masterData.addAll(items);
            dataTable.setItems(this.masterData);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Не вдалося завантажити таблицю: " + e.getMessage());
        }
    }

    private void openRecordDialog(Map<String,Object> existingRow) {
        String table = tableSelector.getValue();
        if (table == null) {
            showAlert("Оберіть таблицю");
            return;
        }

        try {
            Optional<String> pkOpt = db.getPrimaryKeyColumn(table);
            String pk = pkOpt.orElse(null);
            LinkedHashMap<String, Integer> columns = db.getColumns(table);
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/libertyappsql/RecordEditDalog.fxml")
            );
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(createButton.getScene().getWindow());
            stage.setTitle(existingRow == null ? "Створити запис" : "Редагувати запис");
            stage.setScene(new Scene(loader.load()));

            RecordEditController controller = loader.getController();
            controller.setPrimaryKey(pk);
            controller.setColumnTypes(columns);
            controller.loadData(existingRow);

            stage.showAndWait();

            Map<String,Object> values = controller.getResult();
            if (values != null) {
                if (existingRow == null) {
                    if (pk != null) values.remove(pk);
                    db.insert(table, values);
                } else {
                    Object pkVal = existingRow.get(pk);
                    values.remove(pk);
                    db.updateByPK(table, pk, pkVal, values);
                }

                loadTable(table);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Помилка діалогу: " + ex.getMessage());
        }
    }


    private void confirmAndDelete(Map<String,Object> row) {
        String table = tableSelector.getValue();
        try {
            Optional<String> pkOpt = db.getPrimaryKeyColumn(table);
            if (pkOpt.isEmpty()) {
                showAlert("Таблиця не має первинного ключа; видалення не можливо через цей інтерфейс");
                return;
            }
            String pk = pkOpt.get();
            Object pkVal = row.get(pk);
            if (pkVal == null) {
                showAlert("Не знайдено значення PK у вибраному рядку");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/libertyappsql/ConfirmDeleteDialog.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(deleteButton.getScene().getWindow());

            try {
                stage.setScene(new Scene(loader.load()));
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Помилка завантаження FXML: " + e.getMessage());
                return;
            }

            ConfirmDeleteController controller = loader.getController();
            controller.setMessage("Ви впевнені, що хочете видалити запис з " + pk + " = " + pkVal + "?");

            controller.setOnConfirm(() -> {
                try {
                    db.deleteByPK(table, pk, pkVal);
                    loadTable(table);
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Помилка видалення: " + e.getMessage());
                }
            });

            stage.showAndWait();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Помилка SQL: " + e.getMessage());
        }
    }

    private void printNode(Node node) {
        if (node instanceof TableView) {
            @SuppressWarnings("unchecked")
            TableView<Map<String, Object>> table = (TableView<Map<String, Object>>) node;
            Stage owner = (Stage) tableSelector.getScene().getWindow();

            PrintPreviewDialog.showPreviewAndPrint(table, owner);
        }
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.initOwner(tableSelector.getScene().getWindow());
        a.showAndWait();
    }

    private void exportDatabase() {
        if (!DbConfig.isRoot()) {
            showAlert("У вас немає прав для експорту бази даних.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("furniture_store_backup_" +
                java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                ) + ".sql");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("SQL files", "*.sql")
        );

        File file = chooser.showSaveDialog((Stage) tableSelector.getScene().getWindow());
        if (file == null) return;

        try {
            db.exportDatabaseToSql(file);
            sqlTextArea.setText("✅ База даних успішно експортована в:\n" + file.getAbsolutePath());
            showAlert("База даних експортована успішно!");
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Помилка експорту: " + ex.getMessage());
        }
    }
}