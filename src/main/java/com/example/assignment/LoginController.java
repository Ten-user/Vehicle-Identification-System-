package com.example.assignment;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

/**
 * Login Controller — FIXED for fullscreen stability.
 *
 * CHANGES:
 * 1. Replaced showCustomDialog() and showExitConfirmation() custom Stage dialogs
 *    with UIUtils overlay-based dialogs. The old custom Stages (even with initOwner)
 *    could cause the fullscreen to drop on some platforms.
 * 2. Removed all manual setFullScreen(true) calls — the App.java fullscreen
 *    property listener handles this automatically.
 */
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
            String sql = "SELECT user_id, username, role, full_name, email, phone, is_active " +
                    "FROM users WHERE username = ? AND password_hash = ? LIMIT 1";

            PreparedStatement stmt = DatabaseConnection.getInstance()
                    .getConnection().prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                boolean isActive = rs.getBoolean("is_active");

                if (!isActive) {
                    if (loginProgress != null) loginProgress.setVisible(false);
                    setStatus("");
                    rs.close(); stmt.close();

                    // FIX: Use overlay dialog instead of custom Stage
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
                App.switchScene("/com/example/assignment/dashboard.fxml", "Dashboard");

            } else {
                if (loginProgress != null) loginProgress.setVisible(false);
                setStatus("Invalid username or password");
                rs.close(); stmt.close();
            }

        } catch (Exception e) {
            if (loginProgress != null) loginProgress.setVisible(false);
            setStatus("Database error");
            // FIX: Use overlay dialog instead of custom Stage
            UIUtils.showError("Login Error", "Login failed:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleExit(ActionEvent event) {
        // FIX: Use overlay confirmation instead of custom Stage
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
        if (statusLabel != null) statusLabel.setText(msg);
    }
}
