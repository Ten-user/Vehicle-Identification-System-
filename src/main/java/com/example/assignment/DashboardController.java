package com.example.assignment;

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
 * Hides ENTIRE module cards (image + title + subtitle + button) AND
 * stat cards for modules the current role cannot access.
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
 */
public class DashboardController implements Initializable {

    // ── Top nav ──────────────────────────────────────────────────────────────
    @FXML private MenuBar menuBar;
    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Button logoutBtn;
    @FXML private ProgressBar systemProgressBar;
    @FXML private ProgressIndicator systemProgressIndicator;
    @FXML private Label systemStatusLabel;

    // ── Stat cards (entire VBox containers) ──────────────────────────────────
    @FXML private HBox statsRow;
    @FXML private VBox statVehiclesCard;
    @FXML private VBox statCustomersCard;
    @FXML private VBox statReportsCard;
    @FXML private VBox statViolationsCard;

    // ── Stat number labels (for populating live data) ─────────────────────────
    @FXML private Label statVehicles;
    @FXML private Label statCustomers;
    @FXML private Label statReports;
    @FXML private Label statViolations;

    // ── Module cards (entire VBox — image + title + subtitle + button) ───────
    @FXML private VBox vehiclesCard;
    @FXML private VBox customersCard;
    @FXML private VBox servicesCard;
    @FXML private VBox policeCard;
    @FXML private VBox violationsCard;
    @FXML private VBox insuranceCard;
    @FXML private VBox queriesCard;
    @FXML private VBox usersCard;

    // ── Open buttons (kept for effect styling) ────────────────────────────────
    @FXML private Button vehiclesBtn;
    @FXML private Button customersBtn;
    @FXML private Button servicesBtn;
    @FXML private Button policeBtn;
    @FXML private Button violationsBtn;
    @FXML private Button insuranceBtn;
    @FXML private Button queriesBtn;
    @FXML private Button usersBtn;

    // ── Module images ─────────────────────────────────────────────────────────
    @FXML private ImageView imgVehicles;
    @FXML private ImageView imgCustomers;
    @FXML private ImageView imgServices;
    @FXML private ImageView imgPolice;
    @FXML private ImageView imgViolations;
    @FXML private ImageView imgInsurance;
    @FXML private ImageView imgQueries;
    @FXML private ImageView imgUsers;

    // ── Menu items ────────────────────────────────────────────────────────────
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
        configureRoleAccess();   // ← hides cards + stat cards + menu items
        loadModuleImages();
        loadLiveStats();
    }

    // ── Button hover effects ──────────────────────────────────────────────────
    private void applyButtonEffects() {
        DropShadow shadow = new DropShadow();
        shadow.setRadius(10); shadow.setSpread(0.25);
        shadow.setColor(Color.rgb(0, 120, 215, 0.35));

        DropShadow hover = new DropShadow();
        hover.setRadius(15); hover.setSpread(0.4);
        hover.setColor(Color.rgb(0, 120, 215, 0.6));

        Button[] btns = {vehiclesBtn, customersBtn, servicesBtn, policeBtn,
                         violationsBtn, insuranceBtn, queriesBtn, usersBtn};
        for (Button b : btns) {
            if (b == null) continue;
            b.setEffect(shadow);
            b.setOnMouseEntered(e -> b.setEffect(hover));
            b.setOnMouseExited(e -> b.setEffect(shadow));
        }

        DropShadow logoutShadow = new DropShadow();
        logoutShadow.setRadius(8); logoutShadow.setSpread(0.3);
        logoutShadow.setColor(Color.rgb(231, 76, 60, 0.4));
        if (logoutBtn != null) logoutBtn.setEffect(logoutShadow);
    }

    // ── Menu bar ──────────────────────────────────────────────────────────────
    private void setupMenuBar() {
        Menu fileMenu = new Menu("File");
        MenuItem refreshItem = new MenuItem("Refresh Dashboard");
        refreshItem.setOnAction(e -> refreshDashboard());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e ->
            UIUtils.showInfo("Exit Disabled",
                "This application cannot be closed during an active session."));
        fileMenu.getItems().addAll(refreshItem, new SeparatorMenuItem(), exitItem);

        Menu modulesMenu = new Menu("Modules");
        vehiclesItem   = menuItem("Vehicles",           e -> navigateToVehicles());
        customersItem  = menuItem("Customers",          e -> navigateToCustomers());
        servicesItem   = menuItem("Service Records",    e -> navigateToServices());
        policeItem     = menuItem("Police Reports",     e -> navigateToPolice());
        violationsItem = menuItem("Violations",         e -> navigateToViolations());
        insuranceItem  = menuItem("Insurance",          e -> navigateToInsurance());
        queriesItem    = menuItem("Customer Queries",   e -> navigateToQueries());
        usersItem      = menuItem("User Management",    e -> navigateToUsers());

        modulesMenu.getItems().addAll(
            vehiclesItem, customersItem, servicesItem,
            new SeparatorMenuItem(), policeItem, violationsItem,
            new SeparatorMenuItem(), insuranceItem, queriesItem,
            new SeparatorMenuItem(), usersItem);

        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About VIS");
        aboutItem.setOnAction(e -> UIUtils.showInfo("About Vehicle Identification System",
            "Vehicle Identification System v1.0 — Kingdom of Lesotho\n\n" +
            "OOP2 Assignment 2026 — Kingdom of Lesotho\n" +
            "Built with JavaFX, MVC Architecture, and PostgreSQL (Neon)\n\n" +
            "Modules: Admin, Workshop, Customer, Insurance, Police"));
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, modulesMenu, helpMenu);
    }

    private MenuItem menuItem(String text, javafx.event.EventHandler<ActionEvent> handler) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(handler);
        return item;
    }

    // ── Role-based access ─────────────────────────────────────────────────────
    /**
     * STEP 1: Hide EVERYTHING (entire cards + stat cards + menu items).
     * STEP 2: Reveal only what the current role is allowed to see.
     *
     * Cards are hidden with setVisible(false) + setManaged(false) so
     * layout reflows naturally — no empty gaps left behind.
     */
    private void configureRoleAccess() {
        User user = App.getCurrentUser();
        if (user == null) return;
        String role = user.getRole() != null ? user.getRole().toLowerCase() : "";

        // ── Step 1: Hide ALL module cards ──────────────────────────────────
        hideCard(vehiclesCard);   hideMenu(vehiclesItem);
        hideCard(customersCard);  hideMenu(customersItem);
        hideCard(servicesCard);   hideMenu(servicesItem);
        hideCard(policeCard);     hideMenu(policeItem);
        hideCard(violationsCard); hideMenu(violationsItem);
        hideCard(insuranceCard);  hideMenu(insuranceItem);
        hideCard(queriesCard);    hideMenu(queriesItem);
        hideCard(usersCard);      hideMenu(usersItem);

        // ── Step 1b: Hide ALL stat cards ────────────────────────────────────
        hideCard(statVehiclesCard);
        hideCard(statCustomersCard);
        hideCard(statReportsCard);
        hideCard(statViolationsCard);

        // ── Step 2: Show allowed items per role ─────────────────────────────
        switch (role) {
            case "admin" -> {
                showCard(vehiclesCard);   showMenu(vehiclesItem);
                showCard(customersCard);  showMenu(customersItem);
                showCard(servicesCard);   showMenu(servicesItem);
                showCard(policeCard);     showMenu(policeItem);
                showCard(violationsCard); showMenu(violationsItem);
                showCard(insuranceCard);  showMenu(insuranceItem);
                showCard(queriesCard);    showMenu(queriesItem);
                showCard(usersCard);      showMenu(usersItem);
                showCard(statVehiclesCard);
                showCard(statCustomersCard);
                showCard(statReportsCard);
                showCard(statViolationsCard);
            }
            case "police" -> {
                showCard(vehiclesCard);   showMenu(vehiclesItem);
                showCard(policeCard);     showMenu(policeItem);
                showCard(violationsCard); showMenu(violationsItem);
                showCard(statVehiclesCard);
                showCard(statReportsCard);
                showCard(statViolationsCard);
            }
            case "workshop" -> {
                showCard(vehiclesCard);  showMenu(vehiclesItem);
                showCard(servicesCard);  showMenu(servicesItem);
                showCard(statVehiclesCard);
            }
            case "insurance" -> {
                showCard(vehiclesCard);  showMenu(vehiclesItem);
                showCard(customersCard); showMenu(customersItem);
                showCard(insuranceCard); showMenu(insuranceItem);
                showCard(statVehiclesCard);
                showCard(statCustomersCard);
            }
            case "customer" -> {
                showCard(vehiclesCard); showMenu(vehiclesItem);
                showCard(queriesCard);  showMenu(queriesItem);
                showCard(statVehiclesCard);
            }
        }
    }

    private void hideCard(javafx.scene.Node card) {
        if (card == null) return;
        card.setVisible(false);
        card.setManaged(false);
    }

    private void showCard(javafx.scene.Node card) {
        if (card == null) return;
        card.setVisible(true);
        card.setManaged(true);
    }

    private void hideMenu(MenuItem item) { if (item != null) item.setVisible(false); }
    private void showMenu(MenuItem item) { if (item != null) item.setVisible(true); }

    // ── Misc setup ────────────────────────────────────────────────────────────
    private void setupFadeAnimation() {
        if (logoutBtn == null) return;
        FadeTransition out = new FadeTransition(Duration.seconds(2), logoutBtn);
        out.setFromValue(1.0); out.setToValue(0.3);
        FadeTransition in = new FadeTransition(Duration.seconds(2), logoutBtn);
        in.setFromValue(0.3); in.setToValue(1.0);
        SequentialTransition cycle = new SequentialTransition(out, in);
        cycle.setCycleCount(javafx.animation.Animation.INDEFINITE);
        cycle.play();
    }

    private void setupProgressIndicators() {
        systemProgressBar.setProgress(0.85);
        systemProgressIndicator.setProgress(0.85);
        systemStatusLabel.setText("System Status: Online - 85% Health");
    }

    private void loadModuleImages() {
        loadImage(imgVehicles,   "vehicles");
        loadImage(imgCustomers,  "customers");
        loadImage(imgServices,   "services");
        loadImage(imgPolice,     "police");
        loadImage(imgViolations, "violations");
        loadImage(imgInsurance,  "insurance");
        loadImage(imgQueries,    "queries");
        loadImage(imgUsers,      "users");
    }

    private void loadImage(ImageView iv, String name) {
        if (iv == null) return;
        for (String ext : new String[]{".png", ".jpg", ".jpeg"}) {
            java.net.URL url = getClass().getResource(
                "/com/example/assignment/images/modules/" + name + ext);
            if (url != null) { iv.setImage(new Image(url.toExternalForm())); return; }
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
                java.sql.ResultSet rs = db.executeQuery(
                    "SELECT COUNT(*) FROM violations WHERE status = 'Unpaid'");
                if (rs != null && rs.next()) statViolations.setText(String.valueOf(rs.getInt(1)));
            }
        } catch (Exception ignored) {}
    }

    private void refreshDashboard() {
        setupProgressIndicators();
        configureRoleAccess();
        loadLiveStats();
    }

    // ── Navigation handlers ───────────────────────────────────────────────────
    @FXML private void navigateToVehicles() {
        navigate("/com/example/assignment/vehicles.fxml", "Vehicle Management"); }
    @FXML private void navigateToCustomers() {
        navigate("/com/example/assignment/customers.fxml", "Customer Management"); }
    @FXML private void navigateToServices() {
        navigate("/com/example/assignment/services.fxml", "Service Records"); }
    @FXML private void navigateToPolice() {
        navigate("/com/example/assignment/police.fxml", "Police Reports"); }
    @FXML private void navigateToViolations() {
        navigate("/com/example/assignment/violations.fxml", "Violations"); }
    @FXML private void navigateToInsurance() {
        navigate("/com/example/assignment/insurance.fxml", "Insurance Policies"); }
    @FXML private void navigateToQueries() {
        navigate("/com/example/assignment/queries.fxml", "Customer Queries"); }
    @FXML private void navigateToUsers() {
        navigate("/com/example/assignment/users.fxml", "User Management"); }

    private void navigate(String fxml, String title) {
        try { App.switchScene(fxml, title); }
        catch (Exception e) { UIUtils.showError("Navigation Error", e.getMessage()); }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        UIUtils.showConfirmation("Logout", "Are you sure you want to logout?", confirmed -> {
            if (confirmed) {
                try { App.logout(); }
                catch (Exception e) { UIUtils.showError("Logout Error", e.getMessage()); }
            }
        });
    }
}
