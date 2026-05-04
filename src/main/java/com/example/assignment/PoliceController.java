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
import java.sql.ResultSet;
import java.util.ResourceBundle;

/**
 * Police Controller - FIXED for fullscreen stability.
 * CHANGE: showConfirmation() uses callback pattern.
 */
public class PoliceController implements Initializable {

    @FXML private TableView<PoliceReport> reportTable;
    @FXML private TableColumn<PoliceReport, Integer> colId;
    @FXML private TableColumn<PoliceReport, String> colRegNumber;
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
    @FXML private ComboBox<String> stationCombo;
    @FXML private TextField stationCustomField;
    @FXML private TextField caseNumberField;
    @FXML private TextField otherTypeField;
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

    private static final String[] LESOTHO_STATIONS = {
        "Maseru Police Station",
        "Leribe (Hlotse) Police Station",
        "Berea Police Station",
        "Mafeteng Police Station",
        "Mohale's Hoek Police Station",
        "Quthing Police Station",
        "Qacha's Nek Police Station",
        "Mokhotlong Police Station",
        "Thaba-Tseka Police Station",
        "Butha-Buthe Police Station",
        "Other (type below)"
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ACCESS GUARD: Only Admin and Police can access Police Reports
        if (!UIUtils.checkAccess("police")) return;

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
        reportTypeCombo.valueProperty().addListener((obs, old, val) -> {
            boolean isOther = "Other".equals(val);
            if (otherTypeField != null) {
                otherTypeField.setVisible(isOther);
                otherTypeField.setManaged(isOther);
            }
        });

        if (stationCombo != null) {
            stationCombo.getItems().addAll(LESOTHO_STATIONS);
            stationCombo.valueProperty().addListener((obs, old, val) -> {
                boolean isOther = "Other (type below)".equals(val);
                if (stationCustomField != null) {
                    stationCustomField.setVisible(isOther);
                    stationCustomField.setManaged(isOther);
                }
            });
        }

        User currentUser = App.getCurrentUser();
        if (currentUser != null && officerField != null) {
            officerField.setText(currentUser.getName());
            if ("police".equalsIgnoreCase(currentUser.getRole())) {
                officerField.setEditable(false);
                officerField.setStyle("-fx-background-color: #F0F4F8; -fx-text-fill: #445566;");
            }
        }

        generateNextCaseNumber();

        DropShadow shadow = new DropShadow();
        shadow.setRadius(6); shadow.setSpread(0.2);
        shadow.setColor(Color.rgb(231, 76, 60, 0.3));
        if (saveBtn != null) saveBtn.setEffect(shadow);

        loadVehicles();
        loadReports();
        setupPagination();

        reportTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, newVal) -> {
                if (newVal != null) { selectedReport = newVal; populateFields(newVal); }
            });
    }

    private void generateNextCaseNumber() {
        if (caseNumberField == null) return;
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery(
                "SELECT case_number FROM police_reports WHERE case_number LIKE 'CASE%' ORDER BY case_number DESC LIMIT 1");
            int nextNum = 1;
            if (rs != null && rs.next()) {
                String last = rs.getString("case_number");
                try {
                    nextNum = Integer.parseInt(last.replaceAll("[^0-9]", "")) + 1;
                } catch (NumberFormatException ignored) {}
            }
            caseNumberField.setText(String.format("CASE%03d", nextNum));
        } catch (Exception e) {
            caseNumberField.setText("CASE001");
        }
    }

    private String getEffectiveStation() {
        if (stationCombo == null) return "";
        String val = stationCombo.getValue();
        if ("Other (type below)".equals(val)) {
            return stationCustomField != null ? stationCustomField.getText().trim() : "";
        }
        return val != null ? val : "";
    }

    private String getEffectiveReportType() {
        String val = reportTypeCombo.getValue();
        if ("Other".equals(val)) {
            String custom = otherTypeField != null ? otherTypeField.getText().trim() : "";
            return custom.isEmpty() ? "Other" : custom;
        }
        return val != null ? val : "";
    }

    private void loadReports() {
        reportList.clear();
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
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
            if (recordCountLabel != null)
                recordCountLabel.setText("Total Reports: " + reportList.size());
        } catch (Exception e) {
            UIUtils.showError("Error", "Failed to load police reports: " + e.getMessage());
        }
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
        } catch (Exception e) {
            UIUtils.showError("Error", "Could not load vehicles: " + e.getMessage());
        }
    }

    private void setupPagination() {
        int pageCount = Math.max(1, (reportList.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        if (pagination == null) return;
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
            String type = getEffectiveReportType();
            String date = reportDatePicker.getValue() != null ? reportDatePicker.getValue().toString() : "";
            String station = getEffectiveStation();
            String caseNum = caseNumberField.getText().trim();

            if (vehicle == null || type.isEmpty() || date.isEmpty()) {
                UIUtils.showWarning("Validation", "Vehicle, Report Type, and Date are required.");
                return;
            }
            if (station.isEmpty()) {
                UIUtils.showWarning("Validation", "Police station is required.");
                return;
            }
            if (caseNum.isEmpty()) {
                UIUtils.showWarning("Validation", "Case number is required.");
                return;
            }

            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet check = db.executeParameterizedQuery(
                "SELECT report_id FROM police_reports WHERE case_number = ?", caseNum);
            if (check != null && check.next()) {
                UIUtils.showWarning("Duplicate Case Number",
                    "Case number '" + caseNum + "' already exists. A new number has been generated.");
                generateNextCaseNumber();
                return;
            }

            int vehicleId = Integer.parseInt(vehicle.split(" - ")[0]);
            String officerName = officerField.getText().trim();

            int result = db.executeParameterizedUpdate(
                "INSERT INTO police_reports (vehicle_id, report_date, report_type, description, officer_name, station, case_number) VALUES (?, ?, ?, ?, ?, ?, ?)",
                vehicleId, java.sql.Date.valueOf(date), type,
                descriptionArea.getText().trim(),
                officerName, station, caseNum);

            if (result > 0) {
                UIUtils.showInfo("Success", "Police report added! Case: " + caseNum);
                clearFields();
                loadReports();
                setupPagination();
            }
        } catch (Exception e) {
            UIUtils.showError("Save Error", "Failed to add report: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (selectedReport == null) { UIUtils.showWarning("No Selection", "Select a report."); return; }
        // FIX: callback-based confirmation
        UIUtils.showConfirmation("Delete", "Delete this police report?", confirmed -> {
            if (confirmed) {
                try {
                    DatabaseConnection db = DatabaseConnection.getInstance();
                    int result = db.executeParameterizedUpdate(
                        "DELETE FROM police_reports WHERE report_id = ?", selectedReport.getId());
                    if (result > 0) {
                        UIUtils.showInfo("Deleted", "Report deleted.");
                        loadReports(); setupPagination(); clearFields();
                    }
                } catch (Exception e) { UIUtils.showError("Delete Error", e.getMessage()); }
            }
        });
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) { loadReports(); return; }
        try {
            reportList.clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeParameterizedQuery(
                "SELECT * FROM police_report_overview WHERE registration_number LIKE ? OR report_type LIKE ? OR officer_name LIKE ? OR case_number LIKE ?",
                "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%");
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
            if (recordCountLabel != null)
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
        User currentUser = App.getCurrentUser();
        if (currentUser == null || !"police".equalsIgnoreCase(currentUser.getRole())) {
            officerField.setText(pr.getOfficerName());
        }
        if (stationCombo != null) {
            String station = pr.getStation();
            boolean found = false;
            for (String s : LESOTHO_STATIONS) {
                if (s.equals(station)) { stationCombo.setValue(s); found = true; break; }
            }
            if (!found && station != null && !station.isEmpty()) {
                stationCombo.setValue("Other (type below)");
                if (stationCustomField != null) stationCustomField.setText(station);
            }
        }
        caseNumberField.setText(pr.getCaseNumber());
    }

    private void clearFields() {
        vehicleCombo.getSelectionModel().clearSelection();
        reportTypeCombo.getSelectionModel().clearSelection();
        reportDatePicker.setValue(null);
        descriptionArea.clear();

        User currentUser = App.getCurrentUser();
        if (currentUser != null) {
            officerField.setText(currentUser.getName());
        } else {
            officerField.clear();
        }

        if (stationCombo != null) stationCombo.getSelectionModel().clearSelection();
        if (stationCustomField != null) { stationCustomField.clear(); stationCustomField.setVisible(false); stationCustomField.setManaged(false); }
        if (otherTypeField != null) { otherTypeField.clear(); otherTypeField.setVisible(false); otherTypeField.setManaged(false); }

        selectedReport = null;
        generateNextCaseNumber();
    }
}
