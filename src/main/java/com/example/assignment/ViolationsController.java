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
    @FXML private Button refreshBtn;

    @FXML private Label recordCountLabel;
    @FXML private Label totalFinesLabel;
    @FXML private Pagination pagination;
    @FXML private ImageView vehicleImageView;
    @FXML private Label vehicleImageLabel;

    @FXML private javafx.scene.control.ScrollPane formPane;
    @FXML private SplitPane mainSplitPane;
    @FXML private javafx.scene.layout.HBox actionBtnsBox;

    private final ObservableList<Violation> violationList = FXCollections.observableArrayList();
    private Violation selectedViolation;

    private static final int ROWS_PER_PAGE = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        if (!UIUtils.checkAccess("violations")) return;

        // Columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("recordDate"));
        colType.setCellValueFactory(new PropertyValueFactory<>("violationType"));
        colFine.setCellValueFactory(new PropertyValueFactory<>("fineAmount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colOfficer.setCellValueFactory(new PropertyValueFactory<>("officerName"));

        colVehicle.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("vehicleLabel"));

        setupStatusColors();
        setupUIDefaults();

        loadVehicles();
        vehicleCombo.valueProperty().addListener((obs, old, val) -> { if (val != null) loadVehicleImage(val); });
        loadViolations();

        violationTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, val) -> {
                    selectedViolation = val;
                    if (val != null) populateFields(val);
                }
        );

        applyRoleUI();
    }

    private void applyRoleUI() {
        // Police and Admin: full access. No other roles reach this screen.
    }

    private void setupStatusColors() {
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("Unpaid".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void setupUIDefaults() {
        violationTypeCombo.getItems().addAll(
                "Speeding", "Red Light", "No Seatbelt",
                "Drunk Driving", "Reckless Driving", "No License",
                "No Insurance", "Illegal Parking", "Overloading", "Other"
        );

        statusCombo.getItems().addAll("Unpaid", "Paid");

        DropShadow shadow = new DropShadow();
        shadow.setRadius(6);
        shadow.setSpread(0.2);
        shadow.setColor(Color.rgb(231, 76, 60, 0.3));
        saveBtn.setEffect(shadow);

        User currentUser = App.getCurrentUser();
        if (currentUser != null && officerField != null) {
            officerField.setText(currentUser.getName());

            if ("police".equalsIgnoreCase(currentUser.getRole())) {
                officerField.setEditable(false);
            }
        }
    }

    private void loadVehicleImage(String regOrCombo) {
        if (vehicleImageView == null || regOrCombo == null) return;
        try {
            String safe = regOrCombo.replaceAll("^\\d+ - ", "").replaceAll("[\\s/\\\\]", "");
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

    // =========================
    // LOAD VEHICLES INTO COMBO
    // =========================
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

    // =========================
    // LOAD DATA (FIXED)
    // =========================
    private void loadViolations() {
        violationList.clear();
        BigDecimal totalFines = BigDecimal.ZERO;

        try {
            DatabaseConnection db = DatabaseConnection.getInstance();

            ResultSet rs = db.executeQuery(
                    "SELECT vl.*, v.registration_number " +
                            "FROM violations vl " +
                            "JOIN vehicles v ON vl.vehicle_id = v.vehicle_id " +
                            "ORDER BY vl.violation_date DESC"
            );

            while (rs != null && rs.next()) {
                Violation v = new Violation();

                v.setId(rs.getInt("violation_id"));
                v.setVehicleId(rs.getInt("vehicle_id"));
                v.setVehicleLabel(rs.getString("registration_number"));
                v.setRecordDate(rs.getString("violation_date"));
                v.setViolationType(rs.getString("violation_type"));
                v.setFineAmount(rs.getBigDecimal("fine_amount"));
                v.setStatus(rs.getString("status"));
                v.setLocation(rs.getString("location"));
                v.setOfficerName(rs.getString("officer_name"));

                violationList.add(v);

                if (v.getFineAmount() != null) {
                    totalFines = totalFines.add(v.getFineAmount());
                }
            }

            recordCountLabel.setText("Total Records: " + violationList.size());
            totalFinesLabel.setText("Total Fines: M" + String.format("%,.2f", totalFines));

            setupPagination(); // IMPORTANT FIX

        } catch (Exception e) {
            UIUtils.showError("Error", "Failed to load violations: " + e.getMessage());
        }
    }

    // =========================
    // PAGINATION FIX
    // =========================
    private void setupPagination() {

        int pageCount = Math.max(1,
                (int) Math.ceil((double) violationList.size() / ROWS_PER_PAGE)
        );

        pagination.setPageCount(pageCount);

        pagination.setPageFactory(pageIndex -> {
            int from = pageIndex * ROWS_PER_PAGE;
            int to = Math.min(from + ROWS_PER_PAGE, violationList.size());

            if (from > to) return null;

            ObservableList<Violation> pageData =
                    FXCollections.observableArrayList(violationList.subList(from, to));

            violationTable.setItems(pageData);
            return violationTable;
        });
    }

    // =========================
    // SAVE
    // =========================
    @FXML
    private void handleSave(ActionEvent event) {
        try {
            String vehicle = vehicleCombo.getValue();
            String type = violationTypeCombo.getValue();
            String date = violationDatePicker.getValue() != null
                    ? violationDatePicker.getValue().toString()
                    : null;

            if (vehicle == null || type == null || date == null) {
                UIUtils.showWarning("Validation", "Vehicle, Type and Date required");
                return;
            }

            int vehicleId = Integer.parseInt(vehicle.split(" - ")[0]);

            BigDecimal fine = fineAmountField.getText().trim().isEmpty()
                    ? BigDecimal.ZERO
                    : new BigDecimal(fineAmountField.getText().trim());

            DatabaseConnection db = DatabaseConnection.getInstance();

            int result = db.executeParameterizedUpdate(
                    "INSERT INTO violations (vehicle_id, violation_date, violation_type, fine_amount, status, location, officer_name) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    vehicleId,
                    java.sql.Date.valueOf(date),
                    type,
                    fine,
                    statusCombo.getValue() == null ? "Unpaid" : statusCombo.getValue(),
                    locationField.getText(),
                    officerField.getText()
            );

            if (result > 0) {
                UIUtils.showInfo("Success", "Violation added");
                clearFields();
                loadViolations();
            }

        } catch (Exception e) {
            UIUtils.showError("Save Error", e.getMessage());
        }
    }

    // =========================
    // MARK PAID (FIXED CHECK)
    // =========================
    @FXML
    private void handleMarkPaid(ActionEvent event) {

        if (selectedViolation == null) {
            UIUtils.showWarning("No Selection", "Select violation first");
            return;
        }

        if ("Paid".equalsIgnoreCase(selectedViolation.getStatus())) {
            UIUtils.showInfo("Info", "Already paid");
            return;
        }

        try {
            DatabaseConnection db = DatabaseConnection.getInstance();

            int result = db.executeParameterizedUpdate(
                    "UPDATE violations SET status='Paid', paid_date=CURRENT_DATE WHERE violation_id=?",
                    selectedViolation.getId()
            );

            if (result > 0) {
                UIUtils.showInfo("Success", "Marked as paid");
                loadViolations();
            }

        } catch (Exception e) {
            UIUtils.showError("Error", e.getMessage());
        }
    }

    // =========================
    // DELETE (SAFE CALLBACK)
    // =========================
    @FXML
    private void handleDelete(ActionEvent event) {

        if (selectedViolation == null) {
            UIUtils.showWarning("No Selection", "Select violation first");
            return;
        }

        UIUtils.showConfirmation("Delete", "Delete this record?", confirmed -> {

            if (!confirmed) return;

            try {
                DatabaseConnection db = DatabaseConnection.getInstance();

                int result = db.executeParameterizedUpdate(
                        "DELETE FROM violations WHERE violation_id=?",
                        selectedViolation.getId()
                );

                if (result > 0) {
                    UIUtils.showInfo("Deleted", "Record removed");
                    loadViolations();
                    clearFields();
                }

            } catch (Exception e) {
                UIUtils.showError("Delete Error", e.getMessage());
            }
        });
    }

    // =========================
    // SEARCH (FIXED PAGINATION RESET)
    // =========================
    @FXML
    private void handleSearch(ActionEvent event) {

        String keyword = searchField.getText().trim();

        if (keyword.isEmpty()) {
            loadViolations();
            return;
        }

        try {
            violationList.clear();

            DatabaseConnection db = DatabaseConnection.getInstance();

            ResultSet rs = db.executeParameterizedQuery(
                    "SELECT vl.*, v.registration_number FROM violations vl " +
                            "JOIN vehicles v ON vl.vehicle_id = v.vehicle_id " +
                            "WHERE v.registration_number LIKE ? OR vl.violation_type LIKE ? OR vl.status LIKE ?",
                    "%" + keyword + "%",
                    "%" + keyword + "%",
                    "%" + keyword + "%"
            );

            while (rs != null && rs.next()) {
                Violation v = new Violation();
v.setId(rs.getInt("violation_id"));
                v.setVehicleId(rs.getInt("vehicle_id"));
                v.setVehicleLabel(rs.getString("registration_number"));
                v.setRecordDate(rs.getString("violation_date"));
                v.setViolationType(rs.getString("violation_type"));
                v.setFineAmount(rs.getBigDecimal("fine_amount"));
                v.setStatus(rs.getString("status"));
                v.setLocation(rs.getString("location"));
                v.setOfficerName(rs.getString("officer_name"));
                violationList.add(v);
            }

            recordCountLabel.setText("Results: " + violationList.size());

            setupPagination();

        } catch (Exception e) {
            UIUtils.showError("Search Error", e.getMessage());
        }
    }

    // =========================
    // HELPERS
    // =========================
    private void populateFields(Violation v) {
        if (v.getVehicleLabel() != null) loadVehicleImage(v.getVehicleLabel());
        violationTypeCombo.setValue(v.getViolationType());
        fineAmountField.setText(v.getFormattedFine());
        statusCombo.setValue(v.getStatus());
        locationField.setText(v.getLocation());
        officerField.setText(v.getOfficerName());
        // Restore vehicle combo
        if (v.getVehicleLabel() != null && vehicleCombo != null) {
            vehicleCombo.getItems().stream()
                .filter(item -> item.contains(v.getVehicleLabel()))
                .findFirst()
                .ifPresent(vehicleCombo::setValue);
        }
        // Restore date picker
        if (v.getRecordDate() != null && !v.getRecordDate().isEmpty()) {
            try { violationDatePicker.setValue(java.time.LocalDate.parse(v.getRecordDate())); } catch (Exception ignored) {}
        }
    }

    private void clearFields() {
        vehicleCombo.getSelectionModel().clearSelection();
        violationDatePicker.setValue(null);
        violationTypeCombo.getSelectionModel().clearSelection();
        fineAmountField.clear();
        statusCombo.getSelectionModel().clearSelection();
        locationField.clear();

        selectedViolation = null;
    }

    @FXML private void handleClear(ActionEvent e) { clearFields(); }

    @FXML private void handleRefresh(ActionEvent e) { loadViolations(); }

    @FXML
    private void handleBack(ActionEvent e) {
        try {
            App.switchScene("/com/example/assignment/dashboard.fxml", "Dashboard");
        } catch (Exception ex) {
            UIUtils.showError("Error", ex.getMessage());
        }
    }
}