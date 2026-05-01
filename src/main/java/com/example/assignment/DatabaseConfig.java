package com.example.assignment;

public class DatabaseConfig {

    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "vis_database";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "12345"; // <-- change if needed

    public static String getJdbcUrl() {
        // NOTE: serverTimezone=UTC bypasses the "Location is not set" JDBC bug
        return "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
                + "?useSSL=false"
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=UTC"
                + "&useUnicode=true"
                + "&characterEncoding=UTF-8";
    }

    public static String getUsername() { return DB_USER; }
    public static String getPassword() { return DB_PASSWORD; }
}
