package com.example.assignment;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * FIX SUMMARY for fullscreen glitch:
 *
 * ROOT CAUSE 1: Calling primaryStage.setScene() causes JavaFX to momentarily
 *   exit fullscreen mode, creating the "minimise to right then fullscreen" glitch.
 *   FIX: Use a SINGLE Scene with a StackPane root container. Instead of swapping
 *   scenes, we swap the CONTENT inside the StackPane. This never triggers the
 *   fullscreen exit because the Scene object stays the same.
 *
 * ROOT CAUSE 2: Standard Alert dialogs create a separate OS-level window,
 *   which causes JavaFX to drop fullscreen entirely (the app "disappears").
 *   FIX: UIUtils now uses overlay-based dialogs that render INSIDE the scene,
 *   on top of the current content. No separate window = no fullscreen drop.
 *
 * ADDITIONAL FIX: A fullScreenProperty() listener that force-restores fullscreen
 *   if anything (FileChooser, etc.) causes it to drop. Belt-and-suspenders approach.
 */
public class App extends Application {

    private static Stage primaryStage;
    private static User currentUser;
    private static boolean isExiting = false;
    private static boolean isSceneSwitching = false;

    // KEY FIX: Single scene + root container for content swapping
    private static StackPane rootContainer;
    private static Scene mainScene;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // KEY FIX: Create ONE scene that will be reused for the entire app lifetime.
        // We never call setScene() again — instead we swap the content inside rootContainer.
        rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: #0F1923;");
        mainScene = new Scene(rootContainer);

        // Load CSS once (persists across all content swaps)
        String css = getClass().getResource("/com/example/assignment/style.css").toExternalForm();
        if (css != null) {
            mainScene.getStylesheets().add(css);
        }

        stage.setScene(mainScene);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        // KEY FIX: Force fullscreen to never drop.
        // If anything (FileChooser, accidental Escape, etc.) causes fullscreen to exit,
        // this listener immediately restores it on the next frame.
        stage.fullScreenProperty().addListener((obs, wasFull, isFull) -> {
            if (!isFull && !isExiting) {
                Platform.runLater(() -> {
                    if (!isExiting) {
                        stage.setFullScreen(true);
                        stage.toFront();
                    }
                });
            }
        });

        // KEY FIX: Must call show() — without it the stage is invisible!
        stage.show();

        showSplashThenLogin();

        // Test database connection in background
        new Thread(() -> {
            try {
                boolean ok = DatabaseConnection.getInstance().testConnection();
                System.out.println("Database: " + (ok ? "OK" : "FAILED"));
            } catch (Exception e) {
                System.err.println("DB error: " + e.getMessage());
            }
        }).start();
    }

    public void performExit() {
        if (isExiting) return;
        isExiting = true;
        Platform.runLater(() -> {
            try {
                if (DatabaseConnection.getInstance() != null) {
                    DatabaseConnection.getInstance().closeConnection();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Platform.exit();
                System.exit(0);
            }
        });
    }

    private void showSplashThenLogin() {
        StackPane splash = new StackPane();
        splash.setStyle("-fx-background-color: #0F1923;");

        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER);

        ImageView logoView = new ImageView();
        logoView.setFitWidth(140);
        logoView.setFitHeight(140);
        logoView.setPreserveRatio(true);
        java.net.URL logoUrl = getClass().getResource("/com/example/assignment/images/logo/logo.png");
        if (logoUrl == null) logoUrl = getClass().getResource("/com/example/assignment/images/logo/logo.jpg");
        if (logoUrl != null) logoView.setImage(new Image(logoUrl.toExternalForm()));

        Label title = new Label("VIS");
        title.setStyle("-fx-font-size: 64px; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-family: 'Segoe UI';");

        Label sub = new Label("VEHICLE IDENTIFICATION SYSTEM");
        sub.setStyle("-fx-font-size: 14px; -fx-text-fill: #4A90D9; -fx-letter-spacing: 4; -fx-font-family: 'Segoe UI';");

        Label country = new Label("Kingdom of Lesotho");
        country.setStyle("-fx-font-size: 12px; -fx-text-fill: #667788; -fx-font-family: 'Segoe UI';");

        Label loading = new Label("Loading...");
        loading.setStyle("-fx-font-size: 13px; -fx-text-fill: #445566; -fx-font-family: 'Segoe UI';");

        if (logoUrl != null) content.getChildren().add(logoView);
        content.getChildren().addAll(title, sub, country, loading);
        splash.getChildren().add(content);

        // KEY FIX: Add splash to rootContainer instead of creating a new Scene
        rootContainer.getChildren().setAll(splash);

        primaryStage.setTitle("VIS — Vehicle Identification System | Lesotho");

        splash.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(700), splash);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(600), splash);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.millis(1500));
        fadeOut.setOnFinished(e -> {
            try {
                loadLoginScene();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        fadeIn.play();
        fadeOut.play();
    }

    private void loadLoginScene() throws Exception {
        isSceneSwitching = true;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/assignment/login.fxml"));
            Parent root = loader.load();

            root.setOpacity(0);

            // KEY FIX: Bind content size to container so it fills the fullscreen area
            // Parent doesn't have prefWidthProperty — cast to Region (all FXML roots are Regions)
            if (root instanceof Region) {
                ((Region) root).prefWidthProperty().bind(rootContainer.widthProperty());
                ((Region) root).prefHeightProperty().bind(rootContainer.heightProperty());
            }

            primaryStage.setTitle("VIS — Login");

            // KEY FIX: Swap content inside the existing Scene — NO setScene() call!
            rootContainer.getChildren().setAll(root);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(600), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.setOnFinished(e -> {
                isSceneSwitching = false;
            });
            fadeIn.play();

        } catch (Exception e) {
            isSceneSwitching = false;
            throw e;
        }
    }

    /**
     * KEY FIX: Switch scene by swapping content inside the existing Scene.
     * This method NO LONGER creates a new Scene object.
     * Since primaryStage.setScene() is never called, fullscreen is never interrupted.
     */
    public static void switchScene(String fxmlPath, String title) throws Exception {
        isSceneSwitching = true;
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlPath));
            Parent root = loader.load();

            root.setOpacity(0);

            // Bind content size so it fills the fullscreen area
            // Parent doesn't have prefWidthProperty — cast to Region (all FXML roots are Regions)
            if (root instanceof Region) {
                ((Region) root).prefWidthProperty().bind(rootContainer.widthProperty());
                ((Region) root).prefHeightProperty().bind(rootContainer.heightProperty());
            }

            primaryStage.setTitle("VIS — " + title);

            // KEY FIX: Swap content in the existing Scene — NO setScene() call!
            rootContainer.getChildren().setAll(root);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(350), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.setOnFinished(e -> {
                isSceneSwitching = false;
            });
            fadeIn.play();

        } catch (Exception e) {
            isSceneSwitching = false;
            throw e;
        }
    }

    /**
     * Returns the root StackPane container.
     * Used by UIUtils to add overlay dialogs on top of the current content.
     */
    public static StackPane getRootContainer() {
        return rootContainer;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void logout() throws Exception {
        currentUser = null;
        switchScene("/com/example/assignment/login.fxml", "Login");
    }

    @Override
    public void stop() throws Exception {
        if (!isExiting) {
            DatabaseConnection.getInstance().closeConnection();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
