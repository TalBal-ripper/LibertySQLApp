plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.example.libertyappsql"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.hibernate.orm:hibernate-core:6.5.2.Final")
    implementation("com.itextpdf:itextpdf:5.5.13.3")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation("mysql:mysql-connector-java:8.0.33")
}

javafx {
    version = "21"
    modules("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("com.example.libertyappsql.launcher.Launcher")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("") // чтобы jar был без -all или -shadow
    mergeServiceFiles() // важно для JavaFX и сервисов Hibernate
    manifest {
        attributes(mapOf(
            "Main-Class" to application.mainClass.get()
        ))
    }
}

