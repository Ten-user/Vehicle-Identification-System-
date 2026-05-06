package com.example.assignment;

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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.sql.ResultSet;
import java.util.ResourceBundle;

public class QueriesController implements Initializable {

    @FXML private TableView<CustomerQuery> queryTable;
    @FXML private TableColumn<CustomerQuery, Integer> colId;
    @FXML private TableColumn<CustomerQuery, String> colCustomer;
    @FXML private TableColumn<CustomerQuery, String> colVehicle;
    @FXML private TableColumn<CustomerQuery, String> colDate;
    @FXML private TableColumn<CustomerQuery, String> colQuery;
    @FXML private TableColumn<CustomerQuery, String> colStatus;

    @FXML private ComboBox<String> customerCombo;
    @FXML private ComboBox<String> vehicleCombo;
    @FXML private DatePicker queryDatePicker;
    @FXML private TextArea queryTextArea;
    @FXML private TextArea responseTextArea;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextField searchField;

    @FXML private Button saveBtn;
    @FXML private Button respondBtn;
    @FXML private Button deleteBtn;
    @FXML private Button clearBtn;
    @FXML private Button searchBtn;
    @FXML private Button backBtn;
    @FXML private Label recordCountLabel;
    @FXML private Pagination pagination;
    @FXML private ImageView vehicleImageView;
    @FXML private Label vehicleImageLabel;
    @FXML private Label statusLabel;
    @FXML private Button refreshBtn;

    @FXML private javafx.scene.control.ScrollPane formPane;
    @FXML private javafx.scene.layout.HBox actionBtnsBox;
    @FXML private SplitPane mainSplitPane;

    private ObservableList<CustomerQuery> queryList = FXCollections.observableArrayList();
    private CustomerQuery selectedQuery;
    private static final int ROWS_PER_PAGE = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!UIUtils.checkAccess("queries")) return;

        // Table bindings
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colVehicle.setCellValueFactory(new PropertyValueFactory<>("vehicleLabel")); // ✅ FIXED
        colDate.setCellValueFactory(new PropertyValueFactory<>("recordDate"));
        colQuery.setCellValueFactory(new PropertyValueFactory<>("queryText"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Status coloring
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "Pending" -> setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        case "In Progress" -> setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                        case "Resolved" -> setStyle("-fx-text-fill: #27ae60;");
                        default -> setStyle("-fx-text-fill: #7f8c8d;");
                    }
                }
            }
        });

        if (statusCombo != null) {
            statusCombo.getItems().addAll("Pending", "In Progress", "Resolved", "Closed");
        }

        // Button styling
        DropShadow shadow = new DropShadow();
        shadow.setRadius(6);
        shadow.setSpread(0.2);
        shadow.setColor(Color.rgb(52, 152, 219, 0.3));
        if (saveBtn != null) saveBtn.setEffect(shadow);

        // ── ROLE-BASED SETUP ──
        User currentUser = App.getCurrentUser();
        boolean isCustomer = currentUser != null && "Customer".equalsIgnoreCase(currentUser.getRole());

        if (isCustomer) {
            // Hide customerCombo — customer can only submit for themselves
            if (customerCombo != null) { customerCombo.setVisible(false); customerCombo.setManaged(false); }
            // Hide Respond, Delete buttons
            if (respondBtn != null) { respondBtn.setVisible(false); respondBtn.setManaged(false); }
            if (deleteBtn != null)  { deleteBtn.setVisible(false);  deleteBtn.setManaged(false); }
            // Status: read-only for customer (display only)
            if (statusCombo != null) { statusCombo.setDisable(true); }
            if (statusLabel != null) { statusLabel.setText("Status (set by admin)"); }
            // Response: display only, admin fills it
            if (responseTextArea != null) {
                responseTextArea.setEditable(false);
                responseTextArea.setStyle(responseTextArea.getStyle() + "-fx-opacity: 0.75;");
                responseTextArea.setPromptText("Admin response will appear here once processed");
            }
        }

        loadVehicles();
        if (!isCustomer) loadCustomers();
        vehicleCombo.valueProperty().addListener((obs, old, val) -> { if (val != null) loadVehicleImage(val); });
        loadQueries();
        setupPagination();

        // Table selection
        queryTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, newVal) -> {
                    if (newVal != null) {
                        selectedQuery = newVal;
                        populateFields(newVal);
                    }
                });
    }

    private void loadVehicleImage(String regOrCombo) {
        if (vehicleImageView == null || regOrCombo == null) return;
        try {
            String safe = regOrCombo.replaceAll("^\\d+ - ", "").replaceAll("[\\s/\\\\]", "");
            for (String ext : new String[]{".png", ".jpg", ".jpeg"}) {
                URL url = getClass().getResource("/com/example/assignment/images/vehicles/" + safe + ext);
                if (url != null) {
                    vehicleImageView.setImage(new Image(url.toExternalForm()));
                    if (vehicleImageLabel != null) vehicleImageLabel.setText(safe);
                    return;
                }
            }
            vehicleImageView.setImage(null);
            if (vehicleImageLabel != null) vehicleImageLabel.setText("No image for " + safe);
        } catch (Exception ignored) {}
    }

    private void loadQueries() {
        queryList.clear();
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            User currentUser = App.getCurrentUser();
            boolean isCustomer = currentUser != null && "Customer".equalsIgnoreCase(currentUser.getRole());

            ResultSet rs;
            if (isCustomer) {
                // Look up this user's linked customer_id by matching full_name to customer name
                rs = db.executeParameterizedQuery(
                        "SELECT * FROM customer_query_overview WHERE customer_name = ?",
                        currentUser.getName());
            } else {
                rs = db.executeQuery("SELECT * FROM customer_query_overview");
            }

            while (rs != null && rs.next()) {
                CustomerQuery cq = new CustomerQuery();
                cq.setId(rs.getInt("query_id"));
                cq.setCustomerName(rs.getString("customer_name"));
                cq.setVehicleLabel(rs.getString("registration_number")); // ✅ FIXED
                cq.setRecordDate(rs.getString("query_date"));
                cq.setQueryText(rs.getString("query_text"));
                cq.setResponseText(rs.getString("response_text"));
                cq.setStatus(rs.getString("status"));
                queryList.add(cq);
            }

            queryTable.setItems(queryList);
            if (recordCountLabel != null)
                recordCountLabel.setText("Total Records: " + queryList.size());

        } catch (Exception e) {
            UIUtils.showError("Error", "Failed to load queries: " + e.getMessage());
        }
    }

    private void loadCustomers() {
        try {
            if (customerCombo == null) return;

            customerCombo.getItems().clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery("SELECT customer_id, name FROM customers ORDER BY name");

            while (rs != null && rs.next()) {
                customerCombo.getItems().add(rs.getInt("customer_id") + " - " + rs.getString("name"));
            }
        } catch (Exception ignored) {}
    }

    private void loadVehicles() {
        try {
            if (vehicleCombo == null) return;
            vehicleCombo.getItems().clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            User currentUser = App.getCurrentUser();
            boolean isCustomer = currentUser != null && "Customer".equalsIgnoreCase(currentUser.getRole());
            ResultSet rs;
            if (isCustomer) {
                // Only show vehicles owned by this customer
                rs = db.executeParameterizedQuery(
                    "SELECT v.vehicle_id, v.registration_number FROM vehicles v " +
                    "JOIN customers c ON v.owner_id = c.customer_id " +
                    "WHERE c.name = ? ORDER BY v.registration_number",
                    currentUser.getName());
            } else {
                rs = db.executeQuery("SELECT vehicle_id, registration_number FROM vehicles ORDER BY registration_number");
            }
            while (rs != null && rs.next()) {
                vehicleCombo.getItems().add(rs.getInt("vehicle_id") + " - " + rs.getString("registration_number"));
            }
        } catch (Exception ignored) {}
    }

    private void setupPagination() {
        if (pagination == null) return;

        int pageCount = Math.max(1, (queryList.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);

        pagination.setPageFactory(pageIndex -> {
            int from = pageIndex * ROWS_PER_PAGE;
            int to = Math.min(from + ROWS_PER_PAGE, queryList.size());
            queryTable.setItems(FXCollections.observableArrayList(queryList.subList(from, to)));
            return queryTable;
        });
    }

    @FXML
    private void handleSave(ActionEvent event) {
        try {
            User currentUser = App.getCurrentUser();
            boolean isCustomer = currentUser != null && "Customer".equalsIgnoreCase(currentUser.getRole());

            int customerId;
            if (isCustomer) {
                // Resolve customer_id from customers table by matching the user's full name
                ResultSet crs = DatabaseConnection.getInstance().executeParameterizedQuery(
                        "SELECT customer_id FROM customers WHERE name = ? LIMIT 1",
                        currentUser.getName());
                if (crs == null || !crs.next()) {
                    UIUtils.showWarning("Not Linked", "Your user account is not linked to a customer record. Please contact an administrator.");
                    return;
                }
                customerId = crs.getInt("customer_id");
            } else {
                String customer = customerCombo.getSelectionModel().getSelectedItem();
                if (customer == null) {
                    UIUtils.showWarning("Validation", "Customer, Date, and Query are required.");
                    return;
                }
                customerId = Integer.parseInt(customer.split(" - ")[0]);
            }

            String vehicle = vehicleCombo.getSelectionModel().getSelectedItem();
            String date = queryDatePicker.getValue() != null ? queryDatePicker.getValue().toString() : "";
            String queryText = queryTextArea.getText().trim();

            if (date.isEmpty() || queryText.isEmpty()) {
                UIUtils.showWarning("Validation", "Date and Query are required.");
                return;
            }

            Integer vehicleId = (vehicle != null) ? Integer.parseInt(vehicle.split(" - ")[0]) : null;

            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet saveRs = db.executeParameterizedQuery(
                    "SELECT add_customer_query(?, ?, ?, ?, ?) AS new_id",
                    customerId, vehicleId, java.sql.Date.valueOf(date), queryText,
                    statusCombo.getValue() != null ? statusCombo.getValue() : "Pending");
            int newId = (saveRs != null && saveRs.next()) ? saveRs.getInt("new_id") : -1;

            if (newId > 0) {
                UIUtils.showInfo("Success", "Query submitted!");
                clearFields();
                loadQueries();
                setupPagination();
            }

        } catch (Exception e) {
            UIUtils.showError("Save Error", e.getMessage());
        }
    }

    @FXML
    private void handleRespond(ActionEvent event) {
        if (selectedQuery == null) {
            UIUtils.showWarning("No Selection", "Select a query.");
            return;
        }

        String response = responseTextArea.getText().trim();
        if (response.isEmpty()) {
            UIUtils.showWarning("Validation", "Enter a response.");
            return;
        }

        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            int result = db.executeParameterizedUpdate(
                    "UPDATE customer_queries SET response_text = ?, status = 'Resolved' WHERE query_id = ?",
                    response, selectedQuery.getId());

            if (result > 0) {
                UIUtils.showInfo("Success", "Response submitted!");
                loadQueries();
                clearFields();
            }

        } catch (Exception e) {
            UIUtils.showError("Error", e.getMessage());
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (selectedQuery == null) {
            UIUtils.showWarning("No Selection", "Select a query.");
            return;
        }

        UIUtils.showConfirmation("Delete", "Delete this query?", confirmed -> {
            if (confirmed) {
                try {
                    DatabaseConnection db = DatabaseConnection.getInstance();
                    int result = db.executeParameterizedUpdate(
                            "DELETE FROM customer_queries WHERE query_id = ?",
                            selectedQuery.getId());

                    if (result > 0) {
                        UIUtils.showInfo("Deleted", "Query deleted.");
                        loadQueries();
                        setupPagination();
                        clearFields();
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
        if (keyword.isEmpty()) {
            loadQueries();
            return;
        }

        try {
            queryList.clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            User currentUser = App.getCurrentUser();
            boolean isCustomer = currentUser != null && "Customer".equalsIgnoreCase(currentUser.getRole());

            ResultSet rs;
            if (isCustomer) {
                rs = db.executeParameterizedQuery(
                        "SELECT * FROM customer_query_overview WHERE customer_name = ? AND (query_text LIKE ? OR status LIKE ?)",
                        currentUser.getName(), "%" + keyword + "%", "%" + keyword + "%");
            } else {
                rs = db.executeParameterizedQuery(
                        "SELECT * FROM customer_query_overview WHERE customer_name LIKE ? OR query_text LIKE ? OR status LIKE ?",
                        "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%");
            }

            while (rs != null && rs.next()) {
                CustomerQuery cq = new CustomerQuery();
                cq.setId(rs.getInt("query_id"));
                cq.setCustomerName(rs.getString("customer_name"));
                cq.setVehicleLabel(rs.getString("registration_number"));
                cq.setRecordDate(rs.getString("query_date"));
                cq.setQueryText(rs.getString("query_text"));
                cq.setResponseText(rs.getString("response_text"));
                cq.setStatus(rs.getString("status"));
                queryList.add(cq);
            }

            queryTable.setItems(queryList);
            if (recordCountLabel != null)
                recordCountLabel.setText("Search Results: " + queryList.size());

        } catch (Exception e) {
            UIUtils.showError("Search Error", e.getMessage());
        }
    }

    @FXML private void handleClear(ActionEvent e) { clearFields(); }

    @FXML private void handleRefresh(ActionEvent e) { loadQueries(); setupPagination(); }

    @FXML
    private void handleBack(ActionEvent e) {
        try {
            App.switchScene("/com/example/assignment/dashboard.fxml", "Dashboard");
        } catch (Exception ex) {
            UIUtils.showError("Error", ex.getMessage());
        }
    }

    private void populateFields(CustomerQuery cq) {
        if (cq.getVehicleLabel() != null) loadVehicleImage(cq.getVehicleLabel());
        queryTextArea.setText(cq.getQueryText() != null ? cq.getQueryText() : "");
        if (responseTextArea != null) responseTextArea.setText(cq.getResponseText() != null ? cq.getResponseText() : "");
        if (statusCombo != null) statusCombo.setValue(cq.getStatus());
        // Restore date picker
        if (cq.getRecordDate() != null && !cq.getRecordDate().isEmpty()) {
            try { queryDatePicker.setValue(java.time.LocalDate.parse(cq.getRecordDate())); } catch (Exception ignored) {}
        }
        // Restore customer combo
        if (customerCombo != null && cq.getCustomerName() != null) {
            customerCombo.getItems().stream()
                .filter(item -> item.contains(cq.getCustomerName()))
                .findFirst()
                .ifPresent(customerCombo::setValue);
        }
        // Restore vehicle combo
        if (vehicleCombo != null && cq.getVehicleLabel() != null) {
            vehicleCombo.getItems().stream()
                .filter(item -> item.contains(cq.getVehicleLabel()))
                .findFirst()
                .ifPresent(vehicleCombo::setValue);
        }
    }

    private void clearFields() {
        if (customerCombo != null) customerCombo.getSelectionModel().clearSelection();
        if (vehicleCombo != null) vehicleCombo.getSelectionModel().clearSelection();
        queryDatePicker.setValue(null);
        queryTextArea.clear();
        responseTextArea.clear();
        if (statusCombo != null) statusCombo.getSelectionModel().clearSelection();
        selectedQuery = null;
    }
}