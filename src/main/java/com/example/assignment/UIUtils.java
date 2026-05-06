package com.example.assignment;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * UI Utility class with OVERLAY-BASED dialogs.
 *
 * KEY FIX: All dialog methods (showInfo, showError, showWarning, showConfirmation)
 * now render as overlays INSIDE the scene instead of creating separate OS windows
 * (which was causing the fullscreen stage to drop out and show the desktop).
 *
 * How it works:
 * - The App class uses a StackPane as the root of a single Scene.
 * - When a dialog is needed, a semi-transparent overlay is added ON TOP of the
 *   current content inside that StackPane.
 * - The overlay blocks interaction with the content below (acts as modal).
 * - When the user clicks OK/Yes/No, the overlay is removed.
 * - Since no separate Window is created, fullscreen is NEVER interrupted.
 *
 * API CHANGE: showConfirmation() now takes a Consumer<Boolean> callback instead
 * of returning boolean. This is necessary because overlay dialogs are asynchronous
 * (they don't block the FX thread like Alert.showAndWait() did).
 *
 * Update your controllers from:
 *   if (UIUtils.showConfirmation("Title", "Msg")) { ... }
 * to:
 *   UIUtils.showConfirmation("Title", "Msg", confirmed -> { if (confirmed) { ... } });
 */
public class UIUtils {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static final Pattern LETTERS_ONLY =
        Pattern.compile("^[\\p{L}\\s\\-\\']+$");

    private static final Pattern NUMBERS_ONLY =
        Pattern.compile("^[0-9\\+\\s\\-\\(\\)]+$");

    // Lesotho country codes - Lesotho (+266) first as default
    public static final String[] COUNTRY_CODES = {
        "\uD83C\uDDF1\uD83C\uDDF8 +266", "\uD83C\uDDFF\uD83C\uDDE6 +27", "\uD83C\uDDFF\uD83C\uDDFC +263", "\uD83C\uDDFF\uD83C\uDDF2 +260", "\uD83C\uDDE7\uD83C\uDDFC +267",
        "\uD83C\uDDF3\uD83C\uDDE6 +264", "\uD83C\uDDE8\uD83C\uDDFB +258", "\uD83C\uDDF8\uD83C\uDDFF +268", "\uD83C\uDDF0\uD83C\uDDEA +254", "\uD83C\uDDE8\uD83C\uDDF3 +234",
        "\uD83C\uDDEC\uD83C\uDDED +233", "\uD83C\uDDFA\uD83C\uDDE6 +256", "\uD83C\uDDF9\uD83C\uDDFF +255", "\uD83C\uDDFA\uD83C\uDDF8 +1",  "\uD83C\uDDEC\uD83C\uDDE7 +44",
        "\uD83C\uDDEE\uD83C\uDDF3 +91",  "\uD83C\uDDE8\uD83C\uDDF3 +86"
    };
    public static final String DEFAULT_COUNTRY_CODE = "\uD83C\uDDF1\uD83C\uDDF8 +266";

    // =========================================================================
    // OVERLAY DIALOG SYSTEM
    // =========================================================================

    /**
     * Shows an info overlay dialog. No fullscreen glitch because it renders
     * inside the scene, not as a separate window.
     */
    public static void showInfo(String title, String message) {
        showOverlayDialog(title, message, DialogType.INFO, null);
    }

    /**
     * Shows an error overlay dialog.
     */
    public static void showError(String title, String message) {
        showOverlayDialog(title, message, DialogType.ERROR, null);
    }

    /**
     * Shows a warning overlay dialog.
     */
    public static void showWarning(String title, String message) {
        showOverlayDialog(title, message, DialogType.WARNING, null);
    }

    /**
     * Shows a confirmation overlay dialog with Yes/No buttons.
     *
     * API CHANGE: Now uses a callback instead of returning boolean.
     * Old: if (UIUtils.showConfirmation("Title", "Msg")) { ... }
     * New: UIUtils.showConfirmation("Title", "Msg", confirmed -> { if (confirmed) { ... } });
     */
    public static void showConfirmation(String title, String message, Consumer<Boolean> callback) {
        showOverlayDialog(title, message, DialogType.CONFIRMATION, callback);
    }

    /**
     * Backward-compatible overload that shows confirmation without callback.
     * Useful for simple "Are you sure?" prompts where you just need the dialog.
     */
    public static void showConfirmation(String title, String message) {
        showOverlayDialog(title, message, DialogType.CONFIRMATION, null);
    }

    private enum DialogType { INFO, ERROR, WARNING, CONFIRMATION }

    /**
     * Core overlay dialog implementation.
     * Adds a semi-transparent overlay + styled dialog box ON TOP of the current
     * scene content inside the StackPane root container.
     */
    private static void showOverlayDialog(String title, String message,
                                          DialogType type, Consumer<Boolean> callback) {
        StackPane rootContainer = App.getRootContainer();
        if (rootContainer == null) return;

        // --- Semi-transparent background overlay ---
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.65);");
        overlay.setAlignment(Pos.CENTER);
        overlay.prefWidthProperty().bind(rootContainer.widthProperty());
        overlay.prefHeightProperty().bind(rootContainer.heightProperty());

        // --- Dialog box ---
        VBox dialogBox = new VBox(12);
        dialogBox.setAlignment(Pos.CENTER_LEFT);
        dialogBox.setStyle(
            "-fx-background-color: #2c3e50;" +
            "-fx-padding: 20 28;" +
            "-fx-background-radius: 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0.3, 0, 6);"
        );
        dialogBox.setMaxWidth(520);
        dialogBox.setMinWidth(380);
        dialogBox.setMaxHeight(160);

        // Accent color by type
        String accentColor;
        String iconText;
        switch (type) {
            case ERROR:
                accentColor = "#e74c3c";
                iconText = "\u26A0";  // warning triangle
                break;
            case WARNING:
                accentColor = "#f39c12";
                iconText = "\u26A0";
                break;
            case CONFIRMATION:
                accentColor = "#3498db";
                iconText = "?";
                break;
            default:
                accentColor = "#2ecc71";
                iconText = "\u2713";  // checkmark
                break;
        }

        // Title label
        Label titleLabel = new Label(iconText + "  " + title);
        titleLabel.setStyle(
            "-fx-text-fill: " + accentColor + ";" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-family: 'Segoe UI';"
        );

        // Message label
        Label messageLabel = new Label(message);
        messageLabel.setStyle(
            "-fx-text-fill: #ecf0f1;" +
            "-fx-font-size: 13px;" +
            "-fx-font-family: 'Segoe UI';"
        );
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(480);

        // Separator line
        Region separator = new Region();
        separator.setStyle("-fx-background-color: #34495e; -fx-pref-height: 1;");
        separator.setMaxWidth(Double.MAX_VALUE);

        if (type == DialogType.CONFIRMATION) {
            Button yesBtn = new Button("  Yes  ");
            yesBtn.setStyle(
                "-fx-background-color: #e74c3c;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 8 24;" +
                "-fx-background-radius: 6;"
            );
            Button noBtn = new Button("  No  ");
            noBtn.setStyle(
                "-fx-background-color: #3498db;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 8 24;" +
                "-fx-background-radius: 6;"
            );

            HBox btnBox = new HBox(15, yesBtn, noBtn);
            btnBox.setAlignment(Pos.CENTER);

            dialogBox.getChildren().addAll(titleLabel, separator, messageLabel, btnBox);
            overlay.getChildren().add(dialogBox);

            // Add overlay to the root StackPane (on top of current content)
            rootContainer.getChildren().add(overlay);

            // Button handlers — remove overlay and invoke callback
            yesBtn.setOnAction(e -> {
                rootContainer.getChildren().remove(overlay);
                if (callback != null) callback.accept(true);
            });
            noBtn.setOnAction(e -> {
                rootContainer.getChildren().remove(overlay);
                if (callback != null) callback.accept(false);
            });
        } else {
            Button okBtn = new Button("  OK  ");
            okBtn.setStyle(
                "-fx-background-color: " + accentColor + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 8 30;" +
                "-fx-background-radius: 6;"
            );

            dialogBox.getChildren().addAll(titleLabel, separator, messageLabel, okBtn);
            overlay.getChildren().add(dialogBox);

            // Add overlay to the root StackPane (on top of current content)
            rootContainer.getChildren().add(overlay);

            okBtn.setOnAction(e -> {
                rootContainer.getChildren().remove(overlay);
            });
        }

        // Fade-in animation for the overlay
        overlay.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), overlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    /**
     * @deprecated Use the overlay-based dialogs above instead.
     * This method is kept for reference but should NOT be called.
     * The old Alert-based approach caused fullscreen to drop.
     */
    @Deprecated
    public static void restoreFullScreen() {
        // No longer needed — overlay dialogs don't break fullscreen.
        // The fullscreen property listener in App.java handles any edge cases.
    }

    // =========================================================================
    // VALIDATION UTILITIES (unchanged)
    // =========================================================================

    public static boolean isValidInput(String input) {
        return input != null && !input.trim().isEmpty();
    }

    public static boolean isValidInteger(String input) {
        if (!isValidInput(input)) return false;
        try { Integer.parseInt(input.trim()); return true; }
        catch (NumberFormatException e) { return false; }
    }

    public static boolean isValidDecimal(String input) {
        if (!isValidInput(input)) return false;
        try { Double.parseDouble(input.trim()); return true; }
        catch (NumberFormatException e) { return false; }
    }

    /** Validates email */
    public static boolean isValidEmail(String email) {
        if (!isValidInput(email)) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /** Letters, spaces, hyphens, apostrophes only */
    public static boolean isLettersOnly(String input) {
        if (!isValidInput(input)) return false;
        return LETTERS_ONLY.matcher(input.trim()).matches();
    }

    public static boolean isValidPhone(String input) {
        if (!isValidInput(input)) return false;
        return NUMBERS_ONLY.matcher(input.trim()).matches();
    }

    public static int safeParseInt(String input, int defaultValue) {
        try { return Integer.parseInt(input.trim()); }
        catch (NumberFormatException | NullPointerException e) { return defaultValue; }
    }

    public static double safeParseDouble(String input, double defaultValue) {
        try { return Double.parseDouble(input.trim()); }
        catch (NumberFormatException | NullPointerException e) { return defaultValue; }
    }

    /** Restricts a TextField to letters and spaces only */
    public static void restrictToLetters(javafx.scene.control.TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty() && !newVal.matches("[\\p{L}\\s\\-\\']*")) {
                field.setText(oldVal);
            }
        });
    }

    /** Restricts a TextField to digits, +, -, spaces, brackets only */
    public static void restrictToPhone(javafx.scene.control.TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty() && !newVal.matches("[0-9+\\s\\-()]*")) {
                field.setText(oldVal);
            }
        });
    }

    /** Restricts a TextField to digits only */
    public static void restrictToDigits(javafx.scene.control.TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty() && !newVal.matches("[0-9]*")) {
                field.setText(oldVal);
            }
        });
    }

    /** Extract the +XX part from a country code combo value like "LS +266" */
    public static String extractDialCode(String comboValue) {
        if (comboValue == null) return "+266";
        return comboValue.replaceAll(".*?(\\+\\d+).*", "$1");
    }

    /** Build full phone: dial code + number (strips leading zero from local number) */
    public static String buildFullPhone(String comboValue, String localNumber) {
        String code = extractDialCode(comboValue);
        String number = localNumber == null ? "" : localNumber.trim();
        if (number.isEmpty()) return "";
        if (number.startsWith("0")) number = number.substring(1);
        return code + number;
    }

    public static void centerOnScreen(Stage stage) {
        stage.centerOnScreen();
    }

    // =========================================================================
    // ROLE-BASED ACCESS GUARD
    // =========================================================================

    /**
     * Access rules for each module:
     *   Admin     → all modules
     *   Police    → Vehicles, Police Reports, Violations
     *   Workshop  → Vehicles, Service Records
     *   Insurance → Vehicles, Insurance, Customers
     *   Customer  → Vehicles, Queries
     *
     * Returns true if the current user has access to the given module.
     * Module names: "vehicles", "customers", "services", "police", "violations", "insurance", "queries", "users"
     */
    public static boolean hasAccess(String moduleName) {
        User user = App.getCurrentUser();
        if (user == null) return false;
        String role = user.getRole() != null ? user.getRole().toLowerCase() : "";
        String module = moduleName.toLowerCase();

        // Admin sees everything
        if ("admin".equals(role)) return true;

        return switch (module) {
            case "vehicles"   -> true;  // all roles can view vehicles
            case "customers"  -> "insurance".equals(role);
            case "services"   -> "workshop".equals(role);
            case "police"     -> "police".equals(role);
            case "violations" -> "police".equals(role);
            case "insurance"  -> "insurance".equals(role);
            case "queries"    -> "customer".equals(role);
            case "users"      -> false; // admin only — handled above
            default -> false;
        };
    }

    /**
     * Checks access and redirects to dashboard if denied.
     * Call this at the TOP of each controller's initialize() method.
     * Returns true if access is allowed, false if denied (already redirected).
     *
     * Usage:
     *   if (!UIUtils.checkAccess("police")) return;
     */
    public static boolean checkAccess(String moduleName) {
        if (hasAccess(moduleName)) return true;

        // Access denied — show message and redirect
        showWarning("Access Denied",
                "Your role does not have access to the " + moduleName + " module.\nRedirecting to Dashboard.");
        try {
            App.switchScene("/com/example/assignment/dashboard.fxml", "Dashboard");
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    // =========================================================================
    // NODE VISIBILITY HELPERS
    // =========================================================================

    /** Hide a node and remove it from layout flow */
    public static void hide(javafx.scene.Node node) {
        if (node == null) return;
        node.setVisible(false);
        node.setManaged(false);
    }

    /** Show a node and restore it to layout flow */
    public static void show(javafx.scene.Node node) {
        if (node == null) return;
        node.setVisible(true);
        node.setManaged(true);
    }

    /** Returns the current user's role in lowercase, or "" if none */
    public static String currentRole() {
        User u = App.getCurrentUser();
        if (u == null || u.getRole() == null) return "";
        return u.getRole().toLowerCase();
    }

    /** Returns true if current user is admin */
    public static boolean isAdmin() { return "admin".equals(currentRole()); }
    /** Returns true if current user is police */
    public static boolean isPolice() { return "police".equals(currentRole()); }
    /** Returns true if current user is workshop */
    public static boolean isWorkshop() { return "workshop".equals(currentRole()); }
    /** Returns true if current user is insurance */
    public static boolean isInsurance() { return "insurance".equals(currentRole()); }
    /** Returns true if current user is customer */
    public static boolean isCustomer() { return "customer".equals(currentRole()); }

    /**
     * Returns true if the current user has write access to a module.
     * View-only roles (police/workshop/insurance/customer on vehicles;
     * insurance on customers) return false.
     */
    public static boolean canWrite(String moduleName) {
        String role = currentRole();
        if ("admin".equals(role)) return true;
        return switch (moduleName.toLowerCase()) {
            case "vehicles"   -> false; // only admin can write vehicles
            case "customers"  -> false; // only admin can write customers
            case "services"   -> "workshop".equals(role);
            case "police"     -> "police".equals(role);
            case "violations" -> "police".equals(role);
            case "insurance"  -> "insurance".equals(role);
            case "queries"    -> "customer".equals(role) || "admin".equals(role);
            case "users"      -> false; // admin only
            default -> false;
        };
    }
}
