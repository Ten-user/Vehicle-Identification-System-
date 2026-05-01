package com.example.assignment;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Utility class for UI-related helper methods.
 * Provides alert dialogs, formatting, and common UI operations.
 */
public class UIUtils {

    /**
     * Shows an information alert dialog
     */
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows an error alert dialog
     */
    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows a warning alert dialog
     */
    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows a confirmation dialog and returns the user's choice
     */
    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Validates that a string is not null or empty
     */
    public static boolean isValidInput(String input) {
        return input != null && !input.trim().isEmpty();
    }

    /**
     * Validates that a string can be parsed as an integer
     */
    public static boolean isValidInteger(String input) {
        if (!isValidInput(input)) return false;
        try {
            Integer.parseInt(input.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates that a string can be parsed as a decimal
     */
    public static boolean isValidDecimal(String input) {
        if (!isValidInput(input)) return false;
        try {
            Double.parseDouble(input.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Safely parses an integer, returning default value on failure
     */
    public static int safeParseInt(String input, int defaultValue) {
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }

    /**
     * Safely parses a double, returning default value on failure
     */
    public static double safeParseDouble(String input, double defaultValue) {
        try {
            return Double.parseDouble(input.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }

    /**
     * Centers a stage on screen
     */
    public static void centerOnScreen(Stage stage) {
        stage.centerOnScreen();
    }
}
