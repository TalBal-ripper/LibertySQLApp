package com.example.libertyappsql.db;

import java.util.Properties;

public class DbConfig {

    public static String CURRENT_USER = null;
    public static String CURRENT_PASSWORD = null;

    public static Properties getDynamicConfig() {
        Properties p = new Properties();
        p.setProperty("jdbc.url",
                "jdbc:mysql://localhost:3306/furniture_store_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        p.setProperty("jdbc.user", CURRENT_USER);
        p.setProperty("jdbc.password", CURRENT_PASSWORD);
        return p;
    }

    public static boolean isRoot() {
        return "root".equalsIgnoreCase(CURRENT_USER);
    }
}



