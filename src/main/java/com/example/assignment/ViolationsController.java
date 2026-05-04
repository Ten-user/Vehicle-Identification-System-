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
import java.sql.ResultSet;
import java.util.ResourceBundle;

/**
 * Violations Controller - FIXED for fullscreen stability.
 * CHANGE: showConfirmation() uses callback pattern.
 */
public class ViolationsController implements Initializable {

    @FXML private TableView<Violation> violationTable;
    @FXML private TableColumn<Violation, Integer> colId;
    @FXML private TableColumn<Violation, String> colVehicle;
    @FXML private TableColumn<Violation, String> colDate;
    @FXML private TableColumn<Violation, String> colType;
    @FXML private TableColumn<Violation, BigDecimal> colFine;
    @FXML private TableColumn<Violation, String> colStatus;
    @FXML private TableColumn<Violation, String> colLocation;
    @FXML private TableColumn<Violation, String> colOfficer;

    @FXML private ComboBox<String> vehicleCombo;
    @FXML private DatePicker violationDatePicker;
    @FXML private ComboBox<String> violationTypeCombo;
    @FXML private TextField fineAmountField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextField locationField;
    @FXML private TextField officerField;
    @FXML private TextField searchField;

    @FXML private Button saveBtn;
    @FXML private Button markPaidBtn;
    @FXML private Button deleteBtn;
    @FXML private Button clearBtn;
    @FXML private Button searchBtn;
    @FXML private Button backBtn;
    @FXML private Label recordCountLabel;
    @FXML private Label totalFinesLabel;
    @FXML private Pagination pagination;

    private ObservableList<Violation> violationList = FXCollections.observableArrayList();
    private Violation selectedViolation;
    private static final int ROWS_PER_PAGE = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ACCESS GUARD: Only Admin and Police can access Violations
        if (!UIUtils.checkAccess("violations")) return;

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("recordDate"));
        colType.setCellValueFactory(new PropertyValueFactory<>("violationType"));
        colFine.setCellValueFactory(new PropertyValueFactory<>("fineAmount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colOfficer.setCellValueFactory(new PropertyValueFactory<>("officerName"));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    if ("Unpaid".equalsIgnoreCase(item))
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    else
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                }
            }
        });

        User currentUser = App.getCurrentUser();
        if (currentUser != null && officerField != null) {
            officerField.setText(currentUser.getName());
            if ("police".equalsIgnoreCase(currentUser.getRole())) {
                officerField.setEditable(false);
                officerField.setStyle("-fx-background-color: #F0F4F8; -fx-text-fill: #445566;");
            }
        }

        violationTypeCombo.getItems().addAll("Speeding", "Red Light", "No Seatbelt",
                "Drunk Driving", "Reckless Driving", "No License", "No Insurance",
                "Illegal Parking", "Overloading", "Other");
        statusCombo.getItems().addAll("Unpaid", "Paid");

        DropShadow shadow = new DropShadow();
        shadow.setRadius(6); shadow.setSpread(0.2);
        shadow.setColor(Color.rgb(231, 76, 60, 0.3));
        saveBtn.setEffect(shadow);

        loadVehicles();
        loadViolations();
        setupPagination();

        violationTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, newVal) -> {
                if (newVal != null) { selectedViolation = newVal; populateFields(newVal); }
            });
    }

    private void loadViolations() {
        violationList.clear();
        BigDecimal totalFines = BigDecimal.ZERO;
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery(
                "SELECT vl.*, v.registration_number FROM violations vl " +
                "JOIN vehicles v ON vl.vehicle_id = v.vehicle_id ORDER BY vl.violation_date DESC");
            while (rs != null && rs.next()) {
                Violation vl = new Violation();
                vl.setId(rs.getInt("violation_id"));
                vl.setVehicleId(rs.getInt("vehicle_id"));
                vl.setRecordDate(rs.getString("violation_date"));
                vl.setViolationType(rs.getString("violation_type"));
                vl.setFineAmount(rs.getBigDecimal("fine_amount"));
                vl.setStatus(rs.getString("status"));
                vl.setLocation(rs.getString("location"));
                vl.setOfficerName(rs.getString("officer_name"));
                vl.setPaidDate(rs.getString("paid_date"));
                violationList.add(vl);
                if (rs.getBigDecimal("fine_amount") != null)
                    totalFines = totalFines.add(rs.getBigDecimal("fine_amount"));
            }
            violationTable.setItems(violationList);
            recordCountLabel.setText("Total Records: " + violationList.size());
            totalFinesLabel.setText("Total Fines: M" + String.format("%,.2f", totalFines));
        } catch (Exception e) {
            UIUtils.showError("Error", "Failed to load violations: " + e.getMessage());
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
        int pageCount = Math.max(1, (violationList.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);
        pagination.setPageFactory(pageIndex -> {
            int from = pageIndex * ROWS_PER_PAGE;
            int to = Math.min(from + ROWS_PER_PAGE, violationList.size());
            violationTable.setItems(FXCollections.observableArrayList(violationList.subList(from, to)));
            return violationTable;
        });
    }

    @FXML
    private void handleSave(ActionEvent event) {
        try {
            String vehicle = vehicleCombo.getSelectionModel().getSelectedItem();
            String type = violationTypeCombo.getSelectionModel().getSelectedItem();
            String date = violationDatePicker.getValue() != null ? violationDatePicker.getValue().toString() : "";

            if (vehicle == null || type == null || date.isEmpty()) {
                UIUtils.showWarning("Validation", "Vehicle, Type, and Date are required.");
                return;
            }

            int vehicleId = Integer.parseInt(vehicle.split(" - ")[0]);
            BigDecimal fine = new BigDecimal(fineAmountField.getText().trim().isEmpty() ? "0" : fineAmountField.getText().trim());
            String status = statusCombo.getValue() != null ? statusCombo.getValue() : "Unpaid";

            DatabaseConnection db = DatabaseConnection.getInstance();
            int result = db.executeParameterizedUpdate(
                "INSERT INTO violations (vehicle_id, violation_date, violation_type, fine_amount, status, location, officer_name) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                vehicleId, java.sql.Date.valueOf(date), type, fine, status,
                locationField.getText().trim(), officerField.getText().trim());

            if (result > 0) {
                UIUtils.showInfo("Success", "Violation recorded!");
                clearFields(); loadViolations(); setupPagination();
            }
        } catch (Exception e) {
            UIUtils.showError("Save Error", e.getMessage());
        }
    }

    @FXML
    private void handleMarkPaid(ActionEvent event) {
        if (selectedViolation == null) { UIUtils.showWarning("No Selection", "Select a violation."); return; }
        if (selectedViolation.isPaid()) { UIUtils.showInfo("Info", "This violation is already paid."); return; }
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            int result = db.executeParameterizedUpdate(
                "UPDATE violations SET status = 'Paid', paid_date = CURDATE() WHERE violation_id = ?",
                selectedViolation.getId());
            if (result > 0) {
                UIUtils.showInfo("Success", "Violation marked as paid!");
                loadViolations(); clearFields();
            }
        } catch (Exception e) { UIUtils.showError("Error", e.getMessage()); }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (selectedViolation == null) { UIUtils.showWarning("No Selection", "Select a violation."); return; }
        // FIX: callback-based confirmation
        UIUtils.showConfirmation("Delete", "Delete this violation record?", confirmed -> {
            if (confirmed) {
                try {
                    DatabaseConnection db = DatabaseConnection.getInstance();
                    int result = db.executeParameterizedUpdate("DELETE FROM violations WHERE violation_id = ?", selectedViolation.getId());
                    if (result > 0) { UIUtils.showInfo("Deleted", "Violation deleted."); loadViolations(); setupPagination(); clearFields(); }
                } catch (Exception e) { UIUtils.showError("Delete Error", e.getMessage()); }
            }
        });
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) { loadViolations(); return; }
        try {
            violationList.clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeParameterizedQuery(
                "SELECT vl.*, v.registration_number FROM violations vl JOIN vehicles v ON vl.vehicle_id = v.vehicle_id " +
                "WHERE v.registration_number LIKE ? OR vl.violation_type LIKE ? OR vl.status LIKE ?",
                "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%");
            while (rs != null && rs.next()) {
                Violation vl = new Violation();
                vl.setId(rs.getInt("violation_id"));
                vl.setVehicleId(rs.getInt("vehicle_id"));
                vl.setRecordDate(rs.getString("violation_date"));
                vl.setViolationType(rs.getString("violation_type"));
                vl.setFineAmount(rs.getBigDecimal("fine_amount"));
                vl.setStatus(rs.getString("status"));
                vl.setLocation(rs.getString("location"));
                vl.setOfficerName(rs.getString("officer_name"));
                violationList.add(vl);
            }
            violationTable.setItems(violationList);
            recordCountLabel.setText("Search Results: " + violationList.size());
        } catch (Exception e) { UIUtils.showError("Search Error", e.getMessage()); }
    }

    @FXML private void handleClear(ActionEvent e) { clearFields(); }
    @FXML private void handleBack(ActionEvent e) {
        try { App.switchScene("/com/example/assignment/dashboard.fxml", "Dashboard"); }
        catch (Exception ex) { UIUtils.showError("Error", ex.getMessage()); }
    }

    private void populateFields(Violation vl) {
        violationTypeCombo.setValue(vl.getViolationType());
        fineAmountField.setText(vl.getFormattedFine());
        statusCombo.setValue(vl.getStatus());
        locationField.setText(vl.getLocation());
        officerField.setText(vl.getOfficerName());
    }

    private void clearFields() {
        vehicleCombo.getSelectionModel().clearSelection();
        violationDatePicker.setValue(null);
        violationTypeCombo.getSelectionModel().clearSelection();
        fineAmountField.clear(); statusCombo.getSelectionModel().clearSelection();
        locationField.clear(); officerField.clear();
        selectedViolation = null;
    }
}
