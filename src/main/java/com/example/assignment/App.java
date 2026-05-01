package com.example.assignment;

import com.example.assignment.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

/**
 * Main Application class for the Vehicle Identification System.
 * Entry point for the JavaFX application.
 * Demonstrates MVC architecture - this is the application bootstrap.
 */
public class App extends Application {

    private static Stage primaryStage;
    private static com.example.assignment.User currentUser;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Load the login screen
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/assignment/login.fxml"));
        Parent root = loader.load();

        // Apply fade-in animation on startup
        root.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        Scene scene = new Scene(root);

        // Load CSS styles
        String css = getClass()
                .getResource("/com/example/assignment/style.css")
                .toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("Vehicle Identification System - Login");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.setMaximized(true);
        stage.show();

        // Test database connection on startup
        testDatabaseConnection();
    }

    /**
     * Tests the database connection and shows status
     */
    private void testDatabaseConnection() {
        try {
            boolean connected = DatabaseConnection.getInstance().testConnection();
            if (connected) {
                System.out.println("Database connection: OK");
            } else {
                System.out.println("Database connection: FAILED - Check your Neon credentials in DatabaseConfig.java");
            }
        } catch (Exception e) {
            System.err.println("Database connection error: " + e.getMessage());
        }
    }

    /**
     * Switches the current scene (used for navigation between views)
     */
    public static void switchScene(String fxmlPath, String title) throws Exception {
        FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlPath));
        Parent root = loader.load();

        // Fade transition
        root.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        Scene scene = new Scene(root);
        String css = App.class.getResource("/com/example/assignment/style.css").toExternalForm();
        if (css != null) scene.getStylesheets().add(css);

        primaryStage.setTitle("VIS - " + title);
        primaryStage.setScene(scene);
    }

    /**
     * Gets the primary stage for dialog owners
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Sets the currently logged-in user
     */
    public static void setCurrentUser(com.example.assignment.User user) {
        currentUser = user;
    }

    /**
     * Gets the currently logged-in user
     */
    public static com.example.assignment.User getCurrentUser() {
        return currentUser;
    }

    /**
     * Logs out the current user and returns to login screen
     */
    public static void logout() throws Exception {
        currentUser = null;
        switchScene("login.fxml", "Login");
    }

    @Override
    public void stop() throws Exception {
        // Clean up database connection on exit
        DatabaseConnection.getInstance().closeConnection();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
