module com.example.libertyappsql {
    requires javafx.controls;
    requires javafx.fxml;

    // Цей рядок дозволяє FXML отримувати доступ до ваших контролерів
    opens com.example.libertyappsql.controller to javafx.fxml;

    // Цей рядок експортує ваш пакет з лаунчером для запуску
    exports com.example.libertyappsql.launcher;
}