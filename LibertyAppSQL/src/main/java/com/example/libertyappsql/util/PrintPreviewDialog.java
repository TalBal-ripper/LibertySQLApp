package com.example.libertyappsql.util;

import javafx.print.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.util.Map;

public class PrintPreviewDialog {

    public static void showPreviewAndPrint(TableView<Map<String, Object>> tableView, Stage owner) {

        Stage previewStage = new Stage();
        previewStage.initModality(Modality.APPLICATION_MODAL);
        previewStage.initOwner(owner);
        previewStage.setTitle("Попередній перегляд");
        TableView<Map<String, Object>> previewTable = createPrintableTable(tableView);
        ScrollPane scrollPane = new ScrollPane(previewTable);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: white;");
        Button printBtn = new Button("🖨 Друкувати");
        Button cancelBtn = new Button("Скасувати");

        printBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10 20;");
        cancelBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10 20;");

        HBox buttonBar = new HBox(10, printBtn, cancelBtn);
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.setPadding(new Insets(10));

        printBtn.setOnAction(e -> {
            performPrint(previewTable, owner);
            previewStage.close();
        });

        cancelBtn.setOnAction(e -> previewStage.close());

        BorderPane root = new BorderPane();
        root.setCenter(scrollPane);
        root.setBottom(buttonBar);

        Scene scene = new Scene(root, 900, 700);
        previewStage.setScene(scene);
        previewStage.showAndWait();
    }

    private static TableView<Map<String, Object>> createPrintableTable(
            TableView<Map<String, Object>> original) {

        TableView<Map<String, Object>> printTable = new TableView<>();
        printTable.setItems(original.getItems());

        for (TableColumn<Map<String, Object>, ?> col : original.getColumns()) {
            TableColumn<Map<String, Object>, Object> newCol =
                    new TableColumn<>(col.getText());

            String columnName = col.getText();
            newCol.setCellValueFactory(cellData -> {
                Map<String, Object> row = cellData.getValue();
                Object val = row.get(columnName);
                return new javafx.beans.property.SimpleObjectProperty<>(val);
            });

            newCol.setPrefWidth(120);
            newCol.setMinWidth(80);

            printTable.getColumns().add(newCol);
        }

        printTable.setStyle(
                "-fx-font-size: 10px; " +
                        "-fx-background-color: white;"
        );

        double totalWidth = printTable.getColumns().size() * 120;
        printTable.setPrefWidth(totalWidth);
        printTable.setMinWidth(totalWidth);

        return printTable;
    }

    private static void performPrint(TableView<Map<String, Object>> table, Stage owner) {
        PrinterJob job = PrinterJob.createPrinterJob();

        if (job == null) {
            showAlert("Не вдалося ініціалізувати друк", owner);
            return;
        }

        boolean proceed = job.showPrintDialog(owner);
        if (!proceed) {
            return;
        }
        PageLayout pageLayout = job.getJobSettings().getPageLayout();
        double pageWidth = pageLayout.getPrintableWidth();
        double pageHeight = pageLayout.getPrintableHeight();
        double tableWidth = table.getWidth();
        double tableHeight = table.getHeight();
        double scaleX = pageWidth / tableWidth;
        double scaleY = pageHeight / tableHeight;
        double scale = Math.min(scaleX, scaleY);

        if (scale > 1.0) {
            scale = 1.0;
        }

        Scale scaleTransform = new Scale(scale, scale);
        table.getTransforms().clear();
        table.getTransforms().add(scaleTransform);
        boolean success = job.printPage(pageLayout, table);

        if (success) {
            job.endJob();
            showAlert("Документ надіслано на друк", owner);
        } else {
            showAlert("Помилка друку", owner);
        }
        table.getTransforms().clear();
    }

    private static void showAlert(String message, Stage owner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.initOwner(owner);
        alert.showAndWait();
    }
}