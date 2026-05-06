package com.example.assignment;

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

    @FXML private Label recordCountLabel;
    @FXML private Pagination pagination;
    @FXML private Button refreshBtn;

    @FXML private javafx.scene.control.ScrollPane formPane;
    @FXML private SplitPane mainSplitPane;
    @FXML private javafx.scene.layout.HBox actionBtnsBox;

    private ObservableList<InsurancePolicy> policyList = FXCollections.observableArrayList();
    private InsurancePolicy selectedPolicy;
    private static final int ROWS_PER_PAGE = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else if ("Active".equals(item)) {
                    setText("Active");
                    setStyle("-fx-text-fill: #1DA462; -fx-font-weight: bold;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #E53E3E; -fx-font-weight: bold;");
                }
            }
        });

        coverageTypeCombo.getItems().addAll(
                "Comprehensive",
                "Third Party Fire & Theft",
                "Third Party"
        );

        DropShadow shadow = new DropShadow();
        shadow.setRadius(6);
        shadow.setSpread(0.2);
        shadow.setColor(Color.rgb(155, 89, 182, 0.3));

        loadVehicles();
        loadCustomers();
        loadPolicies();
        setupPagination();

        vehicleCombo.valueProperty().addListener((obs, old, val) -> loadVehicleImage(val));

        insuranceTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, newVal) -> {
                    if (newVal != null) {
                        selectedPolicy = newVal;
                        populateFields(newVal);
                    }
                });

        applyRoleUI();
    }

    private void applyRoleUI() {
        // Insurance and Admin: full access. No other roles reach this screen.
    }

    private void loadVehicleImage(String val) {
        if (vehicleImageView == null || val == null) return;

        try {
            String regNo = val.split(" - ")[1].split(" \\(")[0]
                    .replaceAll("[\\s/\\\\]", "");

            for (String ext : new String[]{".png", ".jpg", ".jpeg"}) {
                URL imgUrl = getClass().getResource(
                        "/com/example/assignment/images/vehicles/" + regNo + ext
                );
                if (imgUrl != null) {
                    vehicleImageView.setImage(new Image(imgUrl.toExternalForm()));
                    vehicleImageLabel.setText(regNo);
                    return;
                }
            }

            vehicleImageView.setImage(null);
            vehicleImageLabel.setText("No image for " + regNo);

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
                            "ORDER BY ip.policy_id"
            );

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

                ip.setVehicleLabel(rs.getString("registration_number") +
                        " (" + rs.getString("make") + " " + rs.getString("model") + ")");

                ip.setCustomerLabel(rs.getString("customer_name"));

                policyList.add(ip);
            }

            if (rs != null) rs.close();

            setupPagination();
            recordCountLabel.setText("Total Records: " + policyList.size());

        } catch (Exception e) {
            UIUtils.showError("Error", e.getMessage());
        }
    }

    private void loadVehicles() {
        try {
            vehicleCombo.getItems().clear();
            DatabaseConnection db = DatabaseConnection.getInstance();

            ResultSet rs = db.executeQuery(
                    "SELECT vehicle_id, registration_number, make, model FROM vehicles"
            );

            while (rs != null && rs.next()) {
                vehicleCombo.getItems().add(
                        rs.getInt("vehicle_id") + " - " +
                                rs.getString("registration_number") +
                                " (" + rs.getString("make") + " " + rs.getString("model") + ")"
                );
            }

            if (rs != null) rs.close();

        } catch (Exception ignored) {}
    }

    private void loadCustomers() {
        try {
            customerCombo.getItems().clear();
            DatabaseConnection db = DatabaseConnection.getInstance();

            ResultSet rs = db.executeQuery(
                    "SELECT customer_id, name FROM customers"
            );

            while (rs != null && rs.next()) {
                customerCombo.getItems().add(
                        rs.getInt("customer_id") + " - " + rs.getString("name")
                );
            }

            if (rs != null) rs.close();

        } catch (Exception ignored) {}
    }

    private void setupPagination() {
        if (pagination == null) return;

        int pageCount = (int) Math.ceil((double) policyList.size() / ROWS_PER_PAGE);
        pagination.setPageCount(Math.max(pageCount, 1));

        pagination.setPageFactory(pageIndex -> {
            int from = pageIndex * ROWS_PER_PAGE;
            int to = Math.min(from + ROWS_PER_PAGE, policyList.size());

            insuranceTable.setItems(
                    FXCollections.observableArrayList(policyList.subList(from, to))
            );

            return insuranceTable;
        });
    }

    @FXML
    private void handleSave(ActionEvent event) {
        try {
            String vehicle = vehicleCombo.getValue();
            String customer = customerCombo.getValue();

            if (vehicle == null || customer == null) {
                UIUtils.showWarning("Validation", "Select vehicle & customer");
                return;
            }

            int vehicleId = Integer.parseInt(vehicle.split(" - ")[0]);
            int customerId = Integer.parseInt(customer.split(" - ")[0]);

            DatabaseConnection db = DatabaseConnection.getInstance();

            ResultSet saveRs = db.executeParameterizedQuery(
                    "SELECT add_insurance_policy(?, ?, ?, ?, ?, ?, ?, ?) AS new_id",
                    vehicleId,
                    customerId,
                    policyNumberField.getText(),
                    providerField.getText(),
                    coverageTypeCombo.getValue(),
                    java.sql.Date.valueOf(startDatePicker.getValue()),
                    java.sql.Date.valueOf(endDatePicker.getValue()),
                    new BigDecimal(premiumField.getText())
            );
            int newId = (saveRs != null && saveRs.next()) ? saveRs.getInt("new_id") : -1;

            if (newId > 0) {
                UIUtils.showInfo("Success", "Saved!");
                clearFields();
                loadPolicies();
            }

        } catch (Exception e) {
            UIUtils.showError("Save Error", e.getMessage());
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (selectedPolicy == null) return;

        DatabaseConnection db = DatabaseConnection.getInstance();

        try {
            db.executeParameterizedUpdate(
                    "DELETE FROM insurance_policies WHERE policy_id = ?",
                    selectedPolicy.getId()
            );
        } catch (java.sql.SQLException e) {
            UIUtils.showError("Delete Error", e.getMessage());
            return;
        }

        loadPolicies();
        clearFields();
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String keyword = searchField.getText();

        if (keyword.isEmpty()) {
            loadPolicies();
            return;
        }

        try {
            policyList.clear();
            DatabaseConnection db = DatabaseConnection.getInstance();

            ResultSet rs = db.executeParameterizedQuery(
                    "SELECT ip.*, v.registration_number, v.make, v.model, c.name AS customer_name " +
                            "FROM insurance_policies ip " +
                            "LEFT JOIN vehicles v ON ip.vehicle_id = v.vehicle_id " +
                            "LEFT JOIN customers c ON ip.customer_id = c.customer_id " +
                            "WHERE ip.policy_number LIKE ? OR ip.provider LIKE ? OR ip.coverage_type LIKE ?",
                    "%" + keyword + "%",
                    "%" + keyword + "%",
                    "%" + keyword + "%"
            );

            while (rs != null && rs.next()) {
                InsurancePolicy ip = new InsurancePolicy();
                ip.setId(rs.getInt("policy_id"));
                ip.setPolicyNumber(rs.getString("policy_number"));
                ip.setProvider(rs.getString("provider"));
                policyList.add(ip);
            }

            if (rs != null) rs.close();

            setupPagination();
            recordCountLabel.setText("Search Results: " + policyList.size());

        } catch (Exception e) {
            UIUtils.showError("Search Error", e.getMessage());
        }
    }

    private void populateFields(InsurancePolicy ip) {
        policyNumberField.setText(ip.getPolicyNumber());
        providerField.setText(ip.getProvider());
        coverageTypeCombo.setValue(ip.getCoverageType());
        premiumField.setText(ip.getFormattedPremium());
        // Restore dates
        if (ip.getStartDate() != null && !ip.getStartDate().isEmpty()) {
            try { startDatePicker.setValue(java.time.LocalDate.parse(ip.getStartDate())); } catch (Exception ignored) {}
        }
        if (ip.getEndDate() != null && !ip.getEndDate().isEmpty()) {
            try { endDatePicker.setValue(java.time.LocalDate.parse(ip.getEndDate())); } catch (Exception ignored) {}
        }
        // Restore vehicle combo & image
        if (ip.getVehicleId() > 0 && vehicleCombo != null) {
            vehicleCombo.getItems().stream()
                .filter(item -> item.startsWith(ip.getVehicleId() + " - "))
                .findFirst()
                .ifPresent(v -> { vehicleCombo.setValue(v); loadVehicleImage(v); });
        }
        // Restore customer combo
        if (ip.getCustomerId() > 0 && customerCombo != null) {
            customerCombo.getItems().stream()
                .filter(item -> item.startsWith(ip.getCustomerId() + " - "))
                .findFirst()
                .ifPresent(customerCombo::setValue);
        }
    }

    private void clearFields() {
        vehicleCombo.getSelectionModel().clearSelection();
        customerCombo.getSelectionModel().clearSelection();
        policyNumberField.clear();
        providerField.clear();
        coverageTypeCombo.getSelectionModel().clearSelection();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        premiumField.clear();
        selectedPolicy = null;
    }

    @FXML private void handleClear(ActionEvent event) { clearFields(); }

    @FXML private void handleRefresh(ActionEvent event) { loadPolicies(); }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            App.switchScene("/com/example/assignment/dashboard.fxml", "Dashboard");
        } catch (Exception ex) {
            UIUtils.showError("Error", ex.getMessage());
        }
    }
}