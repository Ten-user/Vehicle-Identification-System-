package com.example.assignment;

import com.example.assignment.App;
import com.example.assignment.CustomerQuery;
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
 * Customer Queries Controller - FIXED for fullscreen stability.
 * CHANGE: showConfirmation() uses callback pattern.
 */
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

    private ObservableList<CustomerQuery> queryList = FXCollections.observableArrayList();
    private CustomerQuery selectedQuery;
    private static final int ROWS_PER_PAGE = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ACCESS GUARD: Only Admin and Customer can access Queries
        if (!UIUtils.checkAccess("queries")) return;

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("recordDate"));
        colQuery.setCellValueFactory(new PropertyValueFactory<>("queryText"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    if ("Pending".equals(item)) setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    else if ("In Progress".equals(item)) setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    else if ("Resolved".equals(item)) setStyle("-fx-text-fill: #27ae60;");
                    else setStyle("-fx-text-fill: #7f8c8d;");
                }
            }
        });

        statusCombo.getItems().addAll("Pending", "In Progress", "Resolved", "Closed");

        DropShadow shadow = new DropShadow();
        shadow.setRadius(6); shadow.setSpread(0.2);
        shadow.setColor(Color.rgb(52, 152, 219, 0.3));
        saveBtn.setEffect(shadow);

        loadCustomers();
        loadVehicles();
        loadQueries();
        setupPagination();

        queryTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, newVal) -> {
                if (newVal != null) { selectedQuery = newVal; populateFields(newVal); }
            });
    }

    private void loadQueries() {
        queryList.clear();
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery("SELECT * FROM customer_query_overview");
            while (rs != null && rs.next()) {
                CustomerQuery cq = new CustomerQuery();
                cq.setId(rs.getInt("query_id"));
                cq.setCustomerName(rs.getString("customer_name"));
                cq.setRecordDate(rs.getString("query_date"));
                cq.setQueryText(rs.getString("query_text"));
                cq.setResponseText(rs.getString("response_text"));
                cq.setStatus(rs.getString("status"));
                queryList.add(cq);
            }
            queryTable.setItems(queryList);
            recordCountLabel.setText("Total Records: " + queryList.size());
        } catch (Exception e) {
            UIUtils.showError("Error", "Failed to load queries: " + e.getMessage());
        }
    }

    private void loadCustomers() {
        try {
            customerCombo.getItems().clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery("SELECT customer_id, name FROM customers ORDER BY name");
            while (rs != null && rs.next()) {
                customerCombo.getItems().add(rs.getInt("customer_id") + " - " + rs.getString("name"));
            }
        } catch (Exception e) { /* silent */ }
    }

    private void loadVehicles() {
        try {
            vehicleCombo.getItems().clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery("SELECT vehicle_id, registration_number FROM vehicles ORDER BY registration_number");
            while (rs != null && rs.next()) {
                vehicleCombo.getItems().add(rs.getInt("vehicle_id") + " - " + rs.getString("registration_number"));
            }
        } catch (Exception e) { /* silent */ }
    }

    private void setupPagination() {
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
            String customer = customerCombo.getSelectionModel().getSelectedItem();
            String vehicle = vehicleCombo.getSelectionModel().getSelectedItem();
            String date = queryDatePicker.getValue() != null ? queryDatePicker.getValue().toString() : "";
            String queryText = queryTextArea.getText().trim();

            if (customer == null || date.isEmpty() || queryText.isEmpty()) {
                UIUtils.showWarning("Validation", "Customer, Date, and Query are required.");
                return;
            }

            int customerId = Integer.parseInt(customer.split(" - ")[0]);
            int vehicleId = vehicle != null ? Integer.parseInt(vehicle.split(" - ")[0]) : 0;

            Integer vIdParam = vehicleId > 0 ? vehicleId : null;
            DatabaseConnection db = DatabaseConnection.getInstance();
            int result = db.executeParameterizedUpdate(
                "INSERT INTO customer_queries (customer_id, vehicle_id, query_date, query_text, status) VALUES (?, ?, ?, ?, ?)",
                customerId, vIdParam, java.sql.Date.valueOf(date), queryText,
                statusCombo.getValue() != null ? statusCombo.getValue() : "Pending");
            if (result > 0) {
                UIUtils.showInfo("Success", "Query added!");
                clearFields(); loadQueries(); setupPagination();
            }
        } catch (Exception e) {
            UIUtils.showError("Save Error", "Failed to add query: " + e.getMessage());
        }
    }

    @FXML
    private void handleRespond(ActionEvent event) {
        if (selectedQuery == null) { UIUtils.showWarning("No Selection", "Select a query."); return; }
        String response = responseTextArea.getText().trim();
        if (response.isEmpty()) { UIUtils.showWarning("Validation", "Please enter a response."); return; }
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            int result = db.executeParameterizedUpdate(
                "UPDATE customer_queries SET response_text = ?, status = 'Resolved' WHERE query_id = ?",
                response, selectedQuery.getId());
            if (result > 0) {
                UIUtils.showInfo("Success", "Response submitted!");
                loadQueries(); clearFields();
            }
        } catch (Exception e) { UIUtils.showError("Error", e.getMessage()); }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (selectedQuery == null) { UIUtils.showWarning("No Selection", "Select a query."); return; }
        // FIX: callback-based confirmation
        UIUtils.showConfirmation("Delete", "Delete this query?", confirmed -> {
            if (confirmed) {
                try {
                    DatabaseConnection db = DatabaseConnection.getInstance();
                    int result = db.executeParameterizedUpdate("DELETE FROM customer_queries WHERE query_id = ?", selectedQuery.getId());
                    if (result > 0) { UIUtils.showInfo("Deleted", "Query deleted."); loadQueries(); setupPagination(); clearFields(); }
                } catch (Exception e) { UIUtils.showError("Delete Error", e.getMessage()); }
            }
        });
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) { loadQueries(); return; }
        try {
            queryList.clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeParameterizedQuery(
                "SELECT * FROM customer_query_overview WHERE customer_name LIKE ? OR query_text LIKE ? OR status LIKE ?",
                "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%");
            while (rs != null && rs.next()) {
                CustomerQuery cq = new CustomerQuery();
                cq.setId(rs.getInt("query_id")); cq.setCustomerName(rs.getString("customer_name"));
                cq.setRecordDate(rs.getString("query_date")); cq.setQueryText(rs.getString("query_text"));
                cq.setResponseText(rs.getString("response_text")); cq.setStatus(rs.getString("status"));
                queryList.add(cq);
            }
            queryTable.setItems(queryList);
            recordCountLabel.setText("Search Results: " + queryList.size());
        } catch (Exception e) { UIUtils.showError("Search Error", e.getMessage()); }
    }

    @FXML private void handleClear(ActionEvent e) { clearFields(); }
    @FXML private void handleBack(ActionEvent e) {
        try { App.switchScene("/com/example/assignment/dashboard.fxml", "Dashboard"); }
        catch (Exception ex) { UIUtils.showError("Error", ex.getMessage()); }
    }

    private void populateFields(CustomerQuery cq) {
        queryTextArea.setText(cq.getQueryText());
        responseTextArea.setText(cq.getResponseText());
        statusCombo.setValue(cq.getStatus());
    }

    private void clearFields() {
        customerCombo.getSelectionModel().clearSelection();
        vehicleCombo.getSelectionModel().clearSelection();
        queryDatePicker.setValue(null); queryTextArea.clear();
        responseTextArea.clear(); statusCombo.getSelectionModel().clearSelection();
        selectedQuery = null;
    }
}
