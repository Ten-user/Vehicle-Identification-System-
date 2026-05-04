package com.example.assignment;

import javafx.beans.property.SimpleStringProperty;
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
 * Users Controller - FIXED for fullscreen stability.
 * CHANGE: showConfirmation() uses callback pattern.
 */
public class UsersController implements Initializable {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String>  colUsername;
    @FXML private TableColumn<User, String>  colRole;
    @FXML private TableColumn<User, String>  colName;
    @FXML private TableColumn<User, String>  colEmail;
    @FXML private TableColumn<User, String>  colPhone;
    @FXML private TableColumn<User, String>  colActive;

    @FXML private TextField       usernameField;
    @FXML private PasswordField   passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private TextField       fullNameField;
    @FXML private TextField       emailField;
    @FXML private ComboBox<String> countryCodeCombo;
    @FXML private TextField       phoneField;
    @FXML private TextField       searchField;

    @FXML private Button saveBtn;
    @FXML private Button toggleActiveBtn;
    @FXML private Button deleteBtn;
    @FXML private Button clearBtn;
    @FXML private Button searchBtn;
    @FXML private Button backBtn;
    @FXML private Label  recordCountLabel;
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
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colActive.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().isActive() ? "Active" : "Inactive"));

        colActive.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else if ("Active".equals(item)) { setText("Active"); setStyle("-fx-text-fill: #1DA462; -fx-font-weight: bold;"); }
                else { setText("Inactive"); setStyle("-fx-text-fill: #E53E3E; -fx-font-weight: bold;"); }
            }
        });

        roleCombo.getItems().addAll("Admin", "Workshop", "Customer", "Insurance", "Police");

        countryCodeCombo.getItems().addAll(UIUtils.COUNTRY_CODES);
        countryCodeCombo.setValue(UIUtils.DEFAULT_COUNTRY_CODE);

        UIUtils.restrictToLetters(fullNameField);
        UIUtils.restrictToPhone(phoneField);

        if (saveBtn != null) {
            DropShadow shadow = new DropShadow();
            shadow.setRadius(6); shadow.setSpread(0.2);
            shadow.setColor(Color.rgb(26, 95, 200, 0.3));
            saveBtn.setEffect(shadow);
        }

        // ACCESS GUARD: Only Admin can access User Management
        if (!UIUtils.checkAccess("users")) return;

        loadUsers();
        setupPagination();

        userTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, newVal) -> { if (newVal != null) { selectedUser = newVal; populateFields(newVal); } });
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
            if (recordCountLabel != null) recordCountLabel.setText("Total Users: " + userList.size());
        } catch (Exception e) {
            UIUtils.showError("Error", "Failed to load users: " + e.getMessage());
        }
    }

    private void setupPagination() {
        if (pagination == null) return;
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

    @FXML private void handleSave(ActionEvent event) {
        try {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            String role     = roleCombo.getSelectionModel().getSelectedItem();
            String fullName = fullNameField.getText().trim();
            String email    = emailField.getText().trim();
            String phone    = UIUtils.buildFullPhone(countryCodeCombo.getValue(), phoneField.getText());

            if (username.isEmpty() || password.isEmpty() || role == null || fullName.isEmpty()) {
                UIUtils.showWarning("Validation", "Username, Password, Role, and Full Name are required."); return;
            }
            if (!UIUtils.isLettersOnly(fullName)) {
                UIUtils.showWarning("Validation", "Full Name must contain letters only — no numbers."); return;
            }
            if (!email.isEmpty() && !UIUtils.isValidEmail(email)) {
                UIUtils.showWarning("Validation", "Please enter a valid email address (e.g. name@domain.com)."); return;
            }

            DatabaseConnection db = DatabaseConnection.getInstance();
            int result = db.executeParameterizedUpdate(
                "INSERT INTO users (username, password_hash, role, full_name, email, phone, is_active) VALUES (?, ?, ?, ?, ?, ?, 1)",
                username, password, role, fullName, email, phone);
            if (result > 0) {
                UIUtils.showInfo("Success", "User registered successfully!");
                clearFields(); loadUsers(); setupPagination();
            }
        } catch (Exception e) { UIUtils.showError("Save Error", e.getMessage()); }
    }

    @FXML private void handleToggleActive(ActionEvent event) {
        if (selectedUser == null) { UIUtils.showWarning("No Selection", "Select a user."); return; }
        try {
            boolean newStatus = !selectedUser.isActive();
            DatabaseConnection db = DatabaseConnection.getInstance();
            int result = db.executeParameterizedUpdate(
                "UPDATE users SET is_active = ? WHERE user_id = ?", newStatus, selectedUser.getId());
            if (result > 0) {
                UIUtils.showInfo("Success", "User " + (newStatus ? "activated" : "deactivated") + ".");
                loadUsers(); clearFields();
            }
        } catch (Exception e) { UIUtils.showError("Error", e.getMessage()); }
    }

    @FXML private void handleDelete(ActionEvent event) {
        if (selectedUser == null) { UIUtils.showWarning("No Selection", "Select a user."); return; }
        // FIX: callback-based confirmation
        UIUtils.showConfirmation("Delete", "Delete user " + selectedUser.getName() + "?", confirmed -> {
            if (confirmed) {
                try {
                    DatabaseConnection db = DatabaseConnection.getInstance();
                    int result = db.executeParameterizedUpdate("DELETE FROM users WHERE user_id = ?", selectedUser.getId());
                    if (result > 0) { UIUtils.showInfo("Deleted", "User deleted."); loadUsers(); setupPagination(); clearFields(); }
                } catch (Exception e) { UIUtils.showError("Delete Error", e.getMessage()); }
            }
        });
    }

    @FXML private void handleSearch(ActionEvent event) {
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
                u.setEmail(rs.getString("email")); u.setPhone(rs.getString("phone"));
                u.setActive(rs.getBoolean("is_active"));
                userList.add(u);
            }
            userTable.setItems(userList);
            if (recordCountLabel != null) recordCountLabel.setText("Search Results: " + userList.size());
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
        emailField.setText(u.getEmail() != null ? u.getEmail() : "");
        String phone = u.getPhone() != null ? u.getPhone() : "";
        phoneField.setText(phone);
    }

    private void clearFields() {
        usernameField.clear(); passwordField.clear();
        roleCombo.getSelectionModel().clearSelection();
        fullNameField.clear(); emailField.clear(); phoneField.clear();
        countryCodeCombo.setValue(UIUtils.DEFAULT_COUNTRY_CODE);
        selectedUser = null;
    }
}
