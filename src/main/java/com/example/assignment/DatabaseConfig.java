package com.example.assignment;

public class DatabaseConfig {

    // Neon PostgreSQL Connection Details
    private static final String DB_HOST = "ep-crimson-tree-ann8acen-pooler.c-6.us-east-1.aws.neon.tech";
    private static final String DB_PORT = "5432";
    private static final String DB_NAME = "neondb";
    private static final String DB_USER = "neondb_owner";
    private static final String DB_PASSWORD = "npg_FpZt3b4wksDR";

    public static String getJdbcUrl() {
        return "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
                + "?sslmode=require"
                + "&ssl=true"
                + "&sslfactory=org.postgresql.ssl.NonValidatingFactory";
    }

    public static String getUsername() { return DB_USER; }
    public static String getPassword() { return DB_PASSWORD; }
}