package com.example.libertyappsql.db;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

public class DatabaseManager {

    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager(Properties props) {
        String baseUrl = props.getProperty("jdbc.url");
        if (baseUrl != null && !baseUrl.contains("allowPublicKeyRetrieval")) {
            String separator = baseUrl.contains("?") ? "&" : "?";
            baseUrl += separator + "allowPublicKeyRetrieval=true";
        }
        this.url = baseUrl;
        this.user = props.getProperty("jdbc.user");
        this.password = props.getProperty("jdbc.password");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void runSqlScript(File sqlFile) throws Exception {
        String sql = readFile(sqlFile);
        sql = cleanMySQLDump(sql);
        List<String> statements = splitSqlStatements(sql);

        try (Connection conn = getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.execute("SET FOREIGN_KEY_CHECKS = 0");
            }

            conn.setAutoCommit(false);

            int successCount = 0;
            int errorCount = 0;

            try (Statement st = conn.createStatement()) {
                for (String statement : statements) {
                    String trimmed = statement.trim();

                    if (trimmed.isEmpty()) continue;
                    if (trimmed.startsWith("--") || trimmed.startsWith("#")) continue;
                    if (trimmed.toUpperCase().startsWith("LOCK TABLES") ||
                            trimmed.toUpperCase().startsWith("UNLOCK TABLES")) {
                        continue;
                    }

                    try {
                        st.execute(trimmed);
                        successCount++;
                    } catch (SQLException e) {
                        errorCount++;
                        System.err.println("⚠ Помилка виконання запиту:");
                        System.err.println("   " + (trimmed.length() > 100 ?
                                trimmed.substring(0, 100) + "..." : trimmed));
                        System.err.println("   Помилка: " + e.getMessage());
                    }
                }
            }

            conn.commit();
            try (Statement st = conn.createStatement()) {
                st.execute("SET FOREIGN_KEY_CHECKS = 1");
            }

            System.out.println("\n✅ Імпорт завершено:");
            System.out.println("   Успішно: " + successCount);
            System.out.println("   Помилок: " + errorCount);
        }
    }

    private static String readFile(File f) throws IOException {
        try (InputStream in = new FileInputStream(f)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String cleanMySQLDump(String sql) {
        sql = sql.replaceAll("/\\*!\\d+.*?\\*/;?", "");
        sql = sql.replaceAll("COLLATE=utf8mb4_0900_ai_ci", "COLLATE=utf8mb4_general_ci");
        sql = sql.replaceAll("COLLATE utf8mb4_0900_ai_ci", "COLLATE utf8mb4_general_ci");

        String[] lines = sql.split("\n");
        StringBuilder cleaned = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("--") ||
                    trimmed.startsWith("#") ||
                    trimmed.toUpperCase().startsWith("SET @OLD_") ||
                    trimmed.toUpperCase().startsWith("SET NAMES") ||
                    trimmed.toUpperCase().startsWith("SET TIME_ZONE") ||
                    trimmed.toUpperCase().startsWith("SET SQL_MODE") ||
                    trimmed.toUpperCase().startsWith("SET FOREIGN_KEY_CHECKS") ||
                    trimmed.toUpperCase().startsWith("SET UNIQUE_CHECKS")) {
                continue;
            }

            cleaned.append(line).append("\n");
        }

        return cleaned.toString();
    }

    private static List<String> splitSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            char next = (i + 1 < sql.length()) ? sql.charAt(i + 1) : '\0';

            if (c == '\\' && (inSingleQuote || inDoubleQuote)) {
                current.append(c);
                if (next != '\0') {
                    current.append(next);
                    i++;
                }
                continue;
            }

            if (c == '\'' && !inDoubleQuote && !inBacktick) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote && !inBacktick) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktick = !inBacktick;
            }

            current.append(c);

            if (c == ';' && !inSingleQuote && !inDoubleQuote && !inBacktick) {
                String statement = current.toString().trim();
                if (!statement.isEmpty() && !statement.equals(";")) {
                    statements.add(statement);
                }
                current.setLength(0);
            }
        }

        String lastStatement = current.toString().trim();
        if (!lastStatement.isEmpty() && !lastStatement.equals(";")) {
            statements.add(lastStatement);
        }

        return statements;
    }

    public List<String> listTables() throws SQLException {
        try (Connection c = getConnection()) {
            DatabaseMetaData md = c.getMetaData();
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = md.getTables(c.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            return tables;
        }
    }

    public ResultSet queryTable(String tableName) throws SQLException {
        Connection c = getConnection();
        Statement st = c.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        return st.executeQuery("SELECT * FROM `" + tableName + "`");
    }

    public void deleteByPK(String table, String pkColumn, Object pkValue) throws SQLException {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM `" + table + "` WHERE `" + pkColumn + "` = ?")) {
            ps.setObject(1, pkValue);
            ps.executeUpdate();
        }
    }

    public void insert(String table, Map<String, Object> values) throws SQLException {
        String cols = values.keySet().stream().map(k -> "`" + k + "`").collect(Collectors.joining(","));
        String qMarks = values.keySet().stream().map(k -> "?").collect(Collectors.joining(","));
        String sql = String.format("INSERT INTO `%s` (%s) VALUES (%s)", table, cols, qMarks);
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            for (String k : values.keySet()) {
                ps.setObject(i++, values.get(k));
            }
            ps.executeUpdate();
        }
    }

    public void updateByPK(String table, String pkColumn, Object pkValue, Map<String, Object> values) throws SQLException {
        String set = values.keySet().stream().map(k -> "`" + k + "` = ?").collect(Collectors.joining(","));
        String sql = String.format("UPDATE `%s` SET %s WHERE `%s` = ?", table, set, pkColumn);
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            for (String k : values.keySet()) ps.setObject(i++, values.get(k));
            ps.setObject(i, pkValue);
            ps.executeUpdate();
        }
    }

    public Optional<String> getPrimaryKeyColumn(String table) throws SQLException {
        try (Connection c = getConnection()) {
            DatabaseMetaData md = c.getMetaData();
            try (ResultSet rs = md.getPrimaryKeys(c.getCatalog(), null, table)) {
                if (rs.next()) {
                    return Optional.of(rs.getString("COLUMN_NAME"));
                }
            }
        }
        return Optional.empty();
    }

    public LinkedHashMap<String, Integer> getColumns(String table) throws SQLException {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM `" + table + "` LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                map.put(md.getColumnName(i), md.getColumnType(i));
            }
        }
        return map;
    }

    public void exportDatabaseToSql(File outputFile) throws Exception {
        try (Connection conn = getConnection();
             FileWriter writer = new FileWriter(outputFile, StandardCharsets.UTF_8)) {

            writer.write("-- MySQL Database Export\n");
            writer.write("-- Database: " + conn.getCatalog() + "\n");
            writer.write("-- Date: " + new java.util.Date() + "\n\n");

            writer.write("SET FOREIGN_KEY_CHECKS = 0;\n\n");

            List<String> tables = listTables();

            for (String table : tables) {
                writer.write("-- ==========================================\n");
                writer.write("-- Table: " + table + "\n");
                writer.write("-- ==========================================\n\n");

                writer.write("DROP TABLE IF EXISTS `" + table + "`;\n\n");

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + table + "`")) {
                    if (rs.next()) {
                        String createTable = rs.getString(2);
                        createTable = createTable.replaceAll("utf8mb4_0900_ai_ci", "utf8mb4_general_ci");
                        writer.write(createTable + ";\n\n");
                    }
                }

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM `" + table + "`")) {

                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();

                    if (rs.next()) {
                        writer.write("INSERT INTO `" + table + "` VALUES\n");

                        boolean first = true;
                        do {
                            if (!first) writer.write(",\n");
                            first = false;

                            writer.write("(");
                            for (int i = 1; i <= columnCount; i++) {
                                if (i > 1) writer.write(",");

                                Object value = rs.getObject(i);
                                if (value == null) {
                                    writer.write("NULL");
                                } else if (value instanceof Number) {
                                    writer.write(value.toString());
                                } else if (value instanceof Date || value instanceof java.sql.Timestamp) {
                                    writer.write("'" + value.toString() + "'");
                                } else {
                                    String strValue = value.toString()
                                            .replace("\\", "\\\\")
                                            .replace("'", "\\'")
                                            .replace("\"", "\\\"");
                                    writer.write("'" + strValue + "'");
                                }
                            }
                            writer.write(")");

                        } while (rs.next());

                        writer.write(";\n\n");
                    }
                }
            }

            writer.write("SET FOREIGN_KEY_CHECKS = 1;\n");
            writer.flush();
        }
    }
}