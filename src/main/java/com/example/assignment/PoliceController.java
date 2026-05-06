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

    @FXML private Label recordCountLabel;
    @FXML private Pagination pagination;
    @FXML private Button refreshBtn;
    @FXML private ImageView vehicleImageView;
    @FXML private Label vehicleImageLabel;

    @FXML private javafx.scene.control.ScrollPane formPane;
    @FXML private SplitPane mainSplitPane;
    @FXML private javafx.scene.layout.HBox actionBtnsBox;

    private ObservableList<PoliceReport> reportList = FXCollections.observableArrayList();
    private PoliceReport selectedReport;
    private static final int ROWS_PER_PAGE = 10;

    private static final String[] LESOTHO_STATIONS = {
            "Maseru Police Station","Leribe (Hlotse) Police Station","Berea Police Station",
            "Mafeteng Police Station","Mohale's Hoek Police Station","Quthing Police Station",
            "Qacha's Nek Police Station","Mokhotlong Police Station","Thaba-Tseka Police Station",
            "Butha-Buthe Police Station","Other (type below)"
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!UIUtils.checkAccess("police")) return;

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colRegNumber.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getDisplayName()));
        colType.setCellValueFactory(new PropertyValueFactory<>("reportType"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("recordDate"));
        colOfficer.setCellValueFactory(new PropertyValueFactory<>("officerName"));
        colStation.setCellValueFactory(new PropertyValueFactory<>("station"));
        colCase.setCellValueFactory(new PropertyValueFactory<>("caseNumber"));

        reportTypeCombo.getItems().addAll("Accident","Theft","Recovery","Investigation","Other");
        reportTypeCombo.valueProperty().addListener((obs,o,v)->{
            boolean isOther = "Other".equals(v);
            otherTypeField.setVisible(isOther);
            otherTypeField.setManaged(isOther);
        });

        stationCombo.getItems().addAll(LESOTHO_STATIONS);
        stationCombo.valueProperty().addListener((obs,o,v)->{
            boolean isOther = "Other (type below)".equals(v);
            stationCustomField.setVisible(isOther);
            stationCustomField.setManaged(isOther);
        });

        User user = App.getCurrentUser();
        if (user != null) officerField.setText(user.getName());

        generateNextCaseNumber();

        DropShadow shadow = new DropShadow();
        shadow.setRadius(6);
        shadow.setColor(Color.rgb(231,76,60,0.3));

        loadVehicles();
        vehicleCombo.valueProperty().addListener((obs, old, val) -> { if (val != null) loadVehicleImage(val); });
        loadReports();
        setupPagination();

        reportTable.getSelectionModel().selectedItemProperty().addListener(
                (obs,o,n)->{ if(n!=null){ selectedReport=n; populateFields(n);} });

        applyRoleUI();
    }

    private void applyRoleUI() {
        // Police and Admin: full access. No other roles reach this screen.
        // No extra hiding needed — access guard handles it.
    }

    private void generateNextCaseNumber() {
        try {
            ResultSet rs = DatabaseConnection.getInstance().executeQuery(
                    "SELECT case_number FROM police_reports ORDER BY case_number DESC LIMIT 1");

            int next = 1;
            if (rs != null && rs.next()) {
                String last = rs.getString(1);
                next = Integer.parseInt(last.replaceAll("\\D","")) + 1;
            }
            if (rs != null) rs.close();

            caseNumberField.setText(String.format("CASE%03d", next));
        } catch (Exception e) {
            caseNumberField.setText("CASE001");
        }
    }

    private void loadVehicleImage(String regOrLabel) {
        if (vehicleImageView == null || regOrLabel == null) return;
        try {
            // extract registration number — combo format: "id - REG (make model)"
            String safe = regOrLabel.replaceAll("^\\d+ - ", "").replaceAll(" \\(.*\\)$", "").replaceAll("[\\s/\\\\]", "");
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

    private String getStation() {
        if ("Other (type below)".equals(stationCombo.getValue()))
            return stationCustomField.getText().trim();
        return stationCombo.getValue();
    }

    private String getType() {
        if ("Other".equals(reportTypeCombo.getValue()))
            return otherTypeField.getText().trim();
        return reportTypeCombo.getValue();
    }

    private void loadReports() {
        reportList.clear();
        try {
            ResultSet rs = DatabaseConnection.getInstance()
                    .executeQuery("SELECT * FROM police_report_overview");

            while (rs != null && rs.next()) {
                PoliceReport pr = new PoliceReport();
pr.setId(rs.getInt("report_id"));
                pr.setVehicleId(rs.getInt("vehicle_id"));
                pr.setRecordDate(rs.getString("report_date"));
                pr.setReportType(rs.getString("report_type"));
                pr.setDescription(rs.getString("description"));
                pr.setOfficerName(rs.getString("officer_name"));
                pr.setStation(rs.getString("station"));
                pr.setCaseNumber(rs.getString("case_number"));
                reportList.add(pr);
            }
            if (rs != null) rs.close();

            setupPagination();
            recordCountLabel.setText("Total Reports: " + reportList.size());

        } catch (Exception e) {
            UIUtils.showError("Error", e.getMessage());
        }
    }

    private void loadVehicles() {
        try {
            vehicleCombo.getItems().clear();
            ResultSet rs = DatabaseConnection.getInstance().executeQuery(
                    "SELECT vehicle_id, registration_number, make, model FROM vehicles");

            while (rs != null && rs.next()) {
                vehicleCombo.getItems().add(
                        rs.getInt("vehicle_id") + " - " +
                                rs.getString("registration_number") + " (" +
                                rs.getString("make") + " " + rs.getString("model") + ")"
                );
            }
            if (rs != null) rs.close();

        } catch (Exception e) {
            UIUtils.showError("Error", e.getMessage());
        }
    }

    private void setupPagination() {
        if (pagination == null) return;

        int pages = (int)Math.ceil((double)reportList.size()/ROWS_PER_PAGE);
        pagination.setPageCount(Math.max(pages,1));

        pagination.setPageFactory(i -> {
            int from = i * ROWS_PER_PAGE;
            int to = Math.min(from + ROWS_PER_PAGE, reportList.size());
            reportTable.setItems(FXCollections.observableArrayList(reportList.subList(from, to)));
            return reportTable;
        });
    }

    @FXML
    private void handleSave(ActionEvent e) {
        try {
            String vehicle = vehicleCombo.getValue();
            if (vehicle == null) {
                UIUtils.showWarning("Validation","Select vehicle");
                return;
            }

            int vehicleId = Integer.parseInt(vehicle.split(" - ")[0]);

            ResultSet check = DatabaseConnection.getInstance().executeParameterizedQuery(
                    "SELECT report_id FROM police_reports WHERE case_number = ?",
                    caseNumberField.getText());

            if (check != null && check.next()) {
                UIUtils.showWarning("Duplicate","Case number exists");
                generateNextCaseNumber();
                return;
            }
            if (check != null) check.close();

            ResultSet saveRs = DatabaseConnection.getInstance().executeParameterizedQuery(
                    "SELECT add_police_report(?, ?, ?, ?, ?, ?, ?) AS new_id",
                    vehicleId,
                    java.sql.Date.valueOf(reportDatePicker.getValue()),
                    getType(),
                    descriptionArea.getText().trim(),
                    officerField.getText().trim(),
                    getStation(),
                    caseNumberField.getText().trim()
            );
            int newId = (saveRs != null && saveRs.next()) ? saveRs.getInt("new_id") : -1;
            if (newId <= 0) { UIUtils.showError("Error", "Failed to save report."); return; }

            UIUtils.showInfo("Success","Report saved");
            clearFields();
            loadReports();

        } catch (Exception ex) {
            UIUtils.showError("Error", ex.getMessage());
        }
    }

    @FXML
    private void handleDelete(ActionEvent e) {
        if (selectedReport == null) return;

        try {
            DatabaseConnection.getInstance().executeParameterizedUpdate(
                    "DELETE FROM police_reports WHERE report_id = ?",
                    selectedReport.getId()
            );
        } catch (java.sql.SQLException ex) {
            UIUtils.showError("Delete Error", ex.getMessage());
            return;
        }

        loadReports();
        clearFields();
    }

    @FXML
    private void handleSearch(ActionEvent e) {
        String k = searchField.getText().trim();
        if (k.isEmpty()) { loadReports(); return; }

        try {
            reportList.clear();

            ResultSet rs = DatabaseConnection.getInstance().executeParameterizedQuery(
                    "SELECT * FROM police_report_overview WHERE registration_number LIKE ? OR report_type LIKE ? OR officer_name LIKE ? OR case_number LIKE ?",
                    "%"+k+"%","%"+k+"%","%"+k+"%","%"+k+"%"
            );

            while (rs != null && rs.next()) {
                PoliceReport pr = new PoliceReport();
                pr.setId(rs.getInt("report_id"));
                pr.setRecordDate(rs.getString("report_date"));
                pr.setReportType(rs.getString("report_type"));
                pr.setOfficerName(rs.getString("officer_name"));
                pr.setStation(rs.getString("station"));
                pr.setCaseNumber(rs.getString("case_number"));
                reportList.add(pr);
            }
            if (rs != null) rs.close();

            setupPagination();
            recordCountLabel.setText("Search Results: " + reportList.size());

        } catch (Exception ex) {
            UIUtils.showError("Search Error", ex.getMessage());
        }
    }

    private void populateFields(PoliceReport pr) {
        reportTypeCombo.setValue(pr.getReportType());
        if (pr.getDescription() != null) descriptionArea.setText(pr.getDescription());
        if (pr.getOfficerName() != null) officerField.setText(pr.getOfficerName());
        if (pr.getCaseNumber() != null) caseNumberField.setText(pr.getCaseNumber());
        if (pr.getStation() != null) stationCombo.setValue(pr.getStation());
        // Restore vehicle combo by vehicleId and load image
        if (pr.getVehicleId() > 0) {
            vehicleCombo.getItems().stream()
                .filter(item -> item.startsWith(pr.getVehicleId() + " - "))
                .findFirst()
                .ifPresent(v -> { vehicleCombo.setValue(v); loadVehicleImage(v); });
        }
        // Restore date picker
        if (pr.getRecordDate() != null && !pr.getRecordDate().isEmpty()) {
            try { reportDatePicker.setValue(java.time.LocalDate.parse(pr.getRecordDate())); } catch (Exception ignored) {}
        }
    }

    private void clearFields() {
        vehicleCombo.getSelectionModel().clearSelection();
        reportTypeCombo.getSelectionModel().clearSelection();
        reportDatePicker.setValue(null);
        descriptionArea.clear();
        stationCombo.getSelectionModel().clearSelection();
        stationCustomField.clear();
        otherTypeField.clear();
        selectedReport = null;
        generateNextCaseNumber();
    }

    @FXML private void handleClear(ActionEvent e) { clearFields(); }

    @FXML private void handleRefresh(ActionEvent e) { loadReports(); }

    @FXML
    private void handleBack(ActionEvent e) {
        try {
            App.switchScene("/com/example/assignment/dashboard.fxml", "Dashboard");
        } catch (Exception ex) {
            UIUtils.showError("Error", ex.getMessage());
        }
    }
}