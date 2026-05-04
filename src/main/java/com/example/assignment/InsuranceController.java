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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.ResultSet;
import java.util.ResourceBundle;

/**
 * Insurance Controller - FIXED for fullscreen stability.
 * CHANGE: showConfirmation() uses callback pattern.
 */
public class InsuranceController implements Initializable {

    @FXML private TableView<InsurancePolicy> insuranceTable;
    @FXML private TableColumn<InsurancePolicy, Integer> colId;
    @FXML private TableColumn<InsurancePolicy, String> colVehicle;
    @FXML private TableColumn<InsurancePolicy, String> colCustomer;
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
    @FXML private ImageView vehicleImageView;
    @FXML private Label vehicleImageLabel;

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
        // ACCESS GUARD: Only Admin and Insurance can access Insurance
        if (!UIUtils.checkAccess("insurance")) return;

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colVehicle.setCellValueFactory(new PropertyValueFactory<>("vehicleLabel"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerLabel"));
        colPolicy.setCellValueFactory(new PropertyValueFactory<>("policyNumber"));
        colProvider.setCellValueFactory(new PropertyValueFactory<>("provider"));
        colCoverage.setCellValueFactory(new PropertyValueFactory<>("coverageType"));
        colStart.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        colEnd.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        colPremium.setCellValueFactory(new PropertyValueFactory<>("premium"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statusLabel"));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else if ("Active".equals(item)) { setText("Active"); setStyle("-fx-text-fill: #1DA462; -fx-font-weight: bold;"); }
                else { setText(item); setStyle("-fx-text-fill: #E53E3E; -fx-font-weight: bold;"); }
            }
        });

        coverageTypeCombo.getItems().addAll("Comprehensive", "Third Party Fire & Theft", "Third Party");

        DropShadow shadow = new DropShadow();
        shadow.setRadius(6); shadow.setSpread(0.2);
        shadow.setColor(Color.rgb(155, 89, 182, 0.3));
        if (saveBtn != null) saveBtn.setEffect(shadow);

        if (vehicleCombo != null) {
            vehicleCombo.valueProperty().addListener((obs, old, val) -> loadVehicleImage(val));
        }

        loadVehicles(); loadCustomers(); loadPolicies(); setupPagination();

        insuranceTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, newVal) -> {
                if (newVal != null) { selectedPolicy = newVal; populateFields(newVal); }
            });
    }

    private void loadVehicleImage(String vehicleComboVal) {
        if (vehicleImageView == null || vehicleComboVal == null) return;
        try {
            String regNo = vehicleComboVal.contains(" - ") ? vehicleComboVal.split(" - ")[1].split(" \\(")[0] : "";
            regNo = regNo.replaceAll("[\\s/\\\\]", "");
            for (String ext : new String[]{".png", ".jpg", ".jpeg"}) {
                URL imgUrl = getClass().getResource("/com/example/assignment/images/vehicles/" + regNo + ext);
                if (imgUrl != null) {
                    vehicleImageView.setImage(new Image(imgUrl.toExternalForm()));
                    if (vehicleImageLabel != null) vehicleImageLabel.setText(regNo);
                    return;
                }
            }
            vehicleImageView.setImage(null);
            if (vehicleImageLabel != null) vehicleImageLabel.setText("No image for " + regNo);
        } catch (Exception ignored) {}
    }

    private void loadPolicies() {
        policyList.clear();
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery(
                "SELECT ip.*, v.registration_number, v.make, v.model, c.name AS customer_name " +
                "FROM insurance_policies ip " +
                "LEFT JOIN vehicles v ON ip.vehicle_id = v.vehicle_id " +
                "LEFT JOIN customers c ON ip.customer_id = c.customer_id " +
                "ORDER BY ip.policy_id");
            while (rs != null && rs.next()) {
                InsurancePolicy ip = new InsurancePolicy();
                ip.setId(rs.getInt("policy_id"));
                ip.setVehicleId(rs.getInt("vehicle_id"));
                ip.setCustomerId(rs.getInt("customer_id"));
                ip.setPolicyNumber(rs.getString("policy_number"));
                ip.setProvider(rs.getString("provider"));
                ip.setCoverageType(rs.getString("coverage_type"));
                ip.setStartDate(rs.getString("start_date"));
                ip.setEndDate(rs.getString("end_date"));
                ip.setPremium(rs.getBigDecimal("premium"));
                ip.setActive(rs.getBoolean("is_active"));
                ip.setVehicleLabel(rs.getString("registration_number") + " (" +
                    rs.getString("make") + " " + rs.getString("model") + ")");
                ip.setCustomerLabel(rs.getString("customer_name"));
                policyList.add(ip);
            }
            insuranceTable.setItems(policyList);
            if (recordCountLabel != null) recordCountLabel.setText("Total Records: " + policyList.size());
        } catch (Exception e) { UIUtils.showError("Error", "Failed to load policies: " + e.getMessage()); }
    }

    private void loadVehicles() {
        try {
            vehicleCombo.getItems().clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery(
                "SELECT vehicle_id, registration_number, make, model FROM vehicles ORDER BY registration_number");
            while (rs != null && rs.next()) {
                vehicleCombo.getItems().add(rs.getInt("vehicle_id") + " - " +
                    rs.getString("registration_number") + " (" +
                    rs.getString("make") + " " + rs.getString("model") + ")");
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
        if (pagination == null) return;
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

            if (vehicle == null || customer == null || policyNum.isEmpty() || provider.isEmpty()
                    || coverage == null || startDate.isEmpty() || endDate.isEmpty()) {
                UIUtils.showWarning("Validation", "All fields are required.");
                return;
            }
            if (!UIUtils.isValidDecimal(premiumStr)) {
                UIUtils.showWarning("Validation", "Premium must be a valid number (e.g. 350.00).");
                return;
            }

            int vehicleId = Integer.parseInt(vehicle.split(" - ")[0]);
            int customerId = Integer.parseInt(customer.split(" - ")[0]);
            BigDecimal premium = new BigDecimal(premiumStr);

            DatabaseConnection db = DatabaseConnection.getInstance();
            int result = db.executeParameterizedUpdate(
                "INSERT INTO insurance_policies (vehicle_id, customer_id, policy_number, provider, coverage_type, start_date, end_date, premium, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)",
                vehicleId, customerId, policyNum, provider, coverage,
                java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate), premium);

            if (result > 0) {
                UIUtils.showInfo("Success", "Insurance policy added!");
                clearFields(); loadPolicies(); setupPagination();
            }
        } catch (Exception e) {
            UIUtils.showError("Save Error", "Failed to add policy: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (selectedPolicy == null) { UIUtils.showWarning("No Selection", "Select a policy."); return; }
        // FIX: callback-based confirmation
        UIUtils.showConfirmation("Delete", "Delete this insurance policy?", confirmed -> {
            if (confirmed) {
                try {
                    DatabaseConnection db = DatabaseConnection.getInstance();
                    int result = db.executeParameterizedUpdate(
                        "DELETE FROM insurance_policies WHERE policy_id = ?", selectedPolicy.getId());
                    if (result > 0) { UIUtils.showInfo("Deleted", "Policy deleted."); loadPolicies(); setupPagination(); clearFields(); }
                } catch (Exception e) { UIUtils.showError("Delete Error", e.getMessage()); }
            }
        });
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) { loadPolicies(); return; }
        try {
            policyList.clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeParameterizedQuery(
                "SELECT ip.*, v.registration_number, v.make, v.model, c.name AS customer_name " +
                "FROM insurance_policies ip " +
                "LEFT JOIN vehicles v ON ip.vehicle_id = v.vehicle_id " +
                "LEFT JOIN customers c ON ip.customer_id = c.customer_id " +
                "WHERE ip.policy_number LIKE ? OR ip.provider LIKE ? OR ip.coverage_type LIKE ? OR v.registration_number LIKE ? OR c.name LIKE ?",
                "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%");
            while (rs != null && rs.next()) {
                InsurancePolicy ip = new InsurancePolicy();
                ip.setId(rs.getInt("policy_id"));
                ip.setVehicleId(rs.getInt("vehicle_id"));
                ip.setCustomerId(rs.getInt("customer_id"));
                ip.setPolicyNumber(rs.getString("policy_number"));
                ip.setProvider(rs.getString("provider"));
                ip.setCoverageType(rs.getString("coverage_type"));
                ip.setStartDate(rs.getString("start_date"));
                ip.setEndDate(rs.getString("end_date"));
                ip.setPremium(rs.getBigDecimal("premium"));
                ip.setActive(rs.getBoolean("is_active"));
                ip.setVehicleLabel(rs.getString("registration_number") + " (" +
                    rs.getString("make") + " " + rs.getString("model") + ")");
                ip.setCustomerLabel(rs.getString("customer_name"));
                policyList.add(ip);
            }
            insuranceTable.setItems(policyList);
            if (recordCountLabel != null) recordCountLabel.setText("Search Results: " + policyList.size());
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

        if (vehicleCombo != null) {
            String matchVehicle = vehicleCombo.getItems().stream()
                .filter(item -> item.startsWith(ip.getVehicleId() + " - "))
                .findFirst()
                .orElse(null);
            vehicleCombo.setValue(matchVehicle);
        }

        if (customerCombo != null) {
            String matchCustomer = customerCombo.getItems().stream()
                .filter(item -> item.startsWith(ip.getCustomerId() + " - "))
                .findFirst()
                .orElse(null);
            customerCombo.setValue(matchCustomer);
        }

        try {
            if (ip.getStartDate() != null && !ip.getStartDate().isEmpty())
                startDatePicker.setValue(java.time.LocalDate.parse(ip.getStartDate()));
            if (ip.getEndDate() != null && !ip.getEndDate().isEmpty())
                endDatePicker.setValue(java.time.LocalDate.parse(ip.getEndDate()));
        } catch (Exception ignored) {}
    }

    private void clearFields() {
        vehicleCombo.getSelectionModel().clearSelection();
        customerCombo.getSelectionModel().clearSelection();
        policyNumberField.clear(); providerField.clear();
        coverageTypeCombo.getSelectionModel().clearSelection();
        startDatePicker.setValue(null); endDatePicker.setValue(null);
        premiumField.clear();
        if (vehicleImageView != null) vehicleImageView.setImage(null);
        if (vehicleImageLabel != null) vehicleImageLabel.setText("Select a vehicle to see image");
        selectedPolicy = null;
    }
}
