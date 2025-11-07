package com.example.libertyappsql.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DbConfig {
    public static Properties load() {
        try (InputStream in = DbConfig.class.getResourceAsStream("/db.properties")) {
            Properties p = new Properties();
            p.load(in);
            return p;
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить db.properties", e);
        }
    }
}
