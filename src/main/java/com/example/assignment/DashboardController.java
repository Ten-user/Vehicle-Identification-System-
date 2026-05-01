package com.example.assignment;

import com.example.assignment.App;
import com.example.assignment.User;
import com.example.assignment.UIUtils;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Dashboard Controller - main navigation hub after login.
 * Demonstrates: Menu Bar, Menu Items, Visual Effects (FadeTransition, DropShadow),
 * Progress Indicators, and Pagination layout concepts.
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User currentUser = App.getCurrentUser();

        if (currentUser != null) {
            welcomeLabel.setText("Welcome, " + currentUser.getName());
            roleLabel.setText(currentUser.getPersonType());
        }

        // Apply DropShadow to all module buttons (Visual Effects requirement)
        applyButtonEffects();

        // Setup menu bar with module access
        setupMenuBar();

        // Fade-in animation for the dashboard content (FadeTransition requirement)
        setupFadeAnimation();

        // Setup progress indicators
        setupProgressIndicators();

        // Configure module access based on user role
        configureRoleAccess();
    }

    /**
     * Applies DropShadow effects to all module buttons.
     * Visual Effects requirement: DropShadow on at least one control.
     */
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
                // Hover effect: increase shadow on mouse enter
                btn.setOnMouseEntered(e -> btn.setEffect(hoverShadow));
                btn.setOnMouseExited(e -> btn.setEffect(shadow));
            }
        }

        // Logout button with red shadow
        DropShadow logoutShadow = new DropShadow();
        logoutShadow.setRadius(8);
        logoutShadow.setSpread(0.3);
        logoutShadow.setColor(Color.rgb(231, 76, 60, 0.4));
        if (logoutBtn != null) {
            logoutBtn.setEffect(logoutShadow);
        }
    }

    /**
     * Sets up the menu bar with File, Modules, and Help menus.
     * Menu Bar & Menu Items requirement.
     */
    private void setupMenuBar() {
        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem refreshItem = new MenuItem("Refresh Dashboard");
        refreshItem.setOnAction(e -> refreshDashboard());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> {
            if (UIUtils.showConfirmation("Exit", "Are you sure you want to exit?")) {
                System.exit(0);
            }
        });
        fileMenu.getItems().addAll(refreshItem, new SeparatorMenuItem(), exitItem);

        // Modules menu
        Menu modulesMenu = new Menu("Modules");
        MenuItem vehiclesItem = new MenuItem("Vehicles");
        vehiclesItem.setOnAction(e -> navigateToVehicles());
        MenuItem customersItem = new MenuItem("Customers");
        customersItem.setOnAction(e -> navigateToCustomers());
        MenuItem servicesItem = new MenuItem("Service Records");
        servicesItem.setOnAction(e -> navigateToServices());
        MenuItem policeItem = new MenuItem("Police Reports");
        policeItem.setOnAction(e -> navigateToPolice());
        MenuItem violationsItem = new MenuItem("Violations");
        violationsItem.setOnAction(e -> navigateToViolations());
        MenuItem insuranceItem = new MenuItem("Insurance");
        insuranceItem.setOnAction(e -> navigateToInsurance());
        MenuItem queriesItem = new MenuItem("Customer Queries");
        queriesItem.setOnAction(e -> navigateToQueries());
        MenuItem usersItem = new MenuItem("User Management");
        usersItem.setOnAction(e -> navigateToUsers());
        modulesMenu.getItems().addAll(vehiclesItem, customersItem, servicesItem,
                new SeparatorMenuItem(), policeItem, violationsItem,
                new SeparatorMenuItem(), insuranceItem, queriesItem,
                new SeparatorMenuItem(), usersItem);

        // Help menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About VIS");
        aboutItem.setOnAction(e -> UIUtils.showInfo("About Vehicle Identification System",
                "Vehicle Identification System v1.0\n\nOOP2 Assignment 2026\n" +
                "Built with JavaFX, MVC Architecture, and PostgreSQL (Neon)\n\n" +
                "Modules: Admin, Workshop, Customer, Insurance, Police"));
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, modulesMenu, helpMenu);
    }

    /**
     * Sets up fade-in animation for dashboard content.
     * Visual Effects requirement: FadeTransition.
     */
    private void setupFadeAnimation() {
        // The logout button continuously fades in and out (assignment requirement)
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

    /**
     * Sets up progress indicators with simulated progress.
     * Progress Indicators requirement: ProgressBar and ProgressIndicator.
     */
    private void setupProgressIndicators() {
        // Simulate system health progress
        systemProgressBar.setProgress(0.85);
        systemProgressIndicator.setProgress(0.85);
        systemStatusLabel.setText("System Status: Online - 85% Health");
    }

    /**
     * Configures module button visibility based on user role.
     * Admin: Full access to all modules
     * Police: Police + Violations + Vehicles (read)
     * Workshop: Service Records + Vehicles
     * Insurance: Insurance + Vehicles
     * Customer: Queries + Vehicles (own only)
     */
    private void configureRoleAccess() {
        User user = App.getCurrentUser();
        if (user == null) return;

        // Default: all buttons visible
        // For non-admin users, disable modules they shouldn't access
        if (!user.isAdmin()) {
            if (usersBtn != null) usersBtn.setDisable(true); // Only admin can manage users
        }

        if (!user.canAccessPoliceModule()) {
            if (policeBtn != null) policeBtn.setDisable(true);
            if (violationsBtn != null) violationsBtn.setDisable(true);
        }

        if (!user.canAccessWorkshopModule()) {
            if (servicesBtn != null) servicesBtn.setDisable(true);
        }

        if (!user.canAccessInsuranceModule()) {
            if (insuranceBtn != null) insuranceBtn.setDisable(true);
        }
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
        try {
            if (UIUtils.showConfirmation("Logout", "Are you sure you want to logout?")) {
                App.logout();
            }
        } catch (Exception e) {
            UIUtils.showError("Logout Error", e.getMessage());
        }
    }

    private void refreshDashboard() {
        setupProgressIndicators();
        configureRoleAccess();
    }
}
