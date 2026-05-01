package com.example.assignment;

import com.example.assignment.App;
import com.example.assignment.Vehicle;
import com.example.assignment.DataMapper;
import com.example.assignment.DatabaseConnection;
import com.example.assignment.UIUtils;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.ResultSet;
import java.util.ResourceBundle;

/**
 * Vehicles Controller - manages vehicle CRUD operations.
 * Workshop Module: stores vehicle registration details.
 * Demonstrates: TableView, Pagination, Exception Handling, JDBC.
 */
public class VehiclesController implements Initializable {

    @FXML private TableView<Vehicle> vehicleTable;
    @FXML private TableColumn<Vehicle, Integer> colId;
    @FXML private TableColumn<Vehicle, String> colRegNumber;
    @FXML private TableColumn<Vehicle, String> colMake;
    @FXML private TableColumn<Vehicle, String> colModel;
    @FXML private TableColumn<Vehicle, Integer> colYear;
    @FXML private TableColumn<Vehicle, String> colColor;
    @FXML private TableColumn<Vehicle, String> colOwner;
    @FXML private TableColumn<Vehicle, String> colStolen;

    @FXML private TextField searchField;
    @FXML private TextField regNumberField;
    @FXML private TextField makeField;
    @FXML private TextField modelField;
    @FXML private TextField yearField;
    @FXML private TextField colorField;
    @FXML private ComboBox<String> ownerCombo;
    @FXML private TextField engineNumberField;
    @FXML private TextField chassisNumberField;

    @FXML private Button saveBtn;
    @FXML private Button updateBtn;
    @FXML private Button deleteBtn;
    @FXML private Button clearBtn;
    @FXML private Button searchBtn;
    @FXML private Button backBtn;

    @FXML private Pagination pagination;
    @FXML private ScrollPane scrollPane;
    @FXML private Label recordCountLabel;
    @FXML private Label statusLabel;

    private ObservableList<Vehicle> vehicleList = FXCollections.observableArrayList();
    private Vehicle selectedVehicle;

    private static final int ROWS_PER_PAGE = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup TableView columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colRegNumber.setCellValueFactory(new PropertyValueFactory<>("registrationNumber"));
        colMake.setCellValueFactory(new PropertyValueFactory<>("make"));
        colModel.setCellValueFactory(new PropertyValueFactory<>("model"));
        colYear.setCellValueFactory(new PropertyValueFactory<>("year"));
        colColor.setCellValueFactory(new PropertyValueFactory<>("color"));
        colOwner.setCellValueFactory(new PropertyValueFactory<>("ownerName"));
        colStolen.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().isStolen() ? "STOLEN" : "Clear"));

        // Style stolen vehicles
        colStolen.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else if ("STOLEN".equals(item)) {
                    setText("STOLEN");
                    setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                } else {
                    setText("Clear");
                    setStyle("-fx-text-fill: #27ae60;");
                }
            }
        });

        // Apply DropShadow to buttons
        applyButtonEffects();

        // Load data
        loadOwners();
        loadVehicles();

        // Setup pagination
        setupPagination();

        // Table row selection
        vehicleTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    selectedVehicle = newSelection;
                    populateFields(newSelection);
                }
            }
        );
    }

    /**
     * Applies DropShadow effect to action buttons (Visual Effects requirement)
     */
    private void applyButtonEffects() {
        DropShadow shadow = new DropShadow();
        shadow.setRadius(6);
        shadow.setSpread(0.2);
        shadow.setColor(javafx.scene.paint.Color.rgb(0, 120, 215, 0.3));

        if (saveBtn != null) saveBtn.setEffect(shadow);
        if (searchBtn != null) searchBtn.setEffect(shadow);
        if (backBtn != null) backBtn.setEffect(shadow);
    }

    /**
     * Loads vehicles from database using the vehicle_owner_details VIEW
     */
    private void loadVehicles() {
        vehicleList.clear();
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            // Using VIEW for data retrieval (assignment requirement: use views)
            ResultSet rs = db.executeQuery("SELECT * FROM vehicle_owner_details ORDER BY vehicle_id");
            while (rs != null && rs.next()) {
                Vehicle v = new Vehicle();
                v.setId(rs.getInt("vehicle_id"));
                v.setRegistrationNumber(rs.getString("registration_number"));
                v.setMake(rs.getString("make"));
                v.setModel(rs.getString("model"));
                v.setYear(rs.getInt("year"));
                v.setColor(rs.getString("color"));
                v.setStolen(rs.getBoolean("is_stolen"));
                v.setOwnerName(rs.getString("owner_name"));
                vehicleList.add(v);
            }
            vehicleTable.setItems(vehicleList);
            recordCountLabel.setText("Total Records: " + vehicleList.size());
            statusLabel.setText("Loaded from vehicle_owner_details VIEW");
        } catch (Exception e) {
            UIUtils.showError("Database Error", "Failed to load vehicles: " + e.getMessage());
        }
    }

    /**
     * Loads customer names into owner combo box
     */
    private void loadOwners() {
        try {
            ownerCombo.getItems().clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery("SELECT customer_id, name FROM customers ORDER BY name");
            while (rs != null && rs.next()) {
                ownerCombo.getItems().add(rs.getInt("customer_id") + " - " + rs.getString("name"));
            }
        } catch (Exception e) {
            statusLabel.setText("Could not load owners: " + e.getMessage());
        }
    }

    /**
     * Sets up pagination for the vehicle table (Pagination requirement)
     */
    private void setupPagination() {
        int pageCount = (vehicleList.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
        if (pageCount < 1) pageCount = 1;
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);
        pagination.setPageFactory(pageIndex -> {
            int fromIndex = pageIndex * ROWS_PER_PAGE;
            int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, vehicleList.size());
            vehicleTable.setItems(FXCollections.observableArrayList(
                    vehicleList.subList(fromIndex, toIndex)));
            return vehicleTable;
        });
    }

    /**
     * Saves a new vehicle using the stored procedure (PROCEDURE requirement)
     */
    @FXML
    private void handleSave(ActionEvent event) {
        try {
            // Validate inputs - EXCEPTION HANDLING
            String regNum = regNumberField.getText().trim();
            String make = makeField.getText().trim();
            String model = modelField.getText().trim();
            String yearStr = yearField.getText().trim();

            if (regNum.isEmpty() || make.isEmpty() || model.isEmpty()) {
                UIUtils.showWarning("Validation Error", "Registration number, Make, and Model are required.");
                return;
            }

            if (!UIUtils.isValidInteger(yearStr)) {
                UIUtils.showWarning("Validation Error", "Please enter a valid year.");
                return;
            }

            int year = Integer.parseInt(yearStr);
            int ownerId = getSelectedOwnerId();

            // Call stored procedure: add_vehicle (assignment requirement)
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.callProcedure("add_vehicle", regNum, make, model, year,
                    ownerId, colorField.getText().trim(),
                    engineNumberField.getText().trim(),
                    chassisNumberField.getText().trim());

            if (rs != null && rs.next()) {
                int vehicleId = rs.getInt(1);
                UIUtils.showInfo("Success", "Vehicle added successfully! ID: " + vehicleId);
                clearFields();
                loadVehicles();
                setupPagination();
            }
        } catch (Exception e) {
            UIUtils.showError("Save Error", "Failed to add vehicle: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdate(ActionEvent event) {
        if (selectedVehicle == null) {
            UIUtils.showWarning("No Selection", "Please select a vehicle to update.");
            return;
        }
        try {
            String sql = "UPDATE vehicles SET registration_number=?, make=?, model=?, year=?, " +
                        "color=?, engine_number=?, chassis_number=? WHERE vehicle_id=?";
            DatabaseConnection db = DatabaseConnection.getInstance();
            int result = db.executeParameterizedUpdate(sql,
                    regNumberField.getText().trim(),
                    makeField.getText().trim(),
                    modelField.getText().trim(),
                    UIUtils.safeParseInt(yearField.getText(), 2000),
                    colorField.getText().trim(),
                    engineNumberField.getText().trim(),
                    chassisNumberField.getText().trim(),
                    selectedVehicle.getId());

            if (result > 0) {
                UIUtils.showInfo("Success", "Vehicle updated successfully!");
                loadVehicles();
                clearFields();
            }
        } catch (Exception e) {
            UIUtils.showError("Update Error", "Failed to update vehicle: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (selectedVehicle == null) {
            UIUtils.showWarning("No Selection", "Please select a vehicle to delete.");
            return;
        }
        if (UIUtils.showConfirmation("Confirm Delete",
                "Delete vehicle " + selectedVehicle.getDisplayName() + "?")) {
            try {
                String sql = "DELETE FROM vehicles WHERE vehicle_id = ?";
                DatabaseConnection db = DatabaseConnection.getInstance();
                int result = db.executeParameterizedUpdate(sql, selectedVehicle.getId());
                if (result > 0) {
                    UIUtils.showInfo("Deleted", "Vehicle deleted successfully.");
                    loadVehicles();
                    setupPagination();
                    clearFields();
                }
            } catch (Exception e) {
                UIUtils.showError("Delete Error", "Failed to delete vehicle: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadVehicles();
            return;
        }
        // Use the stored procedure for search
        try {
            vehicleList.clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.callProcedure("get_vehicle_history", keyword);
            // Also search by reg number directly
            ResultSet rs2 = db.executeParameterizedQuery(
                "SELECT * FROM vehicle_owner_details WHERE registration_number LIKE ? OR make LIKE ? OR model LIKE ?",
                "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%");
            while (rs2 != null && rs2.next()) {
                Vehicle v = new Vehicle();
                v.setId(rs2.getInt("vehicle_id"));
                v.setRegistrationNumber(rs2.getString("registration_number"));
                v.setMake(rs2.getString("make"));
                v.setModel(rs2.getString("model"));
                v.setYear(rs2.getInt("year"));
                v.setColor(rs2.getString("color"));
                v.setStolen(rs2.getBoolean("is_stolen"));
                v.setOwnerName(rs2.getString("owner_name"));
                vehicleList.add(v);
            }
            vehicleTable.setItems(vehicleList);
            recordCountLabel.setText("Search Results: " + vehicleList.size());
        } catch (Exception e) {
            UIUtils.showError("Search Error", e.getMessage());
        }
    }

    @FXML private void handleClear(ActionEvent event) { clearFields(); }

    @FXML
    private void handleBack(ActionEvent event) {
        try { App.switchScene("/com/example/assignment/dashboard.fxml", "Dashboard"); }
        catch (Exception e) { UIUtils.showError("Navigation Error", e.getMessage()); }
    }

    private void populateFields(Vehicle v) {
        regNumberField.setText(v.getRegistrationNumber());
        makeField.setText(v.getMake());
        modelField.setText(v.getModel());
        yearField.setText(String.valueOf(v.getYear()));
        colorField.setText(v.getColor());
        engineNumberField.setText(v.getEngineNumber());
        chassisNumberField.setText(v.getChassisNumber());
    }

    private void clearFields() {
        regNumberField.clear(); makeField.clear(); modelField.clear();
        yearField.clear(); colorField.clear(); engineNumberField.clear();
        chassisNumberField.clear(); ownerCombo.getSelectionModel().clearSelection();
        selectedVehicle = null;
    }

    private int getSelectedOwnerId() {
        String selected = ownerCombo.getSelectionModel().getSelectedItem();
        if (selected != null && !selected.isEmpty()) {
            try {
                return Integer.parseInt(selected.split(" - ")[0]);
            } catch (NumberFormatException e) {
                return 1; // default
            }
        }
        return 1;
    }
}
