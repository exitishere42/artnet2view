package de.exit.artnet2rig;

import de.exit.artnet2rig.ui.theme.LucideIcon;
import de.exit.artnet2rig.ui.theme.ThemeConstants;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.InputStream;

/**
 * Main JavaFX Application lifecycle entry point for artnet2rig.
 */
public class Artnet2rigApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("artnet2rig - 3D Lighting Rig & Stage Visualizer");
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(600);

        try (InputStream iconStream = getClass().getResourceAsStream("/icons/artnet2rig.png")) {
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception ignored) {
        }

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + ThemeConstants.BG_BASE + ";");

        // Top Header Bar
        HBox header = createHeader();
        root.setTop(header);

        // Center 3D Stage Placeholder Area
        VBox centerArea = new VBox(12);
        centerArea.setAlignment(Pos.CENTER);
        centerArea.setStyle("-fx-background-color: " + ThemeConstants.BG_BASE + "; -fx-border-color: " + ThemeConstants.BORDER_COLOR + ";");
        
        Label titleLabel = new Label("3D Stage & Rig Viewport");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + ThemeConstants.PRIMARY_COLOR + ";");
        
        Label subLabel = new Label("Art-Net UDP Port 6454 • Ready for Rigging");
        subLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #9E9E9E;");

        centerArea.getChildren().addAll(LucideIcon.create(LucideIcon.BOX, 48, ThemeConstants.PRIMARY_COLOR), titleLabel, subLabel);
        root.setCenter(centerArea);

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/de/exit/artnet2rig/styles/dark-theme.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private HBox createHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 16, 10, 16));
        header.setStyle("-fx-background-color: " + ThemeConstants.BG_CARD + "; -fx-border-color: " + ThemeConstants.BORDER_COLOR + "; -fx-border-width: 0 0 1 0;");

        // Brand & Title
        HBox brand = new HBox(8);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getChildren().addAll(
                LucideIcon.create(LucideIcon.BOX, 20, ThemeConstants.PRIMARY_COLOR),
                new Label("artnet2rig") {{
                    setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: " + ThemeConstants.PRIMARY_COLOR + ";");
                }}
        );

        // Spacer
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Telemetry Pill
        HBox telemetry = new HBox(6);
        telemetry.setAlignment(Pos.CENTER);
        telemetry.setPadding(new Insets(4, 10, 4, 10));
        telemetry.setStyle("-fx-background-color: " + ThemeConstants.BG_BASE + "; -fx-border-color: " + ThemeConstants.BORDER_COLOR + "; -fx-border-radius: 4px;");
        Label statusDot = new Label("●");
        statusDot.setStyle("-fx-text-fill: " + ThemeConstants.STATUS_ONLINE + "; -fx-font-size: 10px;");
        Label telemetryText = new Label("UDP: 6454 • 0 FPS");
        telemetryText.setStyle("-fx-font-size: 11px; -fx-text-fill: #B0BEC5;");
        telemetry.getChildren().addAll(statusDot, telemetryText);

        // Action Buttons
        Button resetCamBtn = new Button("Kamera Reset");
        resetCamBtn.setGraphic(LucideIcon.create(LucideIcon.REFRESH_CW, 14, "#E0E0E0"));

        header.getChildren().addAll(brand, spacer, telemetry, resetCamBtn);
        return header;
    }
}
