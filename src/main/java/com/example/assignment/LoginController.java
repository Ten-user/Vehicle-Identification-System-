package com.example.assignment;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private VBox loginContainer;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator loginProgress;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(15);
        dropShadow.setSpread(0.3);
        dropShadow.setColor(Color.rgb(0, 120, 215, 0.3));
        loginContainer.setEffect(dropShadow);
        loginProgress.setVisible(false);
        passwordField.setOnAction(e -> handleLogin(e));
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty()) { statusLabel.setText("Enter username"); return; }
        if (password.isEmpty()) { statusLabel.setText("Enter password"); return; }

        loginProgress.setVisible(true);
        statusLabel.setText("Authenticating...");

        try {
            // Direct query — avoids stored procedure timezone bug in MySQL Connector/J
            String sql = "SELECT user_id, username, role, full_name, email, phone " +
                         "FROM users WHERE username = ? AND password_hash = ? LIMIT 1";

            PreparedStatement stmt = DatabaseConnection.getInstance()
                    .getConnection().prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setRole(rs.getString("role"));
                user.setName(rs.getString("full_name"));

                App.setCurrentUser(user);
                loginProgress.setVisible(false);
                App.switchScene("/com/example/assignment/dashboard.fxml", "Dashboard");
            } else {
                loginProgress.setVisible(false);
                statusLabel.setText("Invalid username or password");
            }

            rs.close();
            stmt.close();

        } catch (Exception e) {
            loginProgress.setVisible(false);
            statusLabel.setText("Database error");
            UIUtils.showError("Login Error", "Login failed:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleExit(ActionEvent event) {
        if (UIUtils.showConfirmation("Exit", "Are you sure?")) {
            System.exit(0);
        }
    }
}
