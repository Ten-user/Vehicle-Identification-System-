package com.example.assignment;

import com.example.assignment.App;
import com.example.assignment.User;
import com.example.assignment.DatabaseConnection;
import com.example.assignment.UIUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

import java.net.URL;
import java.sql.ResultSet;
import java.util.ResourceBundle;

/**
 * Users Controller - Admin Module.
 * Manages system users - only accessible by Admin role.
 * Uses stored procedure: register_user.
 */
public class UsersController implements Initializable {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colActive;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField searchField;

    @FXML private Button saveBtn;
    @FXML private Button toggleActiveBtn;
    @FXML private Button deleteBtn;
    @FXML private Button clearBtn;
    @FXML private Button searchBtn;
    @FXML private Button backBtn;
    @FXML private Label recordCountLabel;
    @FXML private Pagination pagination;

    private ObservableList<User> userList = FXCollections.observableArrayList();
    private User selectedUser;
    private static final int ROWS_PER_PAGE = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        roleCombo.getItems().addAll("Admin", "Workshop", "Customer", "Insurance", "Police");

        DropShadow shadow = new DropShadow();
        shadow.setRadius(6); shadow.setSpread(0.2);
        shadow.setColor(Color.rgb(52, 73, 94, 0.3));
        saveBtn.setEffect(shadow);

        // Check admin access
        User current = App.getCurrentUser();
        if (current == null || !current.isAdmin()) {
            UIUtils.showError("Access Denied", "Only administrators can manage users.");
            try { App.switchScene("/com/example/assignment/dashboard.fxml", "Dashboard"); } catch (Exception e) { /* ignore */ }
            return;
        }

        loadUsers();
        setupPagination();

        userTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, newVal) -> {
                if (newVal != null) { selectedUser = newVal; populateFields(newVal); }
            });
    }

    private void loadUsers() {
        userList.clear();
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery("SELECT * FROM users ORDER BY user_id");
            while (rs != null && rs.next()) {
                User u = new User();
                u.setId(rs.getInt("user_id"));
                u.setUsername(rs.getString("username"));
                u.setRole(rs.getString("role"));
                u.setName(rs.getString("full_name"));
                u.setEmail(rs.getString("email"));
                u.setPhone(rs.getString("phone"));
                u.setActive(rs.getBoolean("is_active"));
                userList.add(u);
            }
            userTable.setItems(userList);
            recordCountLabel.setText("Total Users: " + userList.size());
        } catch (Exception e) {
            UIUtils.showError("Error", "Failed to load users: " + e.getMessage());
        }
    }

    private void setupPagination() {
        int pageCount = Math.max(1, (userList.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);
        pagination.setPageFactory(pageIndex -> {
            int from = pageIndex * ROWS_PER_PAGE;
            int to = Math.min(from + ROWS_PER_PAGE, userList.size());
            userTable.setItems(FXCollections.observableArrayList(userList.subList(from, to)));
            return userTable;
        });
    }

    @FXML
    private void handleSave(ActionEvent event) {
        try {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            String role = roleCombo.getSelectionModel().getSelectedItem();
            String fullName = fullNameField.getText().trim();

            if (username.isEmpty() || password.isEmpty() || role == null || fullName.isEmpty()) {
                UIUtils.showWarning("Validation", "Username, Password, Role, and Full Name are required.");
                return;
            }

            // Call stored procedure: register_user
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.callProcedure("register_user",
                username, password, role, fullName,
                emailField.getText().trim(), phoneField.getText().trim());

            if (rs != null && rs.next()) {
                UIUtils.showInfo("Success", "User registered! ID: " + rs.getInt(1));
                clearFields(); loadUsers(); setupPagination();
            }
        } catch (Exception e) {
            UIUtils.showError("Save Error", "Failed to register user: " + e.getMessage());
        }
    }

    @FXML
    private void handleToggleActive(ActionEvent event) {
        if (selectedUser == null) { UIUtils.showWarning("No Selection", "Select a user."); return; }
        try {
            boolean newStatus = !selectedUser.isActive();
            DatabaseConnection db = DatabaseConnection.getInstance();
            int result = db.executeParameterizedUpdate("UPDATE users SET is_active = ? WHERE user_id = ?",
                newStatus, selectedUser.getId());
            if (result > 0) {
                UIUtils.showInfo("Success", "User " + (newStatus ? "activated" : "deactivated") + "!");
                loadUsers();
            }
        } catch (Exception e) { UIUtils.showError("Error", e.getMessage()); }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (selectedUser == null) { UIUtils.showWarning("No Selection", "Select a user."); return; }
        if (UIUtils.showConfirmation("Delete", "Delete user " + selectedUser.getName() + "?")) {
            try {
                DatabaseConnection db = DatabaseConnection.getInstance();
                int result = db.executeParameterizedUpdate("DELETE FROM users WHERE user_id = ?", selectedUser.getId());
                if (result > 0) { UIUtils.showInfo("Deleted", "User deleted."); loadUsers(); setupPagination(); clearFields(); }
            } catch (Exception e) { UIUtils.showError("Delete Error", e.getMessage()); }
        }
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) { loadUsers(); return; }
        try {
            userList.clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeParameterizedQuery(
                "SELECT * FROM users WHERE username LIKE ? OR full_name LIKE ? OR role LIKE ?",
                "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%");
            while (rs != null && rs.next()) {
                User u = new User();
                u.setId(rs.getInt("user_id")); u.setUsername(rs.getString("username"));
                u.setRole(rs.getString("role")); u.setName(rs.getString("full_name"));
                u.setEmail(rs.getString("email")); u.setActive(rs.getBoolean("is_active"));
                userList.add(u);
            }
            userTable.setItems(userList);
            recordCountLabel.setText("Search Results: " + userList.size());
        } catch (Exception e) { UIUtils.showError("Search Error", e.getMessage()); }
    }

    @FXML private void handleClear(ActionEvent e) { clearFields(); }
    @FXML private void handleBack(ActionEvent e) {
        try { App.switchScene("/com/example/assignment/dashboard.fxml", "Dashboard"); }
        catch (Exception ex) { UIUtils.showError("Error", ex.getMessage()); }
    }

    private void populateFields(User u) {
        usernameField.setText(u.getUsername());
        roleCombo.setValue(u.getRole());
        fullNameField.setText(u.getName());
        emailField.setText(u.getEmail());
        phoneField.setText(u.getPhone());
    }

    private void clearFields() {
        usernameField.clear(); passwordField.clear();
        roleCombo.getSelectionModel().clearSelection();
        fullNameField.clear(); emailField.clear(); phoneField.clear();
        selectedUser = null;
    }
}
