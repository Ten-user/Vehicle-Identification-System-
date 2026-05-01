package com.example.assignment;

import com.example.assignment.App;
import com.example.assignment.PoliceReport;
import com.example.assignment.Vehicle;
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
 * Police Controller - manages police reports.
 * Police Module: helps police check all valid information on the vehicle.
 * Uses the police_report_overview VIEW and stored procedures.
 */
public class PoliceController implements Initializable {

    @FXML private TableView<PoliceReport> reportTable;
    @FXML private TableColumn<PoliceReport, Integer> colId;
    @FXML private TableColumn<PoliceReport, String> colRegNumber;
    @FXML private TableColumn<PoliceReport, String> colVehicle;
    @FXML private TableColumn<PoliceReport, String> colType;
    @FXML private TableColumn<PoliceReport, String> colDate;
    @FXML private TableColumn<PoliceReport, String> colOfficer;
    @FXML private TableColumn<PoliceReport, String> colStation;
    @FXML private TableColumn<PoliceReport, String> colCase;

    @FXML private ComboBox<String> vehicleCombo;
    @FXML private ComboBox<String> reportTypeCombo;
    @FXML private DatePicker reportDatePicker;
    @FXML private TextArea descriptionArea;
    @FXML private TextField officerField;
    @FXML private TextField stationField;
    @FXML private TextField caseNumberField;
    @FXML private TextField searchField;

    @FXML private Button saveBtn;
    @FXML private Button deleteBtn;
    @FXML private Button clearBtn;
    @FXML private Button searchBtn;
    @FXML private Button backBtn;
    @FXML private Label recordCountLabel;
    @FXML private Pagination pagination;

    private ObservableList<PoliceReport> reportList = FXCollections.observableArrayList();
    private PoliceReport selectedReport;
    private static final int ROWS_PER_PAGE = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colRegNumber.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getDisplayName()));
        colType.setCellValueFactory(new PropertyValueFactory<>("reportType"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("recordDate"));
        colOfficer.setCellValueFactory(new PropertyValueFactory<>("officerName"));
        colStation.setCellValueFactory(new PropertyValueFactory<>("station"));
        colCase.setCellValueFactory(new PropertyValueFactory<>("caseNumber"));

        reportTypeCombo.getItems().addAll("Accident", "Theft", "Recovery", "Investigation", "Other");

        DropShadow shadow = new DropShadow();
        shadow.setRadius(6); shadow.setSpread(0.2);
        shadow.setColor(Color.rgb(231, 76, 60, 0.3));
        saveBtn.setEffect(shadow);

        loadVehicles();
        loadReports();
        setupPagination();

        reportTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, newVal) -> {
                if (newVal != null) { selectedReport = newVal; populateFields(newVal); }
            });
    }

    private void loadReports() {
        reportList.clear();
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            // Using VIEW for data (assignment requirement)
            ResultSet rs = db.executeQuery("SELECT * FROM police_report_overview");
            while (rs != null && rs.next()) {
                PoliceReport pr = new PoliceReport();
                pr.setId(rs.getInt("report_id"));
                pr.setRecordDate(rs.getString("report_date"));
                pr.setReportType(rs.getString("report_type"));
                pr.setDescription(rs.getString("description"));
                pr.setOfficerName(rs.getString("officer_name"));
                pr.setStation(rs.getString("station"));
                pr.setCaseNumber(rs.getString("case_number"));
                reportList.add(pr);
            }
            reportTable.setItems(reportList);
            recordCountLabel.setText("Total Reports: " + reportList.size());
        } catch (Exception e) {
            UIUtils.showError("Error", "Failed to load police reports: " + e.getMessage());
        }
    }

    private void loadVehicles() {
        try {
            vehicleCombo.getItems().clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery("SELECT vehicle_id, registration_number, make, model FROM vehicles ORDER BY registration_number");
            while (rs != null && rs.next()) {
                vehicleCombo.getItems().add(rs.getInt("vehicle_id") + " - " +
                    rs.getString("registration_number") + " (" +
                    rs.getString("make") + " " + rs.getString("model") + ")");
            }
        } catch (Exception e) {
            UIUtils.showError("Error", "Could not load vehicles: " + e.getMessage());
        }
    }

    private void setupPagination() {
        int pageCount = Math.max(1, (reportList.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);
        pagination.setPageFactory(pageIndex -> {
            int from = pageIndex * ROWS_PER_PAGE;
            int to = Math.min(from + ROWS_PER_PAGE, reportList.size());
            reportTable.setItems(FXCollections.observableArrayList(reportList.subList(from, to)));
            return reportTable;
        });
    }

    @FXML
    private void handleSave(ActionEvent event) {
        try {
            String vehicle = vehicleCombo.getSelectionModel().getSelectedItem();
            String type = reportTypeCombo.getSelectionModel().getSelectedItem();
            String date = reportDatePicker.getValue() != null ? reportDatePicker.getValue().toString() : "";

            if (vehicle == null || type == null || date.isEmpty()) {
                UIUtils.showWarning("Validation", "Vehicle, Report Type, and Date are required.");
                return;
            }

            int vehicleId = Integer.parseInt(vehicle.split(" - ")[0]);

            // Call stored procedure: add_police_report (assignment requirement)
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.callProcedure("add_police_report",
                vehicleId, java.sql.Date.valueOf(date), type,
                descriptionArea.getText().trim(),
                officerField.getText().trim(),
                stationField.getText().trim(),
                caseNumberField.getText().trim());

            if (rs != null && rs.next()) {
                UIUtils.showInfo("Success", "Police report added! ID: " + rs.getInt(1));
                clearFields(); loadReports(); setupPagination();
            }
        } catch (Exception e) {
            UIUtils.showError("Save Error", "Failed to add report: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (selectedReport == null) { UIUtils.showWarning("No Selection", "Select a report."); return; }
        if (UIUtils.showConfirmation("Delete", "Delete this police report?")) {
            try {
                DatabaseConnection db = DatabaseConnection.getInstance();
                int result = db.executeParameterizedUpdate("DELETE FROM police_reports WHERE report_id = ?", selectedReport.getId());
                if (result > 0) {
                    UIUtils.showInfo("Deleted", "Report deleted.");
                    loadReports(); setupPagination(); clearFields();
                }
            } catch (Exception e) { UIUtils.showError("Delete Error", e.getMessage()); }
        }
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) { loadReports(); return; }
        try {
            reportList.clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeParameterizedQuery(
                "SELECT * FROM police_report_overview WHERE registration_number LIKE ? OR report_type LIKE ? OR officer_name LIKE ?",
                "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%");
            while (rs != null && rs.next()) {
                PoliceReport pr = new PoliceReport();
                pr.setId(rs.getInt("report_id"));
                pr.setRecordDate(rs.getString("report_date"));
                pr.setReportType(rs.getString("report_type"));
                pr.setDescription(rs.getString("description"));
                pr.setOfficerName(rs.getString("officer_name"));
                pr.setStation(rs.getString("station"));
                pr.setCaseNumber(rs.getString("case_number"));
                reportList.add(pr);
            }
            reportTable.setItems(reportList);
            recordCountLabel.setText("Search Results: " + reportList.size());
        } catch (Exception e) { UIUtils.showError("Search Error", e.getMessage()); }
    }

    @FXML private void handleClear(ActionEvent e) { clearFields(); }
    @FXML private void handleBack(ActionEvent e) {
        try { App.switchScene("/com/example/assignment/dashboard.fxml", "Dashboard"); }
        catch (Exception ex) { UIUtils.showError("Error", ex.getMessage()); }
    }

    private void populateFields(PoliceReport pr) {
        reportTypeCombo.setValue(pr.getReportType());
        descriptionArea.setText(pr.getDescription());
        officerField.setText(pr.getOfficerName());
        stationField.setText(pr.getStation());
        caseNumberField.setText(pr.getCaseNumber());
    }

    private void clearFields() {
        vehicleCombo.getSelectionModel().clearSelection();
        reportTypeCombo.getSelectionModel().clearSelection();
        reportDatePicker.setValue(null);
        descriptionArea.clear(); officerField.clear();
        stationField.clear(); caseNumberField.clear();
        selectedReport = null;
    }
}
