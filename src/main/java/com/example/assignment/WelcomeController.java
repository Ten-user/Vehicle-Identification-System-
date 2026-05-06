package com.example.assignment;

import javafx.animation.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class WelcomeController implements Initializable {

    @FXML private HBox rootHBox;
    @FXML private VBox rightPanel;
    @FXML private Label taglineLabel;
    @FXML private Label modeLabel;
    @FXML private Button modeToggle;
    @FXML private Button getStartedBtn;
    @FXML private ImageView logoView;

    // The toggle only affects the RIGHT (white) panel — left panel is always system navy
    private boolean isDark = false;

    private static final List<String> TAGLINES = List.of(
        "Securing Lesotho's roads — one vehicle at a time.",
        "Connecting law enforcement, insurance and citizens.",
        "Real-time vehicle data across the Kingdom of Lesotho.",
        "Trusted by police, workshops and insurers nationwide."
    );
    private int taglineIndex = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Load logo if available
        try {
            java.net.URL logoUrl = getClass().getResource("/com/example/assignment/images/logo/logo.png");
            if (logoUrl == null) logoUrl = getClass().getResource("/com/example/assignment/images/logo/logo.jpg");
            if (logoUrl != null) logoView.setImage(new Image(logoUrl.toExternalForm()));
        } catch (Exception ignored) {}

        // Entrance animation — slide both panels in
        playEntrance();

        // Rotate taglines every 4 seconds
        rotateTaglines();
    }

    // ── ENTRANCE ─────────────────────────────────────────────────────────────
    private void playEntrance() {
        rootHBox.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(700), rootHBox);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    // ── ROTATING TAGLINE ─────────────────────────────────────────────────────
    private void rotateTaglines() {
        Timeline cycle = new Timeline(new KeyFrame(Duration.seconds(4), e -> {
            taglineIndex = (taglineIndex + 1) % TAGLINES.size();
            FadeTransition out = new FadeTransition(Duration.millis(300), taglineLabel);
            out.setFromValue(1); out.setToValue(0);
            out.setOnFinished(ev -> {
                taglineLabel.setText(TAGLINES.get(taglineIndex));
                FadeTransition in = new FadeTransition(Duration.millis(400), taglineLabel);
                in.setFromValue(0); in.setToValue(1);
                in.play();
            });
            out.play();
        }));
        cycle.setCycleCount(Animation.INDEFINITE);
        cycle.play();
    }

    // ── MODE TOGGLE ───────────────────────────────────────────────────────────
    @FXML
    private void handleModeToggle(ActionEvent event) {
        isDark = !isDark;

        // Spin animation on the toggle button
        RotateTransition spin = new RotateTransition(Duration.millis(350), modeToggle);
        spin.setByAngle(360);
        spin.play();

        if (isDark) {
            // Right panel goes dark (matches the left navy panel feel)
            rightPanel.setStyle(
                "-fx-background-color: #0F1923;"
            );
            modeLabel.setText("☀  Light mode");
            modeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #4A90D9; -fx-letter-spacing: 1;");
            modeToggle.setStyle(
                "-fx-background-color: rgba(74,144,217,0.12); -fx-background-radius: 20;" +
                "-fx-border-color: #4A90D9; -fx-border-radius: 20; -fx-border-width: 1;" +
                "-fx-text-fill: #4A90D9; -fx-font-size: 18px; -fx-padding: 6 12; -fx-cursor: hand;"
            );
            // Style the GET STARTED button for dark bg
            getStartedBtn.setStyle(
                "-fx-background-color: #1A5FC8; -fx-text-fill: white;" +
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8;" +
                "-fx-pref-height: 46; -fx-cursor: hand; -fx-letter-spacing: 1;"
            );
        } else {
            // Restore white panel
            rightPanel.setStyle("-fx-background-color: #FFFFFF;");
            modeLabel.setText("🌙  Dark mode");
            modeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #AABBCC; -fx-letter-spacing: 1;");
            modeToggle.setStyle(
                "-fx-background-color: #F0F4FF; -fx-background-radius: 20;" +
                "-fx-border-color: #C8D5F0; -fx-border-radius: 20; -fx-border-width: 1;" +
                "-fx-text-fill: #1A5FC8; -fx-font-size: 18px; -fx-padding: 6 12; -fx-cursor: hand;"
            );
            // Restore the CSS class style
            getStartedBtn.setStyle("");
        }
    }

    // ── GET STARTED ───────────────────────────────────────────────────────────
    @FXML
    private void handleGetStarted(ActionEvent event) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(350), rootHBox);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            try {
                App.switchScene("/com/example/assignment/login.fxml", "Login");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        fadeOut.play();
    }
}
