package com.example.assignment;

import com.example.assignment.App;
import com.example.assignment.InsurancePolicy;
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

import java.math.BigDecimal;
import java.net.URL;
import java.sql.ResultSet;
import java.util.ResourceBundle;

/**
 * Insurance Controller - Insurance Module.
 * Uses stored procedures for adding policies.
 */
public class InsuranceController implements Initializable {

    @FXML private TableView<InsurancePolicy> insuranceTable;
    @FXML private TableColumn<InsurancePolicy, Integer> colId;
    @FXML private TableColumn<InsurancePolicy, String> colPolicy;
    @FXML private TableColumn<InsurancePolicy, String> colProvider;
    @FXML private TableColumn<InsurancePolicy, String> colCoverage;
    @FXML private TableColumn<InsurancePolicy, String> colStart;
    @FXML private TableColumn<InsurancePolicy, String> colEnd;
    @FXML private TableColumn<InsurancePolicy, BigDecimal> colPremium;
    @FXML private TableColumn<InsurancePolicy, String> colStatus;

    @FXML private ComboBox<String> vehicleCombo;
    @FXML private ComboBox<String> customerCombo;
    @FXML private TextField policyNumberField;
    @FXML private TextField providerField;
    @FXML private ComboBox<String> coverageTypeCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField premiumField;
    @FXML private TextField searchField;

    @FXML private Button saveBtn;
    @FXML private Button deleteBtn;
    @FXML private Button clearBtn;
    @FXML private Button searchBtn;
    @FXML private Button backBtn;
    @FXML private Label recordCountLabel;
    @FXML private Pagination pagination;

    private ObservableList<InsurancePolicy> policyList = FXCollections.observableArrayList();
    private InsurancePolicy selectedPolicy;
    private static final int ROWS_PER_PAGE = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPolicy.setCellValueFactory(new PropertyValueFactory<>("policyNumber"));
        colProvider.setCellValueFactory(new PropertyValueFactory<>("provider"));
        colCoverage.setCellValueFactory(new PropertyValueFactory<>("coverageType"));
        colStart.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        colEnd.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        colPremium.setCellValueFactory(new PropertyValueFactory<>("premium"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("isActive"));

        coverageTypeCombo.getItems().addAll("Comprehensive", "Third Party Fire & Theft", "Third Party");

        DropShadow shadow = new DropShadow();
        shadow.setRadius(6); shadow.setSpread(0.2);
        shadow.setColor(Color.rgb(155, 89, 182, 0.3));
        saveBtn.setEffect(shadow);

        loadVehicles(); loadCustomers(); loadPolicies(); setupPagination();

        insuranceTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, newVal) -> {
                if (newVal != null) { selectedPolicy = newVal; populateFields(newVal); }
            });
    }

    private void loadPolicies() {
        policyList.clear();
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery("SELECT * FROM insurance_policies ORDER BY policy_id");
            while (rs != null && rs.next()) {
                InsurancePolicy ip = new InsurancePolicy();
                ip.setId(rs.getInt("policy_id")); ip.setVehicleId(rs.getInt("vehicle_id"));
                ip.setCustomerId(rs.getInt("customer_id"));
                ip.setPolicyNumber(rs.getString("policy_number"));
                ip.setProvider(rs.getString("provider"));
                ip.setCoverageType(rs.getString("coverage_type"));
                ip.setStartDate(rs.getString("start_date"));
                ip.setEndDate(rs.getString("end_date"));
                ip.setPremium(rs.getBigDecimal("premium"));
                ip.setActive(rs.getBoolean("is_active"));
                policyList.add(ip);
            }
            insuranceTable.setItems(policyList);
            recordCountLabel.setText("Total Records: " + policyList.size());
        } catch (Exception e) { UIUtils.showError("Error", "Failed to load policies: " + e.getMessage()); }
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

    private void setupPagination() {
        int pageCount = Math.max(1, (policyList.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);
        pagination.setPageFactory(pageIndex -> {
            int from = pageIndex * ROWS_PER_PAGE;
            int to = Math.min(from + ROWS_PER_PAGE, policyList.size());
            insuranceTable.setItems(FXCollections.observableArrayList(policyList.subList(from, to)));
            return insuranceTable;
        });
    }

    @FXML
    private void handleSave(ActionEvent event) {
        try {
            String vehicle = vehicleCombo.getSelectionModel().getSelectedItem();
            String customer = customerCombo.getSelectionModel().getSelectedItem();
            String policyNum = policyNumberField.getText().trim();
            String provider = providerField.getText().trim();
            String coverage = coverageTypeCombo.getSelectionModel().getSelectedItem();
            String startDate = startDatePicker.getValue() != null ? startDatePicker.getValue().toString() : "";
            String endDate = endDatePicker.getValue() != null ? endDatePicker.getValue().toString() : "";
            String premiumStr = premiumField.getText().trim();

            if (vehicle == null || customer == null || policyNum.isEmpty() || provider.isEmpty() || coverage == null || startDate.isEmpty() || endDate.isEmpty()) {
                UIUtils.showWarning("Validation", "All fields are required.");
                return;
            }

            int vehicleId = Integer.parseInt(vehicle.split(" - ")[0]);
            int customerId = Integer.parseInt(customer.split(" - ")[0]);
            BigDecimal premium = new BigDecimal(premiumStr);

            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.callProcedure("add_insurance_policy",
                vehicleId, customerId, policyNum, provider, coverage,
                java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate), premium);

            if (rs != null && rs.next()) {
                UIUtils.showInfo("Success", "Insurance policy added! ID: " + rs.getInt(1));
                clearFields(); loadPolicies(); setupPagination();
            }
        } catch (Exception e) {
            UIUtils.showError("Save Error", "Failed to add policy: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (selectedPolicy == null) { UIUtils.showWarning("No Selection", "Select a policy."); return; }
        if (UIUtils.showConfirmation("Delete", "Delete this insurance policy?")) {
            try {
                DatabaseConnection db = DatabaseConnection.getInstance();
                int result = db.executeParameterizedUpdate("DELETE FROM insurance_policies WHERE policy_id = ?", selectedPolicy.getId());
                if (result > 0) { UIUtils.showInfo("Deleted", "Policy deleted."); loadPolicies(); setupPagination(); clearFields(); }
            } catch (Exception e) { UIUtils.showError("Delete Error", e.getMessage()); }
        }
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) { loadPolicies(); return; }
        try {
            policyList.clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeParameterizedQuery(
                "SELECT * FROM insurance_policies WHERE policy_number LIKE ? OR provider LIKE ? OR coverage_type LIKE ?",
                "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%");
            while (rs != null && rs.next()) {
                InsurancePolicy ip = new InsurancePolicy();
                ip.setId(rs.getInt("policy_id")); ip.setPolicyNumber(rs.getString("policy_number"));
                ip.setProvider(rs.getString("provider")); ip.setCoverageType(rs.getString("coverage_type"));
                ip.setStartDate(rs.getString("start_date")); ip.setEndDate(rs.getString("end_date"));
                ip.setPremium(rs.getBigDecimal("premium")); ip.setActive(rs.getBoolean("is_active"));
                policyList.add(ip);
            }
            insuranceTable.setItems(policyList);
            recordCountLabel.setText("Search Results: " + policyList.size());
        } catch (Exception e) { UIUtils.showError("Search Error", e.getMessage()); }
    }

    @FXML private void handleClear(ActionEvent e) { clearFields(); }
    @FXML private void handleBack(ActionEvent e) {
        try { App.switchScene("/com/example/assignment/dashboard.fxml", "Dashboard"); }
        catch (Exception ex) { UIUtils.showError("Error", ex.getMessage()); }
    }

    private void populateFields(InsurancePolicy ip) {
        policyNumberField.setText(ip.getPolicyNumber());
        providerField.setText(ip.getProvider());
        coverageTypeCombo.setValue(ip.getCoverageType());
        premiumField.setText(ip.getFormattedPremium());
    }

    private void clearFields() {
        vehicleCombo.getSelectionModel().clearSelection();
        customerCombo.getSelectionModel().clearSelection();
        policyNumberField.clear(); providerField.clear();
        coverageTypeCombo.getSelectionModel().clearSelection();
        startDatePicker.setValue(null); endDatePicker.setValue(null);
        premiumField.clear(); selectedPolicy = null;
    }
}
