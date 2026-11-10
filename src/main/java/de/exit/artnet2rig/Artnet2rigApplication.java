package de.exit.artnet2rig;

import de.exit.artnet2rig.ui.canvas.Stage3DCanvas;
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
import javafx.stage.Stage;

import java.io.InputStream;

/**
 * Main JavaFX Application lifecycle entry point for artnet2rig.
 */
public class Artnet2rigApplication extends Application {

    private Stage3DCanvas stage3DCanvas;

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

        // 3D Visualizer Canvas in Center
        this.stage3DCanvas = new Stage3DCanvas();
        root.setCenter(stage3DCanvas);

        // Top Header Bar with Navigation & Presets
        HBox header = createHeader();
        root.setTop(header);

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/de/exit/artnet2rig/styles/dark-theme.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private HBox createHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 16, 8, 16));
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

        // Camera Perspective Presets
        HBox presets = new HBox(6);
        presets.setAlignment(Pos.CENTER_LEFT);

        Button btn3d = new Button("3D Orbit");
        btn3d.setOnAction(e -> stage3DCanvas.getCameraController().resetView());

        Button btnTop = new Button("Top");
        btnTop.setOnAction(e -> stage3DCanvas.getCameraController().setTopView());

        Button btnFront = new Button("Front");
        btnFront.setOnAction(e -> stage3DCanvas.getCameraController().setFrontView());

        Button btnSide = new Button("Side");
        btnSide.setOnAction(e -> stage3DCanvas.getCameraController().setSideView());

        presets.getChildren().addAll(btn3d, btnTop, btnFront, btnSide);

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

        header.getChildren().addAll(brand, presets, spacer, telemetry);
        return header;
    }
}
