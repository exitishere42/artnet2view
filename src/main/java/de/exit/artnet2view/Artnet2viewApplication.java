package de.exit.artnet2view;

import de.exit.artnet2view.core.artnet.DmxUniverseBuffer;
import de.exit.artnet2view.core.model.*;
import de.exit.artnet2view.service.ArtNetService;
import de.exit.artnet2view.ui.canvas.Stage3DCanvas;
import de.exit.artnet2view.ui.theme.LucideIcon;
import de.exit.artnet2view.ui.theme.ThemeConstants;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.io.InputStream;

/**
 * Main JavaFX Application lifecycle entry point for artnet2view.
 */
public class Artnet2viewApplication extends Application {

    private Stage3DCanvas stage3DCanvas;
    private ArtNetService artNetService;
    private AnimationTimer renderLoop;
    private boolean demoModeActive = false;
    private double demoTick = 0.0;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("artnet2view - 3D Lighting Rig & Stage Visualizer");
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(600);

        try (InputStream iconStream = getClass().getResourceAsStream("/icons/artnet2view.png")) {
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception ignored) {
        }

        // Start Core Art-Net UDP Service
        DmxUniverseBuffer buffer = new DmxUniverseBuffer();
        this.artNetService = new ArtNetService(buffer, ArtNetService.DEFAULT_PORT);
        this.artNetService.start();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + ThemeConstants.BG_BASE + ";");

        // 3D Visualizer Canvas in Center
        this.stage3DCanvas = new Stage3DCanvas();
        root.setCenter(stage3DCanvas);

        // Load Default Stage Setup
        loadDefaultStage();

        // Top Header Bar
        HBox header = createHeader();
        root.setTop(header);

        // Real-time 60 FPS Render Loop
        this.renderLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (demoModeActive) {
                    runDemoFrame();
                }
                stage3DCanvas.renderDmxFrame(artNetService.getBuffer());
            }
        };
        this.renderLoop.start();

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/de/exit/artnet2view/styles/dark-theme.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            renderLoop.stop();
            artNetService.stop();
        });
        primaryStage.show();
    }

    private void loadDefaultStage() {
        stage3DCanvas.clearStage();

        // 1. Overhead Truss (3x 2m = 6m width at Y=-3.5m)
        stage3DCanvas.addTruss(new TrussElement(1L, TrussType.STRAIGHT_2M, new Transform3D(-2.0, -3.5, 0, 0, 0, 0)));
        stage3DCanvas.addTruss(new TrussElement(2L, TrussType.STRAIGHT_2M, new Transform3D(0.0, -3.5, 0, 0, 0, 0)));
        stage3DCanvas.addTruss(new TrussElement(3L, TrussType.STRAIGHT_2M, new Transform3D(2.0, -3.5, 0, 0, 0, 0)));

        // 2. Hanging Moving Head Spots along the truss
        double[] spotX = new double[]{-2.5, -0.8, 0.8, 2.5};
        for (int i = 0; i < spotX.length; i++) {
            Fixture spot = new Fixture(
                    (long) (10 + i),
                    "Spot " + (i + 1),
                    FixtureType.MOVING_HEAD_SPOT,
                    0,
                    1 + (i * 16),
                    16,
                    new Transform3D(spotX[i], -3.5, 0, 0, 0, 0),
                    false,
                    false,
                    15.0
            );
            stage3DCanvas.addFixture(spot);
        }

        // 3. Floor LED Pars
        Fixture parLeft = new Fixture(
                20L,
                "Par Left",
                FixtureType.LED_PAR,
                0,
                65,
                8,
                new Transform3D(-4.0, -0.2, -1.0, -45.0, 30.0, 0),
                false,
                false,
                25.0
        );
        Fixture parRight = new Fixture(
                21L,
                "Par Right",
                FixtureType.LED_PAR,
                0,
                73,
                8,
                new Transform3D(4.0, -0.2, -1.0, -45.0, -30.0, 0),
                false,
                false,
                25.0
        );
        stage3DCanvas.addFixture(parLeft);
        stage3DCanvas.addFixture(parRight);
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
                new Label("artnet2view") {{
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

        // Demo Mode Toggle Button
        ToggleButton demoBtn = new ToggleButton("Demo Mode");
        demoBtn.setOnAction(e -> {
            demoModeActive = demoBtn.isSelected();
            if (!demoModeActive) {
                artNetService.getBuffer().clear();
            }
        });

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
        artNetService.fpsProperty().addListener((obs, oldVal, newVal) ->
                telemetryText.setText(String.format("UDP: %d • %.0f FPS", artNetService.getPort(), newVal.doubleValue()))
        );
        telemetry.getChildren().addAll(statusDot, telemetryText);

        header.getChildren().addAll(brand, presets, demoBtn, spacer, telemetry);
        return header;
    }

    private void runDemoFrame() {
        demoTick += 0.03;
        DmxUniverseBuffer buf = artNetService.getBuffer();

        // 4 Moving Heads wave
        for (int i = 0; i < 4; i++) {
            int start = 1 + (i * 16);
            double panPhase = demoTick + (i * 0.8);
            double tiltPhase = demoTick * 1.5 + (i * 0.4);

            int pan = (int) (128 + 90 * Math.sin(panPhase));
            int tilt = (int) (128 + 60 * Math.cos(tiltPhase));

            // Pan Coarse & Tilt Coarse
            buf.setChannel(0, start, pan);
            buf.setChannel(0, start + 2, tilt);

            // Dimmer
            buf.setChannel(0, start + 5, 255);

            // RGB color cycling
            double colorPhase = demoTick + (i * 1.2);
            int r = (int) (127 + 127 * Math.sin(colorPhase));
            int g = (int) (127 + 127 * Math.sin(colorPhase + 2.0));
            int b = (int) (127 + 127 * Math.sin(colorPhase + 4.0));

            buf.setChannel(0, start + 7, r);
            buf.setChannel(0, start + 8, g);
            buf.setChannel(0, start + 9, b);
        }

        // 2 Pars pulsing amber/cyan
        buf.setChannel(0, 65, 200); // Dimmer
        buf.setChannel(0, 67, 255); // Red
        buf.setChannel(0, 68, 150); // Green
        buf.setChannel(0, 69, 50);  // Blue

        buf.setChannel(0, 73, 200); // Dimmer
        buf.setChannel(0, 75, 50);  // Red
        buf.setChannel(0, 76, 220); // Green
        buf.setChannel(0, 77, 255); // Blue
    }
}
