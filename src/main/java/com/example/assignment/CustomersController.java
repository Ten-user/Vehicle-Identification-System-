package com.example.assignment;

import javafx.application.Platform;
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

public class CustomersController implements Initializable {

    @FXML private TableView<Customer> customerTable;
    @FXML private TableColumn<Customer, Integer> colId;
    @FXML private TableColumn<Customer, String> colName;
    @FXML private TableColumn<Customer, String> colAddress;
    @FXML private TableColumn<Customer, String> colPhone;
    @FXML private TableColumn<Customer, String> colEmail;
    @FXML private TableColumn<Customer, String> colIdNumber;

    @FXML private TextField nameField;
    @FXML private TextField addressField;
    @FXML private ComboBox<String> countryCodeCombo;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField idNumberField;
    @FXML private TextField searchField;

    @FXML private Button saveBtn;
    @FXML private Button updateBtn;
    @FXML private Button deleteBtn;
    @FXML private Button clearBtn;
    @FXML private Button searchBtn;
    @FXML private Button backBtn;
    @FXML private Label recordCountLabel;
    @FXML private Pagination pagination;
    @FXML private Button refreshBtn;

    @FXML private javafx.scene.control.ScrollPane formPane;
    @FXML private javafx.scene.layout.HBox actionBtnsBox;
    @FXML private SplitPane mainSplitPane;

    private ObservableList<Customer> customerList = FXCollections.observableArrayList();
    private Customer selectedCustomer;
    private static final int ROWS_PER_PAGE = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!UIUtils.checkAccess("customers")) return;

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colIdNumber.setCellValueFactory(new PropertyValueFactory<>("idNumber"));

        DropShadow shadow = new DropShadow();
        shadow.setRadius(6); shadow.setSpread(0.2);
        shadow.setColor(Color.rgb(0, 120, 215, 0.3));
        if (saveBtn != null) saveBtn.setEffect(shadow);

        UIUtils.restrictToLetters(nameField);
        UIUtils.restrictToPhone(phoneField);

        if (countryCodeCombo != null) {
            countryCodeCombo.getItems().addAll(UIUtils.COUNTRY_CODES);
            countryCodeCombo.setValue(UIUtils.DEFAULT_COUNTRY_CODE);
        }

        loadCustomers();
        setupPagination();

        customerTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, newVal) -> {
                    if (newVal != null) { selectedCustomer = newVal; populateFields(newVal); }
                });

        applyRoleUI();
    }

    private void applyRoleUI() {
        // Admin: full access. Insurance: view only (no add/update/delete).
        if (!UIUtils.isAdmin()) {
            UIUtils.hide(formPane);
            if (mainSplitPane != null)
                javafx.application.Platform.runLater(() -> mainSplitPane.setDividerPositions(1.0));
        }
    }

    private void loadCustomers() {
        customerList.clear();
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery("SELECT * FROM customers ORDER BY customer_id");
            while (rs != null && rs.next()) {
                Customer c = new Customer();
                c.setId(rs.getInt("customer_id"));
                c.setName(rs.getString("name"));
                c.setAddress(rs.getString("address"));
                c.setPhone(rs.getString("phone"));
                c.setEmail(rs.getString("email"));
                c.setIdNumber(rs.getString("id_number"));
                customerList.add(c);
            }
            customerTable.setItems(customerList);
            if (recordCountLabel != null) recordCountLabel.setText("Total Records: " + customerList.size());
        } catch (Exception e) {
            UIUtils.showError("Database Error", "Failed to load customers: " + e.getMessage());
        }
    }

    private void setupPagination() {
        if (pagination == null) return;
        int pageCount = Math.max(1, (customerList.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);
        pagination.setPageFactory(pageIndex -> {
            int from = pageIndex * ROWS_PER_PAGE;
            int to = Math.min(from + ROWS_PER_PAGE, customerList.size());
            customerTable.setItems(FXCollections.observableArrayList(customerList.subList(from, to)));
            return customerTable;
        });
    }

    @FXML
    private void handleSave(ActionEvent event) {
        try {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { UIUtils.showWarning("Validation", "Name is required."); return; }
            if (!UIUtils.isLettersOnly(name)) { UIUtils.showWarning("Validation", "Name must contain letters only."); return; }

            String email = emailField.getText().trim();
            if (!email.isEmpty() && !UIUtils.isValidEmail(email)) {
                UIUtils.showWarning("Validation", "Please enter a valid email."); return;
            }

            String phone = buildPhone();

            DatabaseConnection db = DatabaseConnection.getInstance();
            int result = db.executeParameterizedUpdate(
                    "INSERT INTO customers (name, address, phone, email, id_number) VALUES (?, ?, ?, ?, ?)",
                    name, addressField.getText().trim(), phone, email, idNumberField.getText().trim());

            if (result > 0) {
                UIUtils.showInfo("Success", "Customer added successfully!");
                clearFields(); loadCustomers(); setupPagination();
            }
        } catch (Exception e) {
            UIUtils.showError("Save Error", e.getMessage());
        }
    }

    @FXML
    private void handleUpdate(ActionEvent event) {
        if (selectedCustomer == null) { UIUtils.showWarning("No Selection", "Select a customer."); return; }
        try {
            String name = nameField.getText().trim();
            if (!UIUtils.isLettersOnly(name)) { UIUtils.showWarning("Validation", "Name must contain letters only."); return; }

            String email = emailField.getText().trim();
            if (!email.isEmpty() && !UIUtils.isValidEmail(email)) {
                UIUtils.showWarning("Validation", "Please enter a valid email."); return;
            }

            String phone = buildPhone();

            DatabaseConnection db = DatabaseConnection.getInstance();
            int result = db.executeParameterizedUpdate(
                    "UPDATE customers SET name=?, address=?, phone=?, email=?, id_number=? WHERE customer_id=?",
                    name, addressField.getText().trim(), phone, email, idNumberField.getText().trim(), selectedCustomer.getId());

            if (result > 0) {
                UIUtils.showInfo("Success", "Customer updated!");
                loadCustomers(); clearFields();
            }
        } catch (Exception e) {
            UIUtils.showError("Update Error", e.getMessage());
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (selectedCustomer == null) { UIUtils.showWarning("No Selection", "Select a customer."); return; }
        UIUtils.showConfirmation("Delete", "Delete customer " + selectedCustomer.getName() + "?", confirmed -> {
            if (confirmed) {
                try {
                    DatabaseConnection db = DatabaseConnection.getInstance();
                    int result = db.executeParameterizedUpdate(
                            "DELETE FROM customers WHERE customer_id = ?",
                            selectedCustomer.getId());

                    if (result > 0) {
                        UIUtils.showInfo("Deleted", "Customer deleted.");
                        loadCustomers(); setupPagination(); clearFields();
                    }
                } catch (Exception e) {
                    UIUtils.showError("Delete Error", e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) { loadCustomers(); return; }

        try {
            customerList.clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeParameterizedQuery(
                    "SELECT * FROM customers WHERE name LIKE ? OR email LIKE ? OR phone LIKE ? OR id_number LIKE ?",
                    "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%");

            while (rs != null && rs.next()) {
                Customer c = new Customer();
                c.setId(rs.getInt("customer_id"));
                c.setName(rs.getString("name"));
                c.setAddress(rs.getString("address"));
                c.setPhone(rs.getString("phone"));
                c.setEmail(rs.getString("email"));
                c.setIdNumber(rs.getString("id_number"));
                customerList.add(c);
            }

            customerTable.setItems(customerList);
            if (recordCountLabel != null)
                recordCountLabel.setText("Search Results: " + customerList.size());

        } catch (Exception e) {
            UIUtils.showError("Search Error", e.getMessage());
        }
    }

    private String buildPhone() {
        String code = countryCodeCombo != null ? countryCodeCombo.getValue() : null;
        String number = phoneField.getText().trim();
        return UIUtils.buildFullPhone(code, number);
    }

    @FXML private void handleClear(ActionEvent e) { clearFields(); }

    @FXML private void handleRefresh(ActionEvent e) { loadCustomers(); setupPagination(); }

    @FXML
    private void handleBack(ActionEvent e) {
        try {
            App.switchScene("/com/example/assignment/dashboard.fxml", "Dashboard");
        } catch (Exception ex) {
            UIUtils.showError("Error", ex.getMessage());
        }
    }

    private void populateFields(Customer c) {
        nameField.setText(c.getName());
        addressField.setText(c.getAddress());
        phoneField.setText(c.getPhone() != null ? c.getPhone() : "");
        emailField.setText(c.getEmail() != null ? c.getEmail() : "");
        idNumberField.setText(c.getIdNumber() != null ? c.getIdNumber() : "");
    }

    private void clearFields() {
        nameField.clear();
        addressField.clear();
        phoneField.clear();
        emailField.clear();
        idNumberField.clear();
        if (countryCodeCombo != null)
            countryCodeCombo.setValue(UIUtils.DEFAULT_COUNTRY_CODE);
        selectedCustomer = null;
    }
}