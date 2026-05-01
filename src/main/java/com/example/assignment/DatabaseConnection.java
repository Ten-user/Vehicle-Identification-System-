package com.example.assignment;

import java.sql.*;

/**
 * Database Connection Manager (FULL VERSION for controllers)
 */
public class DatabaseConnection {

    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found!");
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(
                    DatabaseConfig.getJdbcUrl(),
                    DatabaseConfig.getUsername(),
                    DatabaseConfig.getPassword()
            );
        }
        return connection;
    }

    // =========================
    // BASIC QUERY METHODS
    // =========================

    public ResultSet executeQuery(String sql) throws SQLException {
        Statement stmt = getConnection().createStatement();
        return stmt.executeQuery(sql);
    }

    public int executeUpdate(String sql) throws SQLException {
        Statement stmt = getConnection().createStatement();
        return stmt.executeUpdate(sql);
    }

    // =========================
    // PARAMETERIZED QUERY
    // =========================

    public ResultSet executeParameterizedQuery(String sql, Object... params) throws SQLException {
        PreparedStatement pstmt = getConnection().prepareStatement(sql);

        for (int i = 0; i < params.length; i++) {
            pstmt.setObject(i + 1, params[i]);
        }

        return pstmt.executeQuery();
    }

    public int executeParameterizedUpdate(String sql, Object... params) throws SQLException {
        PreparedStatement pstmt = getConnection().prepareStatement(sql);

        for (int i = 0; i < params.length; i++) {
            pstmt.setObject(i + 1, params[i]);
        }

        return pstmt.executeUpdate();
    }

    // =========================
    // STORED PROCEDURE
    // =========================

    public ResultSet callProcedure(String procedureName, Object... params) throws SQLException {
        StringBuilder sql = new StringBuilder("{ CALL " + procedureName + "(");

        for (int i = 0; i < params.length; i++) {
            sql.append("?");
            if (i < params.length - 1) sql.append(",");
        }

        sql.append(") }");

        CallableStatement stmt = getConnection().prepareCall(sql.toString());

        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }

        return stmt.executeQuery();
    }

    // =========================
    // CONNECTION TEST
    // =========================

    public boolean testConnection() {
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {

            return rs.next();

        } catch (SQLException e) {
            System.err.println("Connection failed: " + e.getMessage());
            return false;
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
}