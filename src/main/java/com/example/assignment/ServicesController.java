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

import java.math.BigDecimal;
import java.net.URL;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.sql.ResultSet;
import java.util.ResourceBundle;

public class ServicesController implements Initializable {

    @FXML private TableView<ServiceRecord> serviceTable;
    @FXML private TableColumn<ServiceRecord, Integer> colId;
    @FXML private TableColumn<ServiceRecord, String> colVehicle;
    @FXML private TableColumn<ServiceRecord, String> colDate;
    @FXML private TableColumn<ServiceRecord, String> colType;
    @FXML private TableColumn<ServiceRecord, String> colDesc;
    @FXML private TableColumn<ServiceRecord, BigDecimal> colCost;
    @FXML private TableColumn<ServiceRecord, String> colWorkshop;
    @FXML private TableColumn<ServiceRecord, String> colNextService;

    @FXML private ComboBox<String> vehicleCombo;
    @FXML private DatePicker serviceDatePicker;
    @FXML private ComboBox<String> serviceTypeCombo;
    @FXML private TextArea descriptionArea;
    @FXML private TextField costField;
    @FXML private TextField workshopField;
    @FXML private TextField mileageField;
    @FXML private DatePicker nextServiceDatePicker;
    @FXML private TextField searchField;

    @FXML private Button saveBtn;
    @FXML private Button deleteBtn;
    @FXML private Button clearBtn;
    @FXML private Button searchBtn;
    @FXML private Button backBtn;
    @FXML private Button refreshBtn;
    @FXML private ImageView vehicleImageView;
    @FXML private Label vehicleImageLabel;
    @FXML private Label recordCountLabel;
    @FXML private Label totalCostLabel;
    @FXML private Pagination pagination;

    @FXML private javafx.scene.control.ScrollPane formPane;
    @FXML private SplitPane mainSplitPane;
    @FXML private javafx.scene.layout.HBox actionBtnsBox;

    private ObservableList<ServiceRecord> serviceList = FXCollections.observableArrayList();
    private ServiceRecord selectedService;
    private static final int ROWS_PER_PAGE = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!UIUtils.checkAccess("services")) return;

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colVehicle.setCellValueFactory(new PropertyValueFactory<>("vehicleLabel")); // ✅ FIX
        colDate.setCellValueFactory(new PropertyValueFactory<>("recordDate"));
        colType.setCellValueFactory(new PropertyValueFactory<>("serviceType"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colCost.setCellValueFactory(new PropertyValueFactory<>("cost"));
        colWorkshop.setCellValueFactory(new PropertyValueFactory<>("workshopName"));
        colNextService.setCellValueFactory(new PropertyValueFactory<>("nextServiceDate"));

        serviceTypeCombo.getItems().addAll(
                "Oil Change", "Minor Service", "Major Service",
                "Brake Service", "Tire Rotation", "Engine Diagnostic",
                "Transmission Service", "Aircon Regas", "Battery Replacement",
                "Clutch Replacement", "Wheel Alignment", "Turbo Replacement",
                "Differential Service", "Timing Belt", "First Service"
        );

        DropShadow shadow = new DropShadow();
        shadow.setRadius(6);
        shadow.setSpread(0.2);
        shadow.setColor(Color.rgb(46, 204, 113, 0.3));
        if (saveBtn != null) saveBtn.setEffect(shadow);

        loadVehicles();
        vehicleCombo.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                String[] parts = val.split(" - ", 2);
                if (parts.length > 1) loadVehicleImage(parts[1].trim());
            }
        });
        loadServices();
        setupPagination();

        serviceTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, newVal) -> {
                    if (newVal != null) {
                        selectedService = newVal;
                        populateFields(newVal);
                    }
                });

        applyRoleUI();
    }

    private void applyRoleUI() {
        // Workshop and Admin: full access. Admin also has full access (via checkAccess).
        // Only workshop can write; admin can always write.
        // Since only workshop/admin can even reach this screen, no further hiding needed
        // beyond ensuring non-write roles see view-only.
        // (The access guard already prevents others from entering.)
    }

    private void loadServices() {
        serviceList.clear();
        BigDecimal totalCost = BigDecimal.ZERO;

        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery(
                    "SELECT * FROM vehicle_service_summary ORDER BY service_date DESC");

            while (rs != null && rs.next()) {
                ServiceRecord sr = new ServiceRecord();
                sr.setId(rs.getInt("service_id"));
                sr.setVehicleId(rs.getInt("vehicle_id"));
                sr.setVehicleLabel(rs.getString("registration_number"));
                sr.setRecordDate(rs.getString("service_date"));
                sr.setServiceType(rs.getString("service_type"));
                sr.setDescription(rs.getString("description"));
                sr.setCost(rs.getBigDecimal("cost"));
                sr.setWorkshopName(rs.getString("workshop_name"));
                sr.setMileage(rs.getInt("mileage"));
                sr.setNextServiceDate(rs.getString("next_service_date"));

                serviceList.add(sr);

                if (rs.getBigDecimal("cost") != null)
                    totalCost = totalCost.add(rs.getBigDecimal("cost"));
            }

            serviceTable.setItems(serviceList);

            if (recordCountLabel != null)
                recordCountLabel.setText("Total Records: " + serviceList.size());

            if (totalCostLabel != null)
                totalCostLabel.setText("Total Service Cost: M" + String.format("%,.2f", totalCost));

        } catch (Exception e) {
            UIUtils.showError("Error", "Failed to load services: " + e.getMessage());
        }
    }

    private void loadVehicleImage(String regNumber) {
        if (vehicleImageView == null || regNumber == null) return;
        try {
            String safe = regNumber.replaceAll("[\\s/\\\\]", "");
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

    private void loadVehicles() {
        try {
            if (vehicleCombo == null) return;

            vehicleCombo.getItems().clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery(
                    "SELECT vehicle_id, registration_number FROM vehicles ORDER BY registration_number");

            while (rs != null && rs.next()) {
                vehicleCombo.getItems().add(
                        rs.getInt("vehicle_id") + " - " + rs.getString("registration_number"));
            }
        } catch (Exception ignored) {}
    }

    private void setupPagination() {
        if (pagination == null) return;

        int pageCount = Math.max(1, (serviceList.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);

        pagination.setPageFactory(pageIndex -> {
            int from = pageIndex * ROWS_PER_PAGE;
            int to = Math.min(from + ROWS_PER_PAGE, serviceList.size());
            serviceTable.setItems(FXCollections.observableArrayList(serviceList.subList(from, to)));
            return serviceTable;
        });
    }

    @FXML
    private void handleSave(ActionEvent event) {
        try {
            String vehicle = vehicleCombo.getValue();
            String type = serviceTypeCombo.getValue();
            String date = serviceDatePicker.getValue() != null ? serviceDatePicker.getValue().toString() : "";
            String costText = costField.getText().trim();

            if (vehicle == null || type == null || date.isEmpty()) {
                UIUtils.showWarning("Validation", "Vehicle, Type and Date required.");
                return;
            }

            if (!UIUtils.isValidDecimal(costText)) {
                UIUtils.showWarning("Validation", "Invalid cost.");
                return;
            }

            int vehicleId = Integer.parseInt(vehicle.split(" - ")[0]);
            BigDecimal cost = new BigDecimal(costText);

            java.sql.Date nextDate = nextServiceDatePicker.getValue() != null
                    ? java.sql.Date.valueOf(nextServiceDatePicker.getValue()) : null;

            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet saveRs = db.executeParameterizedQuery(
                    "SELECT add_service_record(?, ?, ?, ?, ?, ?, ?, ?) AS new_id",
                    vehicleId,
                    java.sql.Date.valueOf(date),
                    type,
                    descriptionArea.getText().trim(),
                    cost,
                    workshopField.getText().trim(),
                    UIUtils.safeParseInt(mileageField.getText(), 0),
                    nextDate
            );
            int newId = (saveRs != null && saveRs.next()) ? saveRs.getInt("new_id") : -1;

            if (newId > 0) {
                UIUtils.showInfo("Success", "Service saved!");
                clearFields();
                loadServices();
                setupPagination();
            }

        } catch (Exception e) {
            UIUtils.showError("Save Error", e.getMessage());
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (selectedService == null) {
            UIUtils.showWarning("No Selection", "Select a record.");
            return;
        }

        UIUtils.showConfirmation("Delete", "Delete this record?", confirmed -> {
            if (confirmed) {
                try {
                    DatabaseConnection db = DatabaseConnection.getInstance();
                    db.executeParameterizedUpdate(
                            "DELETE FROM service_records WHERE service_id = ?",
                            selectedService.getId());

                    loadServices();
                    setupPagination();
                    clearFields();

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
            loadServices();
            return;
        }

        try {
            serviceList.clear();
            DatabaseConnection db = DatabaseConnection.getInstance();

            ResultSet rs = db.executeParameterizedQuery(
                    "SELECT * FROM vehicle_service_summary " +
                    "WHERE registration_number LIKE ? OR service_type LIKE ?",
                    "%" + keyword + "%", "%" + keyword + "%");

            while (rs != null && rs.next()) {
                ServiceRecord sr = new ServiceRecord();
                sr.setId(rs.getInt("service_id"));
                sr.setVehicleId(rs.getInt("vehicle_id"));
                sr.setVehicleLabel(rs.getString("registration_number"));
                sr.setRecordDate(rs.getString("service_date"));
                sr.setServiceType(rs.getString("service_type"));
                sr.setDescription(rs.getString("description"));
                sr.setCost(rs.getBigDecimal("cost"));
                sr.setWorkshopName(rs.getString("workshop_name"));
                sr.setMileage(rs.getInt("mileage"));
                sr.setNextServiceDate(rs.getString("next_service_date"));

                serviceList.add(sr);
            }

            serviceTable.setItems(serviceList);

            if (recordCountLabel != null)
                recordCountLabel.setText("Search Results: " + serviceList.size());

        } catch (Exception e) {
            UIUtils.showError("Search Error", e.getMessage());
        }
    }

    @FXML private void handleClear(ActionEvent e) { clearFields(); }

    @FXML private void handleRefresh(ActionEvent e) { loadServices(); setupPagination(); }

    @FXML
    private void handleBack(ActionEvent e) {
        try {
            App.switchScene("/com/example/assignment/dashboard.fxml", "Dashboard");
        } catch (Exception ex) {
            UIUtils.showError("Error", ex.getMessage());
        }
    }

    private void populateFields(ServiceRecord sr) {
        serviceTypeCombo.setValue(sr.getServiceType());
        descriptionArea.setText(sr.getDescription());
        costField.setText(sr.getFormattedCost());
        workshopField.setText(sr.getWorkshopName());
        mileageField.setText(String.valueOf(sr.getMileage()));
        // Load image from registration number
        if (sr.getVehicleLabel() != null) loadVehicleImage(sr.getVehicleLabel());
        // Restore vehicle combo selection
        String label = sr.getVehicleLabel();
        if (label != null && vehicleCombo != null) {
            vehicleCombo.getItems().stream()
                .filter(item -> item.contains(label))
                .findFirst()
                .ifPresent(vehicleCombo::setValue);
        }
        // Restore date pickers
        if (sr.getRecordDate() != null && !sr.getRecordDate().isEmpty()) {
            try { serviceDatePicker.setValue(java.time.LocalDate.parse(sr.getRecordDate())); } catch (Exception ignored) {}
        }
        if (sr.getNextServiceDate() != null && !sr.getNextServiceDate().isEmpty()) {
            try { nextServiceDatePicker.setValue(java.time.LocalDate.parse(sr.getNextServiceDate())); } catch (Exception ignored) {}
        }
    }

    private void clearFields() {
        if (vehicleCombo != null) vehicleCombo.getSelectionModel().clearSelection();
        serviceTypeCombo.getSelectionModel().clearSelection();
        serviceDatePicker.setValue(null);
        descriptionArea.clear();
        costField.clear();
        workshopField.clear();
        mileageField.clear();
        nextServiceDatePicker.setValue(null);
        selectedService = null;
    }
}