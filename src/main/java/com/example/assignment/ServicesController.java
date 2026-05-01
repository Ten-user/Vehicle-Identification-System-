package com.example.assignment;

import com.example.assignment.App;
import com.example.assignment.ServiceRecord;
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
 * Service Records Controller - Workshop Module.
 * Manages vehicle service history, uses vehicle_service_summary VIEW and stored procedures.
 */
public class ServicesController implements Initializable {

    @FXML private TableView<ServiceRecord> serviceTable;
    @FXML private TableColumn<ServiceRecord, Integer> colId;
    @FXML private TableColumn<ServiceRecord, String> colVehicle;
    @FXML private TableColumn<ServiceRecord, String> colDate;
    @FXML private TableColumn<ServiceRecord, String> colType;
    @FXML private TableColumn<ServiceRecord, String> colDesc;
    @FXML private TableColumn<ServiceRecord, BigDecimal> colCost;
    @FXML private TableColumn<ServiceRecord, String> colWorkshop;

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
    @FXML private Label recordCountLabel;
    @FXML private Label totalCostLabel;
    @FXML private Pagination pagination;

    private ObservableList<ServiceRecord> serviceList = FXCollections.observableArrayList();
    private ServiceRecord selectedService;
    private static final int ROWS_PER_PAGE = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("recordDate"));
        colType.setCellValueFactory(new PropertyValueFactory<>("serviceType"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colCost.setCellValueFactory(new PropertyValueFactory<>("cost"));
        colWorkshop.setCellValueFactory(new PropertyValueFactory<>("workshopName"));

        serviceTypeCombo.getItems().addAll("Oil Change", "Minor Service", "Major Service",
            "Brake Service", "Tire Rotation", "Engine Diagnostic", "Transmission Service",
            "Aircon Regas", "Battery Replacement", "Clutch Replacement", "Wheel Alignment",
            "Turbo Replacement", "Differential Service", "Timing Belt", "First Service");

        DropShadow shadow = new DropShadow();
        shadow.setRadius(6); shadow.setSpread(0.2);
        shadow.setColor(Color.rgb(46, 204, 113, 0.3));
        saveBtn.setEffect(shadow);

        loadVehicles();
        loadServices();
        setupPagination();

        serviceTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, newVal) -> {
                if (newVal != null) { selectedService = newVal; populateFields(newVal); }
            });
    }

    private void loadServices() {
        serviceList.clear();
        BigDecimal totalCost = BigDecimal.ZERO;
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery(
                "SELECT sr.*, v.registration_number FROM service_records sr " +
                "JOIN vehicles v ON sr.vehicle_id = v.vehicle_id ORDER BY sr.service_date DESC");
            while (rs != null && rs.next()) {
                ServiceRecord sr = new ServiceRecord();
                sr.setId(rs.getInt("service_id"));
                sr.setVehicleId(rs.getInt("vehicle_id"));
                sr.setRecordDate(rs.getString("service_date"));
                sr.setServiceType(rs.getString("service_type"));
                sr.setDescription(rs.getString("description"));
                sr.setCost(rs.getBigDecimal("cost"));
                sr.setWorkshopName(rs.getString("workshop_name"));
                sr.setMileage(rs.getInt("mileage"));
                sr.setNextServiceDate(rs.getString("next_service_date"));
                serviceList.add(sr);
                totalCost = totalCost.add(rs.getBigDecimal("cost"));
            }
            serviceTable.setItems(serviceList);
            recordCountLabel.setText("Total Records: " + serviceList.size());
            totalCostLabel.setText("Total Service Cost: R" + String.format("%,.2f", totalCost));
        } catch (Exception e) {
            UIUtils.showError("Error", "Failed to load services: " + e.getMessage());
        }
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
            String vehicle = vehicleCombo.getSelectionModel().getSelectedItem();
            String sType = serviceTypeCombo.getSelectionModel().getSelectedItem();
            String date = serviceDatePicker.getValue() != null ? serviceDatePicker.getValue().toString() : "";

            if (vehicle == null || sType == null || date.isEmpty()) {
                UIUtils.showWarning("Validation", "Vehicle, Service Type, and Date are required.");
                return;
            }

            int vehicleId = Integer.parseInt(vehicle.split(" - ")[0]);
            BigDecimal cost = new BigDecimal(costField.getText().trim());

            // Call stored procedure: add_service_record
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.callProcedure("add_service_record",
                vehicleId, java.sql.Date.valueOf(date), sType,
                descriptionArea.getText().trim(), cost,
                workshopField.getText().trim(),
                UIUtils.safeParseInt(mileageField.getText(), 0),
                nextServiceDatePicker.getValue() != null ? java.sql.Date.valueOf(nextServiceDatePicker.getValue()) : null);

            if (rs != null && rs.next()) {
                UIUtils.showInfo("Success", "Service record added! ID: " + rs.getInt(1));
                clearFields(); loadServices(); setupPagination();
            }
        } catch (Exception e) {
            UIUtils.showError("Save Error", "Failed to add service record: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (selectedService == null) { UIUtils.showWarning("No Selection", "Select a record."); return; }
        if (UIUtils.showConfirmation("Delete", "Delete this service record?")) {
            try {
                DatabaseConnection db = DatabaseConnection.getInstance();
                int result = db.executeParameterizedUpdate("DELETE FROM service_records WHERE service_id = ?", selectedService.getId());
                if (result > 0) { UIUtils.showInfo("Deleted", "Service record deleted."); loadServices(); setupPagination(); clearFields(); }
            } catch (Exception e) { UIUtils.showError("Delete Error", e.getMessage()); }
        }
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) { loadServices(); return; }
        try {
            serviceList.clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeParameterizedQuery(
                "SELECT sr.*, v.registration_number FROM service_records sr JOIN vehicles v ON sr.vehicle_id = v.vehicle_id " +
                "WHERE v.registration_number LIKE ? OR sr.service_type LIKE ?",
                "%" + keyword + "%", "%" + keyword + "%");
            while (rs != null && rs.next()) {
                ServiceRecord sr = new ServiceRecord();
                sr.setId(rs.getInt("service_id")); sr.setRecordDate(rs.getString("service_date"));
                sr.setServiceType(rs.getString("service_type")); sr.setDescription(rs.getString("description"));
                sr.setCost(rs.getBigDecimal("cost")); sr.setWorkshopName(rs.getString("workshop_name"));
                serviceList.add(sr);
            }
            serviceTable.setItems(serviceList);
            recordCountLabel.setText("Search Results: " + serviceList.size());
        } catch (Exception e) { UIUtils.showError("Search Error", e.getMessage()); }
    }

    @FXML private void handleClear(ActionEvent e) { clearFields(); }
    @FXML private void handleBack(ActionEvent e) {
        try { App.switchScene("/com/example/assignment/dashboard.fxml", "Dashboard"); }
        catch (Exception ex) { UIUtils.showError("Error", ex.getMessage()); }
    }

    private void populateFields(ServiceRecord sr) {
        serviceTypeCombo.setValue(sr.getServiceType());
        descriptionArea.setText(sr.getDescription());
        costField.setText(sr.getFormattedCost());
        workshopField.setText(sr.getWorkshopName());
        mileageField.setText(String.valueOf(sr.getMileage()));
    }

    private void clearFields() {
        vehicleCombo.getSelectionModel().clearSelection();
        serviceTypeCombo.getSelectionModel().clearSelection();
        serviceDatePicker.setValue(null); descriptionArea.clear();
        costField.clear(); workshopField.clear(); mileageField.clear();
        nextServiceDatePicker.setValue(null); selectedService = null;
    }
}
