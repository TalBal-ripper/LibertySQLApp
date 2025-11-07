package com.example.libertyappsql.controller;

import com.example.libertyappsql.db.DatabaseManager;
import com.example.libertyappsql.db.DbConfig;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.VPos;
import javafx.print.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
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

    private DatabaseManager db;

    @FXML
    private void initialize() {
        Properties props = DbConfig.load();
        db = new DatabaseManager(props);

        // Тип TableView — строки как Map<columnName, value>
        dataTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        openSqlFileButton.setOnAction(e -> chooseAndImportSql());
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

        // При старте попробуем загрузить таблицы (если БД уже настроена)
        Platform.runLater(() -> refreshTablesList());
    }

    private void applySearch(String query) {
        // Простой клиентский фильтр по вхождению строки в любом столбце:
        if (query == null) query = "";
        ObservableList<Map<String,Object>> all = dataTable.getItems();
        if (query.isBlank()) {
            dataTable.setItems(all);
            return;
        }
        String q = query.toLowerCase();
        ObservableList<Map<String,Object>> filtered = FXCollections.observableArrayList();
        for (Map<String,Object> row : all) {
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

            // Очистка колонок
            dataTable.getColumns().clear();

            // Создание колонок динамически
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
                dataTable.getColumns().add(tc);
            }

            // Сбор строк
            ObservableList<Map<String,Object>> items = FXCollections.observableArrayList();
            while (rs.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                for (String c : colNames) {
                    Object v = rs.getObject(c);
                    row.put(c, v);
                }
                items.add(row);
            }
            dataTable.setItems(items);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Не удалось загрузить таблицу: " + e.getMessage());
        }
    }

    private void openRecordDialog(Map<String,Object> existingRow) {
        String table = tableSelector.getValue();
        if (table == null) { showAlert("Оберіть таблицю"); return; }

        try {
            LinkedHashMap<String,Integer> cols = db.getColumns(table);
            Optional<String> pkOpt = db.getPrimaryKeyColumn(table);

            // Построим диалог с GridPane динамически
            Dialog<Map<String,Object>> dialog = new Dialog<>();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(existingRow == null ? "Створити запис" : "Редагувати запис");
            ButtonType save = new ButtonType("Зберегти", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setVgap(8);
            grid.setHgap(10);

            Map<String, Control> inputs = new LinkedHashMap<>();
            int rowIdx = 0;
            for (Map.Entry<String,Integer> col : cols.entrySet()) {
                String colName = col.getKey();
                Label lab = new Label(colName + ":");
                Control input;
                int sqlType = col.getValue();
                // Простая эвристика по типу
                if (sqlType == Types.DATE || sqlType == Types.TIMESTAMP) {
                    DatePicker dp = new DatePicker();
                    input = dp;
                } else {
                    TextField tf = new TextField();
                    input = tf;
                }
                // Если PK и существующая запись — показываем как disabled
                if (pkOpt.isPresent() && pkOpt.get().equals(colName) && existingRow != null) {
                    input.setDisable(true);
                }

                // Заполняем, если редактируем
                if (existingRow != null && existingRow.containsKey(colName)) {
                    Object v = existingRow.get(colName);
                    if (input instanceof DatePicker) {
                        if (v instanceof java.sql.Date) {
                            ((DatePicker) input).setValue(((java.sql.Date) v).toLocalDate());
                        } else if (v instanceof java.sql.Timestamp) {
                            ((DatePicker) input).setValue(((java.sql.Timestamp) v).toLocalDateTime().toLocalDate());
                        }
                    } else if (input instanceof TextField) {
                        ((TextField) input).setText(v != null ? v.toString() : "");
                    }
                }

                grid.add(lab, 0, rowIdx);
                grid.add(input, 1, rowIdx);
                inputs.put(colName, input);
                rowIdx++;
            }

            dialog.getDialogPane().setContent(grid);
            Platform.runLater(() -> dialog.getDialogPane().requestFocus());

            dialog.setResultConverter(btn -> {
                if (btn == save) {
                    Map<String,Object> result = new HashMap<>();
                    for (String cname : inputs.keySet()) {
                        Control cctrl = inputs.get(cname);
                        Object val = null;
                        if (cctrl instanceof DatePicker) {
                            LocalDate d = ((DatePicker) cctrl).getValue();
                            if (d != null) val = java.sql.Date.valueOf(d);
                        } else if (cctrl instanceof TextField) {
                            String s = ((TextField) cctrl).getText();
                            if (s != null && !s.isEmpty()) val = s;
                        }
                        result.put(cname, val);
                    }
                    return result;
                }
                return null;
            });

            Optional<Map<String,Object>> res = dialog.showAndWait();
            if (res.isPresent()) {
                Map<String,Object> values = res.get();
                if (existingRow == null) {
                    // remove PK if null and auto-increment
                    pkOpt.ifPresent(pk -> {
                        if (values.get(pk) == null) values.remove(pk);
                    });
                    db.insert(table, values);
                } else {
                    // update by PK
                    if (pkOpt.isEmpty()) {
                        showAlert("Таблиця не має PK — операція редагування недоступна");
                        return;
                    }
                    String pk = pkOpt.get();
                    Object pkValue = existingRow.get(pk);
                    // не включаем PK в набор колонок для update
                    values.remove(pk);
                    db.updateByPK(table, pk, pkValue, values);
                }
                refreshTablesList();
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

            Dialog<ButtonType> dlg = new Dialog<>();
            dlg.setTitle("Підтвердження видалення");
            dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dlg.setContentText("Ви впевнені що хочете видалити запис з " + pk + " = " + pkVal + " ?");

            Optional<ButtonType> ans = dlg.showAndWait();
            if (ans.isPresent() && ans.get() == ButtonType.OK) {
                db.deleteByPK(table, pk, pkVal);
                loadTable(table);
            }


        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Помилка видалення: " + e.getMessage());
        }
    }

    private void printNode(Node node) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showAlert("Неможливо створити PrinterJob на цій системі");
            return;
        }
        boolean proceed = job.showPrintDialog(node.getScene().getWindow());
        if (proceed) {
            boolean success = job.printPage(node);
            if (success) job.endJob();
        }
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.initOwner(tableSelector.getScene().getWindow());
        a.showAndWait();
    }
}
