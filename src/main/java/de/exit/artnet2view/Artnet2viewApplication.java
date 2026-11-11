package de.exit.artnet2view;

import de.exit.artnet2view.core.artnet.DmxUniverseBuffer;
import de.exit.artnet2view.core.model.*;
import de.exit.artnet2view.service.A2vProjectService;
import de.exit.artnet2view.service.ArtNetService;
import de.exit.artnet2view.service.DatabaseService;
import de.exit.artnet2view.service.RigRepository;
import de.exit.artnet2view.service.RigService;
import de.exit.artnet2view.ui.canvas.Stage3DCanvas;
import de.exit.artnet2view.ui.components.FixtureInspector;
import de.exit.artnet2view.ui.dialog.AboutUpdateDialog;
import de.exit.artnet2view.ui.menu.BrandActionMenu;
import de.exit.artnet2view.ui.theme.LucideIcon;
import de.exit.artnet2view.ui.theme.ThemeConstants;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main application entry point for the ArtNet2View 3D Lighting Visualizer.
 * Connects UDP Art-Net receiving, H2 local database, .a2v project file system,
 * 3D SubScene canvas, and interactive fixture inspector.
 */
public class Artnet2viewApplication extends Application {

    private DatabaseService databaseService;
    private RigRepository rigRepository;
    private RigService rigService;
    private ArtNetService artNetService;
    private A2vProjectService a2vProjectService;

    private Stage3DCanvas stage3DCanvas;
    private FixtureInspector inspector;
    private AnimationTimer renderLoop;
    private Stage primaryStage;
    private StackPane overlayPane;
    private File currentA2vFile;

    private boolean demoModeActive = false;
    private double demoTick = 0;
    private final AtomicLong nextEntityId = new AtomicLong(100);

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("artnet2view - 3D Lighting Stage Visualizer");

        // Initialize Services
        this.databaseService = new DatabaseService();
        this.rigRepository = new RigRepository(databaseService);
        this.rigService = new RigService(rigRepository);
        this.a2vProjectService = new A2vProjectService();
        this.artNetService = new ArtNetService(new DmxUniverseBuffer(), 6454);
        this.artNetService.start();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + ThemeConstants.BG_BASE + ";");

        // Center 3D Visualizer Canvas
        this.stage3DCanvas = new Stage3DCanvas();
        root.setCenter(stage3DCanvas);

        // Right Inspector Dock
        this.inspector = new FixtureInspector(rigService, stage3DCanvas::updateFixture, this::reloadStageFromProject);
        root.setRight(inspector);

        // Wire 3D Selection with Service
        stage3DCanvas.setSelectionListener(rigService::setSelectedFixture);
        rigService.selectedFixtureProperty().addListener((obs, oldF, newF) -> stage3DCanvas.updateSelection(newF));

        // Wire 3D Gizmo Arrow translation directly to rigService & inspector in-place (no UI rebuild)
        stage3DCanvas.setFixtureModifiedListener(modifiedFix -> {
            rigService.updateFixture(modifiedFix, modifiedFix);
            inspector.syncFromModel(modifiedFix);
        });

        // Load Default Stage Setup
        initDefaultStage();

        // Top Header Bar with Brand Action Menu
        HBox header = createHeader();
        root.setTop(header);

        // Real-time ~60 FPS Render Loop (rate-limited for Linux/prism.vsync=false smoothness)
        this.renderLoop = new AnimationTimer() {
            private long lastFrameNanos = 0L;

            @Override
            public void handle(long now) {
                if (now - lastFrameNanos < 15_500_000L) {
                    return;
                }
                lastFrameNanos = now;
                if (demoModeActive) {
                    runDemoFrame();
                }
                stage3DCanvas.renderDmxFrame(artNetService.getBuffer());
            }
        };
        this.renderLoop.start();

        overlayPane = new StackPane();
        overlayPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.65);");
        overlayPane.setAlignment(Pos.CENTER);
        overlayPane.setVisible(false);
        overlayPane.setManaged(false);
        overlayPane.setOnMouseClicked(e -> {
            if (e.getTarget() == overlayPane) {
                hideOverlay();
            }
        });

        StackPane appRoot = new StackPane(root, overlayPane);
        Scene scene = new Scene(appRoot, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/de/exit/artnet2view/styles/dark-theme.css").toExternalForm());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE && overlayPane.isVisible()) {
                hideOverlay();
                e.consume();
            }
        });

        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> shutdown());
        primaryStage.show();
    }

    private void initDefaultStage() {
        RigProject project = rigService.getActiveProject();
        project.clearAll();

        // 1. Overhead Truss (3x 2m = 6m width at Y=-3.5m)
        project.addTruss(new TrussElement(1L, TrussType.STRAIGHT_2M, new Transform3D(-2.0, -3.5, 0, 0, 0, 0)));
        project.addTruss(new TrussElement(2L, TrussType.STRAIGHT_2M, new Transform3D(0.0, -3.5, 0, 0, 0, 0)));
        project.addTruss(new TrussElement(3L, TrussType.STRAIGHT_2M, new Transform3D(2.0, -3.5, 0, 0, 0, 0)));

        // 2. Hanging U'King ZQ02040 Moving Head Spots (11 Channels each)
        double[] spotX = new double[]{-2.5, -0.8, 0.8, 2.5};
        for (int i = 0; i < spotX.length; i++) {
            Fixture spot = new Fixture(
                    (long) (10 + i),
                    "ZQ02040 Spot " + (i + 1),
                    FixtureType.MOVING_HEAD_ZQ02040,
                    0,
                    1 + (i * 11),
                    11,
                    new Transform3D(spotX[i], -3.5, 0, 0, 0, 0),
                    false,
                    false,
                    6.0
            );
            project.addFixture(spot);
        }

        // 3. Floor LED Pars
        project.addFixture(new Fixture(
                20L,
                "Par Left",
                FixtureType.LED_PAR,
                0,
                45,
                8,
                new Transform3D(-4.0, -0.2, -1.0, -45.0, 30.0, 0),
                false,
                false,
                25.0
        ));
        project.addFixture(new Fixture(
                21L,
                "Par Right",
                FixtureType.LED_PAR,
                0,
                53,
                8,
                new Transform3D(4.0, -0.2, -1.0, -45.0, -30.0, 0),
                false,
                false,
                25.0
        ));

        reloadStageFromProject();
    }

    private void reloadStageFromProject() {
        stage3DCanvas.clearStage();
        RigProject project = rigService.getActiveProject();

        for (TrussElement truss : project.getTrusses()) {
            stage3DCanvas.addTruss(truss);
        }
        for (Fixture fix : project.getFixtures()) {
            stage3DCanvas.addFixture(fix);
        }

        stage3DCanvas.updateSelection(rigService.getSelectedFixture());
    }

    private HBox createHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 16, 8, 16));
        header.setStyle("-fx-background-color: " + ThemeConstants.BG_CARD + "; -fx-border-color: " + ThemeConstants.BORDER_COLOR + "; -fx-border-width: 0 0 1 0;");

        // Brand & Title Button with Dropdown Menu (matching JDMX)
        Button brandBtn = new Button("artnet2view");
        brandBtn.setGraphic(LucideIcon.create(LucideIcon.BOX, 18, "#38BDF8"));
        brandBtn.getStyleClass().add("brand-button");

        BrandActionMenu brandMenu = new BrandActionMenu();
        brandBtn.setOnAction(e -> {
            if (brandMenu.isShowing()) {
                brandMenu.hide();
            } else {
                javafx.geometry.Point2D p = brandBtn.localToScreen(0, brandBtn.getHeight() + 4);
                if (p != null) {
                    brandMenu.show(brandBtn, p.getX(), p.getY());
                }
            }
        });

        brandMenu.setAction("save", this::handleSaveProject);
        brandMenu.setAction("save_as", this::handleSaveAsProject);
        brandMenu.setAction("check_updates", () -> AboutUpdateDialog.show(this::showOverlay, this::hideOverlay));
        brandMenu.setAction("fullscreen", () -> {
            if (primaryStage != null) {
                primaryStage.setFullScreen(!primaryStage.isFullScreen());
            }
        });
        brandMenu.setAction("exit", () -> {
            shutdown();
            Platform.exit();
            System.exit(0);
        });

        // View Presets
        HBox presets = new HBox(4);
        presets.setAlignment(Pos.CENTER_LEFT);

        Button btn3d = new Button("3D");
        btn3d.setOnAction(e -> stage3DCanvas.getCameraController().resetView());

        Button btnTop = new Button("Top");
        btnTop.setOnAction(e -> stage3DCanvas.getCameraController().setTopView());

        Button btnFront = new Button("Front");
        btnFront.setOnAction(e -> stage3DCanvas.getCameraController().setFrontView());

        Button btnSide = new Button("Side");
        btnSide.setOnAction(e -> stage3DCanvas.getCameraController().setSideView());

        presets.getChildren().addAll(btn3d, btnTop, btnFront, btnSide);

        // Add Elements Buttons
        HBox addTools = new HBox(6);
        addTools.setAlignment(Pos.CENTER_LEFT);

        Button addSpotBtn = new Button("+ ZQ02040");
        addSpotBtn.setOnAction(e -> {
            long id = nextEntityId.incrementAndGet();
            Fixture spot = new Fixture(
                    id,
                    "ZQ02040 " + id,
                    FixtureType.MOVING_HEAD_ZQ02040,
                    0,
                    1,
                    11,
                    new Transform3D(0, -3.5, 0, 0, 0, 0),
                    false,
                    false,
                    6.0
            );
            rigService.addFixture(spot);
            reloadStageFromProject();
        });

        Button addParBtn = new Button("+ Par");
        addParBtn.setOnAction(e -> {
            long id = nextEntityId.incrementAndGet();
            Fixture par = new Fixture(
                    id,
                    "Par " + id,
                    FixtureType.LED_PAR,
                    0,
                    1,
                    8,
                    new Transform3D(0, -0.2, 0, 0, 0, 0),
                    false,
                    false,
                    25.0
            );
            rigService.addFixture(par);
            reloadStageFromProject();
        });

        Button addTrussBtn = new Button("+ Traverse");
        addTrussBtn.setOnAction(e -> {
            long id = nextEntityId.incrementAndGet();
            TrussElement truss = new TrussElement(
                    id,
                    TrussType.STRAIGHT_2M,
                    new Transform3D(0, -3.5, 0, 0, 0, 0)
            );
            rigService.addTruss(truss);
            reloadStageFromProject();
        });

        addTools.getChildren().addAll(addSpotBtn, addParBtn, addTrussBtn);

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

        header.getChildren().addAll(
                brandBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                presets,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                addTools,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                demoBtn,
                spacer,
                telemetry
        );
        return header;
    }

    private void showOverlay(Node content) {
        if (overlayPane == null || content == null) {
            return;
        }
        overlayPane.getChildren().setAll(content);
        overlayPane.setOpacity(0);
        overlayPane.setManaged(true);
        overlayPane.setVisible(true);

        FadeTransition fade = new FadeTransition(Duration.millis(140), overlayPane);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void hideOverlay() {
        if (overlayPane == null) {
            return;
        }
        overlayPane.setVisible(false);
        overlayPane.setManaged(false);
        overlayPane.getChildren().clear();
    }

    private void shutdown() {
        if (renderLoop != null) {
            renderLoop.stop();
        }
        if (artNetService != null) {
            artNetService.stop();
        }
        if (databaseService != null) {
            databaseService.close();
        }
    }

    private void handleSaveProject() {
        if (currentA2vFile == null) {
            handleSaveAsProject();
        } else {
            try {
                a2vProjectService.saveToFile(rigService.getActiveProject(), currentA2vFile);
                rigService.saveActiveProject();
            } catch (Exception ex) {
                showError("Fehler beim Speichern", "Das Projekt konnte nicht gespeichert werden:\n" + ex.getMessage());
            }
        }
    }

    private void handleSaveAsProject() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Projekt speichern unter... (*.a2v)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(A2vProjectService.FILE_DESCRIPTION, "*" + A2vProjectService.FILE_EXTENSION));
        fc.setInitialFileName("meine_buehne.a2v");

        File file = fc.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                a2vProjectService.saveToFile(rigService.getActiveProject(), file);
                rigService.saveActiveProject();
                this.currentA2vFile = file;
            } catch (Exception ex) {
                showError("Fehler beim Speichern", "Das Projekt konnte nicht gespeichert werden:\n" + ex.getMessage());
            }
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void runDemoFrame() {
        demoTick += 0.03;
        DmxUniverseBuffer buf = artNetService.getBuffer();

        // 4x U'King ZQ02040 Moving Heads (11 Channels: Pan, PanFine, Tilt, TiltFine, Speed, Dimmer, Strobe, R, G, B, W)
        for (int i = 0; i < 4; i++) {
            int start = 1 + (i * 11);
            double panPhase = demoTick + (i * 0.8);
            double tiltPhase = demoTick * 1.5 + (i * 0.4);

            int pan = (int) (128 + 90 * Math.sin(panPhase));
            int tilt = (int) (128 + 60 * Math.cos(tiltPhase));

            buf.setChannel(0, start, pan);          // CH 1: Pan
            buf.setChannel(0, start + 1, 0);        // CH 2: Pan Fine
            buf.setChannel(0, start + 2, tilt);     // CH 3: Tilt
            buf.setChannel(0, start + 3, 0);        // CH 4: Tilt Fine
            buf.setChannel(0, start + 4, 0);        // CH 5: Speed
            buf.setChannel(0, start + 5, 255);      // CH 6: Dimmer
            buf.setChannel(0, start + 6, 0);        // CH 7: Strobe

            double colorPhase = demoTick + (i * 1.2);
            int r = (int) (127 + 127 * Math.sin(colorPhase));
            int g = (int) (127 + 127 * Math.sin(colorPhase + 2.0));
            int b = (int) (127 + 127 * Math.sin(colorPhase + 4.0));

            buf.setChannel(0, start + 7, r);        // CH 8: Red
            buf.setChannel(0, start + 8, g);        // CH 9: Green
            buf.setChannel(0, start + 9, b);        // CH 10: Blue
            buf.setChannel(0, start + 10, 0);       // CH 11: White
        }

        // Par Left (CH 45)
        buf.setChannel(0, 45, 200);
        buf.setChannel(0, 47, 255);
        buf.setChannel(0, 48, 150);
        buf.setChannel(0, 49, 50);

        // Par Right (CH 53)
        buf.setChannel(0, 53, 200);
        buf.setChannel(0, 55, 50);
        buf.setChannel(0, 56, 220);
        buf.setChannel(0, 57, 255);
    }
}
