package com.example.assignment;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private VBox loginContainer;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator loginProgress;
    @FXML private ProgressBar systemHealthBar;
    @FXML private ProgressIndicator connectionIndicator;
    @FXML private Label systemStatusLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (loginProgress != null) loginProgress.setVisible(false);
        if (passwordField != null) passwordField.setOnAction(e -> handleLogin(e));

        testDatabaseConnection();
    }

    private void testDatabaseConnection() {
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            Connection conn = db.getConnection();
            if (conn != null && !conn.isClosed()) {
                systemStatusLabel.setText("Database: Connected");
                systemStatusLabel.setStyle("-fx-text-fill: #4A90D9;");
                connectionIndicator.setProgress(1.0);
                systemHealthBar.setProgress(0.85);

                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
                if (rs.next()) {
                    int userCount = rs.getInt(1);
                    System.out.println("Users table found. Record count: " + userCount);
                }
                rs.close();
                stmt.close();
            }
        } catch (Exception e) {
            systemStatusLabel.setText("Database: Error");
            systemStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
            connectionIndicator.setProgress(0.0);
            System.err.println("Database test failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty()) { setStatus("Enter username"); return; }
        if (password.isEmpty()) { setStatus("Enter password"); return; }

        if (loginProgress != null) loginProgress.setVisible(true);
        setStatus("Authenticating...");

        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            Connection conn = db.getConnection();

            if (conn == null || conn.isClosed()) {
                setStatus("Database connection failed");
                UIUtils.showError("Connection Error", "Cannot connect to database.");
                if (loginProgress != null) loginProgress.setVisible(false);
                return;
            }

            // Use authenticate_user function (returns matching active user)
            String sql = "SELECT u.user_id, u.username, u.role, u.full_name, u.email, u.phone, u.is_active " +
                    "FROM users u WHERE u.username = ? AND u.password_hash = ? LIMIT 1";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                boolean isActive = rs.getBoolean("is_active");
                System.out.println("User found: " + username + ", Active: " + isActive);

                if (!isActive) {
                    if (loginProgress != null) loginProgress.setVisible(false);
                    setStatus("");
                    rs.close(); stmt.close();

                    UIUtils.showWarning("Account Deactivated",
                            "Your account has been deactivated by an administrator.\n\n" +
                                    "Please contact the HQ Department to have your account reinstated.\n\n" +
                                    "Email: admin@vis.ls  |  Ext: 1001");
                    setStatus("Account deactivated — contact HQ Department");
                    return;
                }

                User user = new User();
                user.setId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setRole(rs.getString("role"));
                user.setName(rs.getString("full_name"));

                App.setCurrentUser(user);
                if (loginProgress != null) loginProgress.setVisible(false);
                rs.close(); stmt.close();

                System.out.println("Login successful! Redirecting to dashboard...");
                App.switchScene("/com/example/assignment/dashboard.fxml", "Dashboard");

            } else {
                if (loginProgress != null) loginProgress.setVisible(false);
                setStatus("Invalid username or password");
                System.out.println("Login failed: User not found - " + username);
                rs.close(); stmt.close();
            }

        } catch (Exception e) {
            if (loginProgress != null) loginProgress.setVisible(false);
            setStatus("Database error: " + e.getMessage());
            System.err.println("Login error details: ");
            e.printStackTrace();
            UIUtils.showError("Login Error", "Login failed:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleExit(ActionEvent event) {
        UIUtils.showConfirmation("Confirm Exit",
                "Are you sure you want to exit?\nAny unsaved data will be lost.",
                confirmed -> {
                    if (confirmed) {
                        App app = new App();
                        app.performExit();
                    }
                });
    }

    private void setStatus(String msg) {
        if (statusLabel != null) {
            Platform.runLater(() -> statusLabel.setText(msg));
        }
    }
}