package com.example.assignment;

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

/**
 * Vehicles Controller - FIXED for fullscreen stability.
 * CHANGES:
 * 1. showConfirmation() uses callback pattern
 * 2. Removed manual setFullScreen(true) after FileChooser (App listener handles it)
 */
public class VehiclesController implements Initializable {

    @FXML private TableView<Vehicle> vehicleTable;
    @FXML private TableColumn<Vehicle, Integer> colId;
    @FXML private TableColumn<Vehicle, String>  colRegNumber;
    @FXML private TableColumn<Vehicle, String>  colMake;
    @FXML private TableColumn<Vehicle, String>  colModel;
    @FXML private TableColumn<Vehicle, Integer> colYear;
    @FXML private TableColumn<Vehicle, String>  colColor;
    @FXML private TableColumn<Vehicle, String>  colOwner;
    @FXML private TableColumn<Vehicle, String>  colEngine;
    @FXML private TableColumn<Vehicle, String>  colChassis;
    @FXML private TableColumn<Vehicle, String>  colStolen;

    @FXML private TextField        searchField;
    @FXML private TextField        regNumberField;
    @FXML private TextField        makeField;
    @FXML private TextField        modelField;
    @FXML private TextField        yearField;
    @FXML private TextField        colorField;
    @FXML private ComboBox<String> ownerCombo;
    @FXML private TextField        engineNumberField;
    @FXML private TextField        chassisNumberField;
    @FXML private Button           saveBtn;
    @FXML private Button           updateBtn;
    @FXML private Button           deleteBtn;
    @FXML private Button           clearBtn;
    @FXML private Button           searchBtn;
    @FXML private Button           backBtn;
    @FXML private Button           uploadImageBtn;
    @FXML private Pagination       pagination;
    @FXML private Label            recordCountLabel;
    @FXML private Label            statusLabel;
    @FXML private ImageView        vehicleImageView;
    @FXML private Label            vehicleImageLabel;

    private ObservableList<Vehicle> vehicleList = FXCollections.observableArrayList();
    private Vehicle selectedVehicle;
    private File pendingImageFile;
    private static final int ROWS_PER_PAGE = 10;

    private Path getVehicleImagesDir() {
        try {
            URL classUrl = getClass().getResource("/com/example/assignment/images/vehicles/");
            if (classUrl != null) return Paths.get(classUrl.toURI());
        } catch (Exception ignored) {}
        return Paths.get("src/main/resources/com/example/assignment/images/vehicles");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ACCESS GUARD: All roles can view vehicles, so no redirect needed here.
        // But we still check for null user (session expired)
        if (App.getCurrentUser() == null) { try { App.switchScene("/com/example/assignment/login.fxml", "Login"); } catch (Exception e) {} return; }

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colRegNumber.setCellValueFactory(new PropertyValueFactory<>("registrationNumber"));
        colMake.setCellValueFactory(new PropertyValueFactory<>("make"));
        colModel.setCellValueFactory(new PropertyValueFactory<>("model"));
        colYear.setCellValueFactory(new PropertyValueFactory<>("year"));
        colColor.setCellValueFactory(new PropertyValueFactory<>("color"));
        colOwner.setCellValueFactory(new PropertyValueFactory<>("ownerName"));
        colEngine.setCellValueFactory(new PropertyValueFactory<>("engineNumber"));
        colChassis.setCellValueFactory(new PropertyValueFactory<>("chassisNumber"));
        colStolen.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().isStolen() ? "STOLEN" : "Clear"));

        colStolen.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else if ("STOLEN".equals(item)) { setText("STOLEN"); setStyle("-fx-text-fill: #E53E3E; -fx-font-weight: bold;"); }
                else { setText("Clear"); setStyle("-fx-text-fill: #1DA462;"); }
            }
        });

        UIUtils.restrictToDigits(yearField);

        if (saveBtn != null) {
            DropShadow shadow = new DropShadow();
            shadow.setRadius(8); shadow.setSpread(0.2);
            shadow.setColor(Color.rgb(26, 95, 200, 0.35));
            saveBtn.setEffect(shadow);
        }

        loadOwners();
        loadVehicles();
        setupPagination();

        vehicleTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, newVal) -> {
                if (newVal != null) { selectedVehicle = newVal; populateFields(newVal); loadVehicleImage(newVal); }
            });
    }

    private void loadVehicleImage(Vehicle v) {
        if (vehicleImageView == null) return;
        String regNo = v.getRegistrationNumber() != null
            ? v.getRegistrationNumber().replaceAll("[\\s/\\\\]", "") : "";
        for (String ext : new String[]{".png", ".jpg", ".jpeg"}) {
            URL imgUrl = getClass().getResource("/com/example/assignment/images/vehicles/" + regNo + ext);
            if (imgUrl != null) {
                vehicleImageView.setImage(new Image(imgUrl.toExternalForm()));
                if (vehicleImageLabel != null) vehicleImageLabel.setText(v.getDisplayName());
                return;
            }
        }
        vehicleImageView.setImage(null);
        if (vehicleImageLabel != null)
            vehicleImageLabel.setText("No image — click 'Upload Image' or add " + regNo + ".png to images/vehicles/");
    }

    @FXML private void handleUploadImage(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Vehicle Image");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File chosen = chooser.showOpenDialog(App.getPrimaryStage());
        // FIX: No manual setFullScreen needed — App's fullscreen listener handles it
        if (chosen == null) return;

        pendingImageFile = chosen;
        vehicleImageView.setImage(new Image(chosen.toURI().toString()));
        if (vehicleImageLabel != null)
            vehicleImageLabel.setText("Image selected — will be saved when you click Add Vehicle");
    }

    private void saveVehicleImage(String regNo) {
        if (pendingImageFile == null) return;
        try {
            String ext = pendingImageFile.getName().substring(pendingImageFile.getName().lastIndexOf('.'));
            String safeName = regNo.replaceAll("[\\s/\\\\]", "");

            Path destDir = Paths.get("src/main/resources/com/example/assignment/images/vehicles");
            if (!Files.exists(destDir)) {
                URL dirUrl = getClass().getResource("/com/example/assignment/images/vehicles/");
                if (dirUrl != null) destDir = Paths.get(dirUrl.toURI());
            }

            if (Files.exists(destDir)) {
                for (String oldExt : new String[]{".png", ".jpg", ".jpeg"}) {
                    Files.deleteIfExists(destDir.resolve(safeName + oldExt));
                }
                Files.copy(pendingImageFile.toPath(), destDir.resolve(safeName + ext),
                    StandardCopyOption.REPLACE_EXISTING);
            }
            pendingImageFile = null;
        } catch (Exception e) {
            System.err.println("Image save warning: " + e.getMessage());
        }
    }

    private void loadVehicles() {
        vehicleList.clear();
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery(
                "SELECT v.*, c.name AS owner_name FROM vehicles v " +
                "LEFT JOIN customers c ON v.owner_id = c.customer_id ORDER BY v.vehicle_id");
            while (rs != null && rs.next()) {
                Vehicle v = new Vehicle();
                v.setId(rs.getInt("vehicle_id"));
                v.setRegistrationNumber(rs.getString("registration_number"));
                v.setMake(rs.getString("make")); v.setModel(rs.getString("model"));
                v.setYear(rs.getInt("year")); v.setColor(rs.getString("color"));
                v.setEngineNumber(rs.getString("engine_number"));
                v.setChassisNumber(rs.getString("chassis_number"));
                v.setStolen(rs.getBoolean("is_stolen"));
                v.setOwnerName(rs.getString("owner_name"));
                vehicleList.add(v);
            }
            vehicleTable.setItems(vehicleList);
            if (recordCountLabel != null) recordCountLabel.setText("Total Records: " + vehicleList.size());
        } catch (Exception e) {
            UIUtils.showError("Database Error", "Failed to load vehicles: " + e.getMessage());
        }
    }

    private void loadOwners() {
        try {
            ownerCombo.getItems().clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery("SELECT customer_id, name FROM customers ORDER BY name");
            while (rs != null && rs.next())
                ownerCombo.getItems().add(rs.getInt("customer_id") + " - " + rs.getString("name"));
        } catch (Exception e) {
            if (statusLabel != null) statusLabel.setText("Could not load owners");
        }
    }

    private void setupPagination() {
        int pageCount = Math.max(1, (vehicleList.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);
        pagination.setPageFactory(pageIndex -> {
            int from = pageIndex * ROWS_PER_PAGE;
            int to = Math.min(from + ROWS_PER_PAGE, vehicleList.size());
            vehicleTable.setItems(FXCollections.observableArrayList(vehicleList.subList(from, to)));
            return vehicleTable;
        });
    }

    @FXML private void handleSave(ActionEvent event) {
        try {
            String regNum  = regNumberField.getText().trim();
            String make    = makeField.getText().trim();
            String model   = modelField.getText().trim();
            String yearStr = yearField.getText().trim();

            if (regNum.isEmpty() || make.isEmpty() || model.isEmpty()) {
                UIUtils.showWarning("Validation", "Registration Number, Make, and Model are required."); return;
            }
            if (!UIUtils.isValidInteger(yearStr) || Integer.parseInt(yearStr) < 1900) {
                UIUtils.showWarning("Validation", "Please enter a valid year (e.g. 2022)."); return;
            }
            if (make.matches(".*\\d.*")) {
                UIUtils.showWarning("Validation", "Make should not contain numbers."); return;
            }

            int year    = Integer.parseInt(yearStr);
            int ownerId = getSelectedOwnerId();

            DatabaseConnection db = DatabaseConnection.getInstance();
            int result = db.executeParameterizedUpdate(
                "INSERT INTO vehicles (registration_number, make, model, year, owner_id, color, engine_number, chassis_number, registration_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURDATE())",
                regNum, make, model, year, ownerId,
                colorField.getText().trim(), engineNumberField.getText().trim(), chassisNumberField.getText().trim());

            if (result > 0) {
                saveVehicleImage(regNum);
                UIUtils.showInfo("Success", "Vehicle added successfully!");
                clearFields(); loadVehicles(); setupPagination();
            }
        } catch (Exception e) { UIUtils.showError("Save Error", e.getMessage()); }
    }

    @FXML private void handleUpdate(ActionEvent event) {
        if (selectedVehicle == null) { UIUtils.showWarning("No Selection", "Select a vehicle to update."); return; }
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            int result = db.executeParameterizedUpdate(
                "UPDATE vehicles SET registration_number=?, make=?, model=?, year=?, color=?, engine_number=?, chassis_number=? WHERE vehicle_id=?",
                regNumberField.getText().trim(), makeField.getText().trim(), modelField.getText().trim(),
                UIUtils.safeParseInt(yearField.getText(), 2000), colorField.getText().trim(),
                engineNumberField.getText().trim(), chassisNumberField.getText().trim(), selectedVehicle.getId());
            if (result > 0) {
                if (pendingImageFile != null) saveVehicleImage(regNumberField.getText().trim());
                UIUtils.showInfo("Success", "Vehicle updated!"); loadVehicles(); clearFields();
            }
        } catch (Exception e) { UIUtils.showError("Update Error", e.getMessage()); }
    }

    @FXML private void handleDelete(ActionEvent event) {
        if (selectedVehicle == null) { UIUtils.showWarning("No Selection", "Select a vehicle to delete."); return; }
        // FIX: callback-based confirmation
        UIUtils.showConfirmation("Delete", "Delete " + selectedVehicle.getDisplayName() + "?", confirmed -> {
            if (confirmed) {
                try {
                    DatabaseConnection db = DatabaseConnection.getInstance();
                    int result = db.executeParameterizedUpdate("DELETE FROM vehicles WHERE vehicle_id = ?", selectedVehicle.getId());
                    if (result > 0) { UIUtils.showInfo("Deleted", "Vehicle deleted."); loadVehicles(); setupPagination(); clearFields(); }
                } catch (Exception e) { UIUtils.showError("Delete Error", e.getMessage()); }
            }
        });
    }

    @FXML private void handleSearch(ActionEvent event) {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) { loadVehicles(); return; }
        try {
            vehicleList.clear();
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeParameterizedQuery(
                "SELECT v.*, c.name AS owner_name FROM vehicles v LEFT JOIN customers c ON v.owner_id = c.customer_id " +
                "WHERE v.registration_number LIKE ? OR v.make LIKE ? OR v.model LIKE ? OR c.name LIKE ?",
                "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%");
            while (rs != null && rs.next()) {
                Vehicle v = new Vehicle();
                v.setId(rs.getInt("vehicle_id"));
                v.setRegistrationNumber(rs.getString("registration_number"));
                v.setMake(rs.getString("make")); v.setModel(rs.getString("model"));
                v.setYear(rs.getInt("year")); v.setColor(rs.getString("color"));
                v.setEngineNumber(rs.getString("engine_number"));
                v.setChassisNumber(rs.getString("chassis_number"));
                v.setStolen(rs.getBoolean("is_stolen"));
                v.setOwnerName(rs.getString("owner_name"));
                vehicleList.add(v);
            }
            vehicleTable.setItems(vehicleList);
            if (recordCountLabel != null) recordCountLabel.setText("Search Results: " + vehicleList.size());
        } catch (Exception e) { UIUtils.showError("Search Error", e.getMessage()); }
    }

    @FXML private void handleClear(ActionEvent e) { clearFields(); }
    @FXML private void handleBack(ActionEvent e) {
        try { App.switchScene("/com/example/assignment/dashboard.fxml", "Dashboard"); }
        catch (Exception ex) { UIUtils.showError("Error", ex.getMessage()); }
    }

    private void populateFields(Vehicle v) {
        regNumberField.setText(v.getRegistrationNumber());
        makeField.setText(v.getMake()); modelField.setText(v.getModel());
        yearField.setText(String.valueOf(v.getYear())); colorField.setText(v.getColor());
        engineNumberField.setText(v.getEngineNumber());
        chassisNumberField.setText(v.getChassisNumber());
    }

    private void clearFields() {
        regNumberField.clear(); makeField.clear(); modelField.clear();
        yearField.clear(); colorField.clear(); engineNumberField.clear();
        chassisNumberField.clear(); ownerCombo.getSelectionModel().clearSelection();
        pendingImageFile = null;
        if (vehicleImageView != null) vehicleImageView.setImage(null);
        if (vehicleImageLabel != null) vehicleImageLabel.setText("Select a vehicle to see its image");
        selectedVehicle = null;
    }

    private int getSelectedOwnerId() {
        String s = ownerCombo.getSelectionModel().getSelectedItem();
        if (s != null && !s.isEmpty()) {
            try { return Integer.parseInt(s.split(" - ")[0]); }
            catch (NumberFormatException e) { return 1; }
        }
        return 1;
    }
}
