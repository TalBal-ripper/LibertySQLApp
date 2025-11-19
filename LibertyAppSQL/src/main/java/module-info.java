module com.example.libertyappsql {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.hibernate.orm.core;
    requires jakarta.persistence;
    requires java.sql;
    requires java.naming;
    requires org.slf4j;
    requires jdk.jdi;
    opens com.example.libertyappsql.controller to javafx.fxml;
    opens com.example.libertyappsql.model to org.hibernate.orm.core;
    exports com.example.libertyappsql.launcher;
    exports com.example.libertyappsql.model;
}