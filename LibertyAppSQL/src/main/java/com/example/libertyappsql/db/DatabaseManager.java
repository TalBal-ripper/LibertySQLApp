package com.example.libertyappsql.db;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class DatabaseManager {

    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager(Properties props) {
        this.url = props.getProperty("jdbc.url");
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

    /**
     * Выполнить SQL-скрипт (дамп). Метод поддерживает множественные запросы;
     * учитывает простое разделение по ';' и игнорирует строки комментариев '--' и '/* ... *\/'
     */
    public void runSqlScript(File sqlFile) throws Exception {
        String sql = readFile(sqlFile);
        // Удалим MySQL служебные директивы типа /*!40101 ... */ — они часто есть в дампах.
        sql = sql.replaceAll("/\\*!.*?\\*/", " ");
        // Простая разборка запросов (может не покрывать все edge-case'ы, но для типичного дампа работает).
        List<String> statements = splitSqlStatements(sql);
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                for (String s : statements) {
                    String trimmed = s.trim();
                    if (trimmed.isEmpty()) continue;
                    st.execute(trimmed);
                }
            }
            conn.commit();
        }
    }

    private static String readFile(File f) throws IOException {
        try (InputStream in = new FileInputStream(f)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<String> splitSqlStatements(String sql) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inSingle = false, inDouble = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            cur.append(c);
            if (c == '\'' && !inDouble) inSingle = !inSingle;
            if (c == '"' && !inSingle) inDouble = !inDouble;
            if (c == ';' && !inSingle && !inDouble) {
                out.add(cur.toString());
                cur.setLength(0);
            }
        }
        if (cur.toString().trim().length() > 0) out.add(cur.toString());
        return out;
    }

    /** Получить список таблиц в текущем каталоге БД */
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

    /** Получить ResultSet данных таблицы (используй только для чтения) */
    public ResultSet queryTable(String tableName) throws SQLException {
        Connection c = getConnection();
        Statement st = c.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        return st.executeQuery("SELECT * FROM `" + tableName + "`");
    }

    /** Удалить запись по первичному ключу (pkColumn и pkValue) */
    public void deleteByPK(String table, String pkColumn, Object pkValue) throws SQLException {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM `" + table + "` WHERE `" + pkColumn + "` = ?")) {
            ps.setObject(1, pkValue);
            ps.executeUpdate();
        }
    }

    /** Вставка: сформировать PreparedStatement динамически по метаданным */
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

    /** Обновление записи по PK */
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

    /** Получить первичный ключ таблицы (первый найденный) */
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

    /** Получить метаданные столбцов: Map<columnName, java.sql.Types> */
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
}
