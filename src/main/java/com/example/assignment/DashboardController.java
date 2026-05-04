package com.example.assignment;

import com.example.assignment.App;
import com.example.assignment.User;
import com.example.assignment.UIUtils;
import javafx.animation.FadeTransition;
import javafx.animation.SequentialTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Dashboard Controller — strict role-based access.
 *
 * ACCESS RULES:
 * ┌───────────┬──────────┬────────────┬─────────┬────────────┬────────────┬───────────┬─────────┬──────┐
 * │ Role      │ Vehicles │ Customers  │ Services│ Police     │ Violations │ Insurance │ Queries │ Users│
 * ├───────────┼──────────┼────────────┼─────────┼────────────┼────────────┼───────────┼─────────┼──────┤
 * │ Admin     │    YES   │    YES     │   YES   │    YES     │    YES     │    YES    │   YES   │ YES  │
 * │ Police    │    YES   │    NO      │   NO    │    YES     │    YES     │    NO     │   NO    │ NO   │
 * │ Workshop  │    YES   │    NO      │   YES   │    NO      │    NO      │    NO     │   NO    │ NO   │
 * │ Insurance │    YES   │    YES     │   NO    │    NO      │    NO      │    YES    │   NO    │ NO   │
 * │ Customer  │    YES   │    NO      │   NO    │    NO      │    NO      │    NO     │   YES   │ NO   │
 * └───────────┴──────────┴────────────┴─────────┴────────────┴────────────┴───────────┴─────────┴──────┘
 *
 * Implementation:
 * 1. Dashboard hides ALL module buttons + menu items FIRST, then shows only allowed ones.
 * 2. Each module controller has an access guard that redirects unauthorized users back.
 *    (Defense in depth — even if someone bypasses the dashboard, the controller blocks them.)
 */
public class DashboardController implements Initializable {

    @FXML private VBox rootContainer;
    @FXML private MenuBar menuBar;
    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Button vehiclesBtn;
    @FXML private Button customersBtn;
    @FXML private Button servicesBtn;
    @FXML private Button policeBtn;
    @FXML private Button violationsBtn;
    @FXML private Button insuranceBtn;
    @FXML private Button queriesBtn;
    @FXML private Button usersBtn;
    @FXML private Button logoutBtn;
    @FXML private ProgressBar systemProgressBar;
    @FXML private ProgressIndicator systemProgressIndicator;
    @FXML private Label systemStatusLabel;
    @FXML private Label statVehicles;
    @FXML private Label statCustomers;
    @FXML private Label statReports;
    @FXML private Label statViolations;
    @FXML private ImageView imgVehicles;
    @FXML private ImageView imgCustomers;
    @FXML private ImageView imgServices;
    @FXML private ImageView imgPolice;
    @FXML private ImageView imgViolations;
    @FXML private ImageView imgInsurance;
    @FXML private ImageView imgQueries;
    @FXML private ImageView imgUsers;

    // Menu items stored as fields so we can hide them per role
    private MenuItem vehiclesItem, customersItem, servicesItem;
    private MenuItem policeItem, violationsItem, insuranceItem;
    private MenuItem queriesItem, usersItem;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User currentUser = App.getCurrentUser();

        if (currentUser != null) {
            welcomeLabel.setText("Welcome, " + currentUser.getName());
            roleLabel.setText(currentUser.getPersonType());
        }

        applyButtonEffects();
        setupMenuBar();
        setupFadeAnimation();
        setupProgressIndicators();
        configureRoleAccess();
        loadModuleImages();
        loadLiveStats();
    }

    private void applyButtonEffects() {
        DropShadow shadow = new DropShadow();
        shadow.setRadius(10);
        shadow.setSpread(0.25);
        shadow.setColor(Color.rgb(0, 120, 215, 0.35));

        DropShadow hoverShadow = new DropShadow();
        hoverShadow.setRadius(15);
        hoverShadow.setSpread(0.4);
        hoverShadow.setColor(Color.rgb(0, 120, 215, 0.6));

        Button[] buttons = {vehiclesBtn, customersBtn, servicesBtn, policeBtn,
                           violationsBtn, insuranceBtn, queriesBtn, usersBtn};

        for (Button btn : buttons) {
            if (btn != null) {
                btn.setEffect(shadow);
                btn.setOnMouseEntered(e -> btn.setEffect(hoverShadow));
                btn.setOnMouseExited(e -> btn.setEffect(shadow));
            }
        }

        DropShadow logoutShadow = new DropShadow();
        logoutShadow.setRadius(8);
        logoutShadow.setSpread(0.3);
        logoutShadow.setColor(Color.rgb(231, 76, 60, 0.4));
        if (logoutBtn != null) {
            logoutBtn.setEffect(logoutShadow);
        }
    }

    private void setupMenuBar() {
        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem refreshItem = new MenuItem("Refresh Dashboard");
        refreshItem.setOnAction(e -> refreshDashboard());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> {
            UIUtils.showInfo("Exit Disabled", "This application cannot be closed during an active session.");
        });
        fileMenu.getItems().addAll(refreshItem, new SeparatorMenuItem(), exitItem);

        // Modules menu — store items as fields for role-based visibility
        Menu modulesMenu = new Menu("Modules");

        vehiclesItem = new MenuItem("Vehicles");
        vehiclesItem.setOnAction(e -> navigateToVehicles());
        customersItem = new MenuItem("Customers");
        customersItem.setOnAction(e -> navigateToCustomers());
        servicesItem = new MenuItem("Service Records");
        servicesItem.setOnAction(e -> navigateToServices());
        policeItem = new MenuItem("Police Reports");
        policeItem.setOnAction(e -> navigateToPolice());
        violationsItem = new MenuItem("Violations");
        violationsItem.setOnAction(e -> navigateToViolations());
        insuranceItem = new MenuItem("Insurance");
        insuranceItem.setOnAction(e -> navigateToInsurance());
        queriesItem = new MenuItem("Customer Queries");
        queriesItem.setOnAction(e -> navigateToQueries());
        usersItem = new MenuItem("User Management");
        usersItem.setOnAction(e -> navigateToUsers());

        modulesMenu.getItems().addAll(
            vehiclesItem, customersItem, servicesItem,
            new SeparatorMenuItem(), policeItem, violationsItem,
            new SeparatorMenuItem(), insuranceItem, queriesItem,
            new SeparatorMenuItem(), usersItem);

        // Help menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About VIS");
        aboutItem.setOnAction(e -> UIUtils.showInfo("About Vehicle Identification System",
                "Vehicle Identification System v1.0 — Kingdom of Lesotho\n\nOOP2 Assignment 2026 — Kingdom of Lesotho\n" +
                "Built with JavaFX, MVC Architecture, and PostgreSQL (Neon)\n\n" +
                "Modules: Admin, Workshop, Customer, Insurance, Police"));
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, modulesMenu, helpMenu);
    }

    private void setupFadeAnimation() {
        if (logoutBtn != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(2), logoutBtn);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.3);

            FadeTransition fadeIn = new FadeTransition(Duration.seconds(2), logoutBtn);
            fadeIn.setFromValue(0.3);
            fadeIn.setToValue(1.0);

            SequentialTransition fadeCycle = new SequentialTransition(fadeOut, fadeIn);
            fadeCycle.setCycleCount(javafx.animation.Animation.INDEFINITE);
            fadeCycle.play();
        }
    }

    private void setupProgressIndicators() {
        systemProgressBar.setProgress(0.85);
        systemProgressIndicator.setProgress(0.85);
        systemStatusLabel.setText("System Status: Online - 85% Health");
    }

    /**
     * STRICT role-based access control.
     * Step 1: Hide EVERYTHING (buttons + menu items)
     * Step 2: Show ONLY what the role allows
     *
     * This is a whitelist approach — far safer than blacklisting.
     */
    private void configureRoleAccess() {
        User user = App.getCurrentUser();
        if (user == null) return;
        String role = user.getRole() != null ? user.getRole().toLowerCase() : "";

        // ── Step 1: Hide ALL modules ──
        setModuleVisible(vehiclesBtn, false);
        setModuleVisible(customersBtn, false);
        setModuleVisible(servicesBtn, false);
        setModuleVisible(policeBtn, false);
        setModuleVisible(violationsBtn, false);
        setModuleVisible(insuranceBtn, false);
        setModuleVisible(queriesBtn, false);
        setModuleVisible(usersBtn, false);

        setMenuItemVisible(vehiclesItem, false);
        setMenuItemVisible(customersItem, false);
        setMenuItemVisible(servicesItem, false);
        setMenuItemVisible(policeItem, false);
        setMenuItemVisible(violationsItem, false);
        setMenuItemVisible(insuranceItem, false);
        setMenuItemVisible(queriesItem, false);
        setMenuItemVisible(usersItem, false);

        // Also hide stat cards that aren't relevant
        setLabelVisible(statVehicles, false);
        setLabelVisible(statCustomers, false);
        setLabelVisible(statReports, false);
        setLabelVisible(statViolations, false);

        // ── Step 2: Show ONLY allowed modules per role ──
        switch (role) {
            case "admin" -> {
                // Admin sees EVERYTHING
                setModuleVisible(vehiclesBtn, true);
                setModuleVisible(customersBtn, true);
                setModuleVisible(servicesBtn, true);
                setModuleVisible(policeBtn, true);
                setModuleVisible(violationsBtn, true);
                setModuleVisible(insuranceBtn, true);
                setModuleVisible(queriesBtn, true);
                setModuleVisible(usersBtn, true);

                setMenuItemVisible(vehiclesItem, true);
                setMenuItemVisible(customersItem, true);
                setMenuItemVisible(servicesItem, true);
                setMenuItemVisible(policeItem, true);
                setMenuItemVisible(violationsItem, true);
                setMenuItemVisible(insuranceItem, true);
                setMenuItemVisible(queriesItem, true);
                setMenuItemVisible(usersItem, true);

                setLabelVisible(statVehicles, true);
                setLabelVisible(statCustomers, true);
                setLabelVisible(statReports, true);
                setLabelVisible(statViolations, true);
            }
            case "police" -> {
                // Police: Vehicles, Police Reports, Violations
                setModuleVisible(vehiclesBtn, true);
                setModuleVisible(policeBtn, true);
                setModuleVisible(violationsBtn, true);

                setMenuItemVisible(vehiclesItem, true);
                setMenuItemVisible(policeItem, true);
                setMenuItemVisible(violationsItem, true);

                setLabelVisible(statVehicles, true);
                setLabelVisible(statReports, true);
                setLabelVisible(statViolations, true);
            }
            case "workshop" -> {
                // Workshop: Vehicles, Service Records
                setModuleVisible(vehiclesBtn, true);
                setModuleVisible(servicesBtn, true);

                setMenuItemVisible(vehiclesItem, true);
                setMenuItemVisible(servicesItem, true);

                setLabelVisible(statVehicles, true);
            }
            case "insurance" -> {
                // Insurance: Vehicles, Insurance, Customers
                setModuleVisible(vehiclesBtn, true);
                setModuleVisible(insuranceBtn, true);
                setModuleVisible(customersBtn, true);

                setMenuItemVisible(vehiclesItem, true);
                setMenuItemVisible(insuranceItem, true);
                setMenuItemVisible(customersItem, true);

                setLabelVisible(statVehicles, true);
                setLabelVisible(statCustomers, true);
            }
            case "customer" -> {
                // Customer: Vehicles (view), Customer Queries
                setModuleVisible(vehiclesBtn, true);
                setModuleVisible(queriesBtn, true);

                setMenuItemVisible(vehiclesItem, true);
                setMenuItemVisible(queriesItem, true);

                setLabelVisible(statVehicles, true);
            }
        }
    }

    private void setModuleVisible(Button btn, boolean visible) {
        if (btn == null) return;
        btn.setVisible(visible);
        btn.setManaged(visible);
    }

    private void setMenuItemVisible(MenuItem item, boolean visible) {
        if (item == null) return;
        item.setVisible(visible);
    }

    private void setLabelVisible(Label label, boolean visible) {
        if (label == null) return;
        label.setVisible(visible);
        label.setManaged(visible);
    }

    // Navigation handlers
    @FXML private void navigateToVehicles() {
        try { App.switchScene("/com/example/assignment/vehicles.fxml", "Vehicle Management"); }
        catch (Exception e) { UIUtils.showError("Navigation Error", e.getMessage()); }
    }
    @FXML private void navigateToCustomers() {
        try { App.switchScene("/com/example/assignment/customers.fxml", "Customer Management"); }
        catch (Exception e) { UIUtils.showError("Navigation Error", e.getMessage()); }
    }
    @FXML private void navigateToServices() {
        try { App.switchScene("/com/example/assignment/services.fxml", "Service Records"); }
        catch (Exception e) { UIUtils.showError("Navigation Error", e.getMessage()); }
    }
    @FXML private void navigateToPolice() {
        try { App.switchScene("/com/example/assignment/police.fxml", "Police Reports"); }
        catch (Exception e) { UIUtils.showError("Navigation Error", e.getMessage()); }
    }
    @FXML private void navigateToViolations() {
        try { App.switchScene("/com/example/assignment/violations.fxml", "Violations"); }
        catch (Exception e) { UIUtils.showError("Navigation Error", e.getMessage()); }
    }
    @FXML private void navigateToInsurance() {
        try { App.switchScene("/com/example/assignment/insurance.fxml", "Insurance Policies"); }
        catch (Exception e) { UIUtils.showError("Navigation Error", e.getMessage()); }
    }
    @FXML private void navigateToQueries() {
        try { App.switchScene("/com/example/assignment/queries.fxml", "Customer Queries"); }
        catch (Exception e) { UIUtils.showError("Navigation Error", e.getMessage()); }
    }
    @FXML private void navigateToUsers() {
        try { App.switchScene("/com/example/assignment/users.fxml", "User Management"); }
        catch (Exception e) { UIUtils.showError("Navigation Error", e.getMessage()); }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        UIUtils.showConfirmation("Logout", "Are you sure you want to logout?", confirmed -> {
            if (confirmed) {
                try {
                    App.logout();
                } catch (Exception e) {
                    UIUtils.showError("Logout Error", e.getMessage());
                }
            }
        });
    }

    private void refreshDashboard() {
        setupProgressIndicators();
        configureRoleAccess();
    }

    private void loadModuleImages() {
        loadModuleImage(imgVehicles,   "vehicles");
        loadModuleImage(imgCustomers,  "customers");
        loadModuleImage(imgServices,   "services");
        loadModuleImage(imgPolice,     "police");
        loadModuleImage(imgViolations, "violations");
        loadModuleImage(imgInsurance,  "insurance");
        loadModuleImage(imgQueries,    "queries");
        loadModuleImage(imgUsers,      "users");
    }

    private void loadModuleImage(ImageView iv, String name) {
        if (iv == null) return;
        String[] exts = {".png", ".jpg", ".jpeg"};
        for (String ext : exts) {
            java.net.URL url = getClass().getResource("/com/example/assignment/images/modules/" + name + ext);
            if (url != null) {
                iv.setImage(new Image(url.toExternalForm()));
                return;
            }
        }
    }

    private void loadLiveStats() {
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            if (statVehicles != null) {
                java.sql.ResultSet rs = db.executeQuery("SELECT COUNT(*) FROM vehicles");
                if (rs != null && rs.next()) statVehicles.setText(String.valueOf(rs.getInt(1)));
            }
            if (statCustomers != null) {
                java.sql.ResultSet rs = db.executeQuery("SELECT COUNT(*) FROM customers");
                if (rs != null && rs.next()) statCustomers.setText(String.valueOf(rs.getInt(1)));
            }
            if (statReports != null) {
                java.sql.ResultSet rs = db.executeQuery("SELECT COUNT(*) FROM police_reports");
                if (rs != null && rs.next()) statReports.setText(String.valueOf(rs.getInt(1)));
            }
            if (statViolations != null) {
                java.sql.ResultSet rs = db.executeQuery("SELECT COUNT(*) FROM violations WHERE status = 'Unpaid'");
                if (rs != null && rs.next()) statViolations.setText(String.valueOf(rs.getInt(1)));
            }
        } catch (Exception e) {
            // Stats are cosmetic — silent fail
        }
    }
}
