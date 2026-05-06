package com.example.assignment;

import javafx.application.Platform;
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
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.nio.file.*;
import java.sql.ResultSet;
import java.util.ResourceBundle;

public class VehiclesController implements Initializable {

    @FXML private TableView<Vehicle> vehicleTable;
    @FXML private TableColumn<Vehicle, Integer> colId;
    @FXML private TableColumn<Vehicle, String> colRegNumber;
    @FXML private TableColumn<Vehicle, String> colMake;
    @FXML private TableColumn<Vehicle, String> colModel;
    @FXML private TableColumn<Vehicle, Integer> colYear;
    @FXML private TableColumn<Vehicle, String> colColor;
    @FXML private TableColumn<Vehicle, String> colOwner;
    @FXML private TableColumn<Vehicle, String> colEngine;
    @FXML private TableColumn<Vehicle, String> colChassis;
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
    @FXML private Button refreshBtn;
    @FXML private Button uploadImageBtn;

    @FXML private Pagination pagination;
    @FXML private Label recordCountLabel;
    @FXML private Label statusLabel;
    @FXML private ImageView vehicleImageView;
    @FXML private Label vehicleImageLabel;

    // Role-based visibility containers
    @FXML private javafx.scene.control.ScrollPane formPane;
    @FXML private javafx.scene.layout.HBox actionBtnsBox;
    @FXML private SplitPane mainSplitPane;

    private ObservableList<Vehicle> vehicleList = FXCollections.observableArrayList();
    private Vehicle selectedVehicle;
    private File pendingImageFile;

    private static final int ROWS_PER_PAGE = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        if (App.getCurrentUser() == null) {
            try {
                App.switchScene("/com/example/assignment/login.fxml", "Login");
            } catch (Exception ignored) {}
            return;
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colRegNumber.setCellValueFactory(new PropertyValueFactory<>("registrationNumber"));
        colMake.setCellValueFactory(new PropertyValueFactory<>("make"));
        colModel.setCellValueFactory(new PropertyValueFactory<>("model"));
        colYear.setCellValueFactory(new PropertyValueFactory<>("year"));
        colColor.setCellValueFactory(new PropertyValueFactory<>("color"));
        colOwner.setCellValueFactory(new PropertyValueFactory<>("ownerName"));
        colEngine.setCellValueFactory(new PropertyValueFactory<>("engineNumber"));
        colChassis.setCellValueFactory(new PropertyValueFactory<>("chassisNumber"));

        colStolen.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().isStolen() ? "STOLEN" : "Clear"));

        colStolen.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else if ("STOLEN".equals(item)) {
                    setText(item);
                    setStyle("-fx-text-fill: #E53E3E; -fx-font-weight: bold;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #1DA462; -fx-font-weight: bold;");
                }
            }
        });

        UIUtils.restrictToDigits(yearField);

        if (saveBtn != null) {
            DropShadow shadow = new DropShadow();
            shadow.setRadius(8);
            shadow.setSpread(0.2);
            shadow.setColor(Color.rgb(26, 95, 200, 0.35));
            saveBtn.setEffect(shadow);
        }

        loadOwners();
        loadVehicles();

        setupPagination();

        vehicleTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, val) -> {
                    if (val != null) {
                        selectedVehicle = val;
                        populateFields(val);
                        loadVehicleImage(val);
                    }
                });

        applyRoleUI();
    }

    /** Hide everything the current role must not see in this module. */
    private void applyRoleUI() {
        // Vehicles: only Admin can add/edit/delete. All other roles are view-only.
        if (!UIUtils.isAdmin()) {
            // Hide entire right-side form panel
            UIUtils.hide(formPane);
            // Expand table to full width by moving divider to the right edge
            if (mainSplitPane != null) {
                javafx.application.Platform.runLater(() ->
                    mainSplitPane.setDividerPositions(1.0));
            }
        }
    }

    private boolean isCustomer() {
        User u = App.getCurrentUser();
        return u != null && "Customer".equalsIgnoreCase(u.getRole());
    }

    private void loadVehicles() {
        vehicleList.clear();

        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs;
            if (isCustomer()) {
                User u = App.getCurrentUser();
                rs = db.executeParameterizedQuery(
                    "SELECT * FROM vehicle_owner_details WHERE owner_name = ? ORDER BY vehicle_id",
                    u.getName());
            } else {
                rs = db.executeQuery("SELECT * FROM vehicle_owner_details ORDER BY vehicle_id");
            }

            while (rs != null && rs.next()) {
                Vehicle v = new Vehicle();
                v.setId(rs.getInt("vehicle_id"));
                v.setRegistrationNumber(rs.getString("registration_number"));
                v.setMake(rs.getString("make"));
                v.setModel(rs.getString("model"));
                v.setYear(rs.getInt("year"));
                v.setColor(rs.getString("color"));
                v.setEngineNumber(rs.getString("engine_number"));
                v.setChassisNumber(rs.getString("chassis_number"));
                v.setStolen(rs.getBoolean("is_stolen"));
                v.setOwnerName(rs.getString("owner_name"));

                vehicleList.add(v);
            }

            vehicleTable.setItems(vehicleList);

            if (recordCountLabel != null)
                recordCountLabel.setText("Total Records: " + vehicleList.size());

            setupPagination();

        } catch (Exception e) {
            UIUtils.showError("Database Error", e.getMessage());
        }
    }

    private void setupPagination() {
        if (pagination == null) return;

        int pageCount = Math.max(1, (vehicleList.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        pagination.setPageCount(pageCount);

        pagination.setPageFactory(pageIndex -> {
            int from = pageIndex * ROWS_PER_PAGE;
            int to = Math.min(from + ROWS_PER_PAGE, vehicleList.size());

            vehicleTable.setItems(
                    FXCollections.observableArrayList(vehicleList.subList(from, to))
            );

            return vehicleTable;
        });
    }

    private void loadOwners() {
        if (ownerCombo == null) return;

        try {
            ownerCombo.getItems().clear();
            DatabaseConnection db = DatabaseConnection.getInstance();

            if (isCustomer()) {
                // Auto-set to logged-in customer — look up their customer_id
                User u = App.getCurrentUser();
                ResultSet rs = db.executeParameterizedQuery(
                    "SELECT customer_id, name FROM customers WHERE name = ? OR email = ? LIMIT 1",
                    u.getName(), u.getEmail());
                if (rs != null && rs.next()) {
                    String entry = rs.getInt("customer_id") + " - " + rs.getString("name");
                    ownerCombo.getItems().add(entry);
                    ownerCombo.setValue(entry);
                } else {
                    // Fallback: just show their name even without a customer record match
                    ownerCombo.getItems().add("0 - " + u.getName());
                    ownerCombo.setValue("0 - " + u.getName());
                }
                ownerCombo.setDisable(true);
            } else {
                ResultSet rs = db.executeQuery("SELECT customer_id, name FROM customers ORDER BY name");
                while (rs != null && rs.next()) {
                    ownerCombo.getItems().add(rs.getInt("customer_id") + " - " + rs.getString("name"));
                }
            }
        } catch (Exception ignored) {}
    }

    private void loadVehicleImage(Vehicle v) {
        if (vehicleImageView == null || v == null) return;

        try {
            String reg = v.getRegistrationNumber() == null ? "" :
                    v.getRegistrationNumber().replaceAll("[\\s/\\\\]", "");

            for (String ext : new String[]{".png", ".jpg", ".jpeg"}) {
                URL url = getClass().getResource(
                        "/com/example/assignment/images/vehicles/" + reg + ext
                );

                if (url != null) {
                    vehicleImageView.setImage(new Image(url.toExternalForm()));
                    if (vehicleImageLabel != null)
                        vehicleImageLabel.setText(v.getDisplayName());
                    return;
                }
            }

            vehicleImageView.setImage(null);
            if (vehicleImageLabel != null)
                vehicleImageLabel.setText("No image found");

        } catch (Exception ignored) {}
    }

    @FXML
    private void handleSave(ActionEvent e) {
        try {
            String reg = regNumberField.getText().trim();
            String make = makeField.getText().trim();
            String model = modelField.getText().trim();
            String yearStr = yearField.getText().trim();
            String color = colorField.getText().trim();
            String engine = engineNumberField.getText().trim();
            String chassis = chassisNumberField.getText().trim();

            if (reg.isEmpty() || make.isEmpty() || model.isEmpty() || yearStr.isEmpty()) {
                UIUtils.showWarning("Validation", "Registration, Make, Model and Year are required.");
                return;
            }

            int year = Integer.parseInt(yearStr);
            int ownerId = getSelectedOwnerId();

            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet addRs = db.executeParameterizedQuery(
                    "SELECT add_vehicle(?, ?, ?, ?, ?, ?, ?, ?) AS new_id",
                    reg, make, model, year, ownerId, color, engine, chassis);
            int newId = (addRs != null && addRs.next()) ? addRs.getInt("new_id") : -1;

            if (newId > 0) {
                // Save pending image if any
                if (pendingImageFile != null) saveVehicleImage(reg, pendingImageFile);
                UIUtils.showInfo("Success", "Vehicle added!");
                clearFields();
                loadVehicles();
            }
        } catch (Exception ex) {
            UIUtils.showError("Save Error", ex.getMessage());
        }
    }

    @FXML
    private void handleUpdate(ActionEvent e) {
        if (selectedVehicle == null) {
            UIUtils.showWarning("No Selection", "Select a vehicle to update.");
            return;
        }
        try {
            String reg = regNumberField.getText().trim();
            String make = makeField.getText().trim();
            String model = modelField.getText().trim();
            String yearStr = yearField.getText().trim();
            String color = colorField.getText().trim();
            String engine = engineNumberField.getText().trim();
            String chassis = chassisNumberField.getText().trim();

            if (reg.isEmpty() || make.isEmpty() || model.isEmpty() || yearStr.isEmpty()) {
                UIUtils.showWarning("Validation", "Registration, Make, Model and Year are required.");
                return;
            }

            int year = Integer.parseInt(yearStr);
            int ownerId = getSelectedOwnerId();

            DatabaseConnection db = DatabaseConnection.getInstance();
            int result = db.executeParameterizedUpdate(
                    "UPDATE vehicles SET registration_number=?, make=?, model=?, year=?, color=?, owner_id=?, engine_number=?, chassis_number=? WHERE vehicle_id=?",
                    reg, make, model, year, color, ownerId, engine, chassis, selectedVehicle.getId());

            if (result > 0) {
                if (pendingImageFile != null) saveVehicleImage(reg, pendingImageFile);
                UIUtils.showInfo("Success", "Vehicle updated!");
                clearFields();
                loadVehicles();
            }
        } catch (Exception ex) {
            UIUtils.showError("Update Error", ex.getMessage());
        }
    }

    @FXML
    private void handleDelete(ActionEvent e) {
        if (selectedVehicle == null) {
            UIUtils.showWarning("No Selection", "Select a vehicle to delete.");
            return;
        }
        UIUtils.showConfirmation("Delete", "Delete vehicle " + selectedVehicle.getRegistrationNumber() + "?", confirmed -> {
            if (confirmed) {
                try {
                    DatabaseConnection db = DatabaseConnection.getInstance();
                    db.executeParameterizedUpdate(
                            "DELETE FROM vehicles WHERE vehicle_id = ?",
                            selectedVehicle.getId());
                    UIUtils.showInfo("Deleted", "Vehicle removed.");
                    clearFields();
                    loadVehicles();
                } catch (Exception ex) {
                    UIUtils.showError("Delete Error", ex.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleSearch(ActionEvent e) {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) { loadVehicles(); return; }

        vehicleList.clear();
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs;
            if (isCustomer()) {
                User u = App.getCurrentUser();
                rs = db.executeParameterizedQuery(
                    "SELECT * FROM vehicle_owner_details WHERE owner_name = ? " +
                    "AND (registration_number LIKE ? OR make LIKE ? OR model LIKE ?)",
                    u.getName(), "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%");
            } else {
                rs = db.executeParameterizedQuery(
                    "SELECT * FROM vehicle_owner_details " +
                    "WHERE registration_number LIKE ? OR make LIKE ? OR model LIKE ? OR owner_name LIKE ?",
                    "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%");
            }

            while (rs != null && rs.next()) {
                Vehicle v = new Vehicle();
                v.setId(rs.getInt("vehicle_id"));
                v.setRegistrationNumber(rs.getString("registration_number"));
                v.setMake(rs.getString("make"));
                v.setModel(rs.getString("model"));
                v.setYear(rs.getInt("year"));
                v.setColor(rs.getString("color"));
                v.setEngineNumber(rs.getString("engine_number"));
                v.setChassisNumber(rs.getString("chassis_number"));
                v.setStolen(rs.getBoolean("is_stolen"));
                v.setOwnerName(rs.getString("owner_name"));
                vehicleList.add(v);
            }
            vehicleTable.setItems(vehicleList);
            if (recordCountLabel != null)
                recordCountLabel.setText("Search Results: " + vehicleList.size());
            setupPagination();
        } catch (Exception ex) {
            UIUtils.showError("Search Error", ex.getMessage());
        }
    }

    @FXML
    private void handleUploadImage(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Vehicle Image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            pendingImageFile = file;
            try {
                vehicleImageView.setImage(new javafx.scene.image.Image(file.toURI().toURL().toExternalForm()));
                if (vehicleImageLabel != null) vehicleImageLabel.setText(file.getName());
            } catch (Exception ex) { /* silent */ }
        }
    }

    private void saveVehicleImage(String regNumber, File imageFile) {
        try {
            String ext = imageFile.getName().substring(imageFile.getName().lastIndexOf('.'));
            String safeReg = regNumber.replaceAll("[\\s/\\\\]", "");
            java.net.URL dest = getClass().getResource("/com/example/assignment/images/vehicles/");
            if (dest != null) {
                Path target = Path.of(dest.toURI()).resolve(safeReg + ext);
                Files.copy(imageFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ex) { /* silent — image save is best-effort */ }
    }

    @FXML
    private void handleClear(ActionEvent e) {
        clearFields();
    }

    @FXML
    private void handleRefresh(ActionEvent e) {
        loadOwners();
        loadVehicles();
    }

    @FXML
    private void handleBack(ActionEvent e) {
        try {
            App.switchScene("/com/example/assignment/dashboard.fxml", "Dashboard");
        } catch (Exception ex) {
            UIUtils.showError("Error", ex.getMessage());
        }
    }

    private void populateFields(Vehicle v) {
        regNumberField.setText(v.getRegistrationNumber());
        makeField.setText(v.getMake());
        modelField.setText(v.getModel());
        yearField.setText(String.valueOf(v.getYear()));
        colorField.setText(v.getColor());
        engineNumberField.setText(v.getEngineNumber());
        chassisNumberField.setText(v.getChassisNumber());
        // Restore owner combo
        if (ownerCombo != null && v.getOwnerName() != null) {
            ownerCombo.getItems().stream()
                .filter(item -> item.contains(v.getOwnerName()))
                .findFirst()
                .ifPresent(ownerCombo::setValue);
        }
    }

    private void clearFields() {
        regNumberField.clear();
        makeField.clear();
        modelField.clear();
        yearField.clear();
        colorField.clear();
        engineNumberField.clear();
        chassisNumberField.clear();

        ownerCombo.getSelectionModel().clearSelection();

        selectedVehicle = null;
        pendingImageFile = null;

        if (vehicleImageView != null) vehicleImageView.setImage(null);
        if (vehicleImageLabel != null) vehicleImageLabel.setText("No vehicle selected");
    }

    private int getSelectedOwnerId() {
        String val = ownerCombo.getValue();
        if (val == null) return 1;

        try {
            return Integer.parseInt(val.split(" - ")[0]);
        } catch (Exception e) {
            return 1;
        }
    }
}