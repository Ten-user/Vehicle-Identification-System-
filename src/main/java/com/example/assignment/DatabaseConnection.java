package com.example.assignment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton Database Connection Manager for PostgreSQL (Neon)
 */
public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        try {
            // Load PostgreSQL JDBC Driver
            Class.forName("org.postgresql.Driver");
            connect();
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver not found: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    private void connect() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(
                        DatabaseConfig.getJdbcUrl(),
                        DatabaseConfig.getUsername(),
                        DatabaseConfig.getPassword()
                );
                System.out.println("Connected to Neon PostgreSQL successfully!");
            }
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    /**
     * Execute SELECT query with parameters (PostgreSQL uses $1, $2, etc.)
     */
    public ResultSet executeParameterizedQuery(String sql, Object... params) throws SQLException {
        PreparedStatement stmt = getConnection().prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
        return stmt.executeQuery();
    }

    /**
     * Execute UPDATE/INSERT/DELETE with parameters
     */
    public int executeParameterizedUpdate(String sql, Object... params) throws SQLException {
        PreparedStatement stmt = getConnection().prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
        return stmt.executeUpdate();
    }

    /**
     * Execute INSERT and return generated ID (PostgreSQL uses RETURNING)
     */
    public int executeInsertWithReturnId(String sql, Object... params) throws SQLException {
        PreparedStatement stmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
        stmt.executeUpdate();
        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next()) {
            return rs.getInt(1);
        }
        return -1;
    }

    /**
     * Execute simple SELECT query
     */
    public ResultSet executeQuery(String sql) throws SQLException {
        Statement stmt = getConnection().createStatement();
        return stmt.executeQuery(sql);
    }

    /**
     * Test database connection
     */
    public boolean testConnection() {
        try {
            Connection conn = getConnection();
            if (conn != null && !conn.isClosed()) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT version()");
                if (rs.next()) {
                    System.out.println("PostgreSQL Version: " + rs.getString(1));
                }
                rs.close();
                stmt.close();
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Connection test failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * Close the database connection
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}