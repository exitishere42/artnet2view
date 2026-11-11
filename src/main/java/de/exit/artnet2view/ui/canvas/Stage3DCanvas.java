package de.exit.artnet2view.ui.canvas;

import de.exit.artnet2view.core.artnet.DmxUniverseBuffer;
import de.exit.artnet2view.core.model.Fixture;
import de.exit.artnet2view.core.model.TrussElement;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Responsive 3D Stage Visualizer Canvas container.
 * Hosts the JavaFX 3D {@link SubScene}, Orbit Camera, batched Stage Floor/Grid, Lighting Rig, and Touchpad Quick Controls.
 */
public class Stage3DCanvas extends Pane {

    private final SubScene subScene;
    private final Group root3D;
    private final Group trussGroup;
    private final Group fixtureGroup;
    private final OrbitCameraController cameraController;

    private final List<Fixture3DNode> fixtureNodes = new ArrayList<>();
    private final byte[][] universeCache = new byte[DmxUniverseBuffer.MAX_UNIVERSES][];
    private Consumer<Fixture> selectionListener;
    private Consumer<Fixture> fixtureModifiedListener;

    public Stage3DCanvas() {
        this.root3D = new Group();
        this.trussGroup = new Group();
        this.fixtureGroup = new Group();

        PerspectiveCamera camera = new PerspectiveCamera(true);
        this.cameraController = new OrbitCameraController(camera);

        // Disable heavy 4x MSAA on Linux/Mesa iGPUs & HiDPI displays unless explicitly requested
        boolean forceMsaa = Boolean.parseBoolean(System.getProperty("artnet2view.msaa", "false"));
        boolean isLinux = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
        SceneAntialiasing antialiasing = (isLinux && !forceMsaa)
                ? SceneAntialiasing.DISABLED
                : SceneAntialiasing.BALANCED;

        this.subScene = new SubScene(root3D, 800, 600, true, antialiasing);
        this.subScene.setFill(Color.web("#0D0D0D"));
        this.subScene.setCamera(camera);

        cameraController.attachListeners(subScene);

        setupLighting();
        setupStageFloor();
        setupCoordinateAxes();

        root3D.getChildren().addAll(trussGroup, fixtureGroup);
        getChildren().add(subScene);

        HBox touchpadDock = createTouchpadDock();
        getChildren().add(touchpadDock);

        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());

        layoutBoundsProperty().addListener((obs, oldB, newB) -> {
            touchpadDock.setLayoutX(16);
            touchpadDock.setLayoutY(newB.getHeight() - 48);
        });
    }

    private HBox createTouchpadDock() {
        HBox dock = new HBox(6);
        dock.setAlignment(Pos.CENTER_LEFT);
        dock.setPadding(new Insets(4, 8, 4, 8));
        dock.setStyle("-fx-background-color: rgba(30, 30, 30, 0.75); -fx-background-radius: 4px; -fx-border-color: #333333; -fx-border-radius: 4px;");

        Button zoomInBtn = new Button("+");
        zoomInBtn.setPrefWidth(28);
        zoomInBtn.setOnAction(e -> cameraController.zoomIn());

        Button zoomOutBtn = new Button("-");
        zoomOutBtn.setPrefWidth(28);
        zoomOutBtn.setOnAction(e -> cameraController.zoomOut());

        ToggleButton panToggle = new ToggleButton("Pan-Modus");
        panToggle.setStyle("-fx-font-size: 11px;");
        panToggle.setOnAction(e -> cameraController.setForcePanMode(panToggle.isSelected()));

        Button resetBtn = new Button("Reset");
        resetBtn.setStyle("-fx-font-size: 11px;");
        resetBtn.setOnAction(e -> {
            cameraController.resetView();
            panToggle.setSelected(false);
            cameraController.setForcePanMode(false);
        });

        dock.getChildren().addAll(zoomInBtn, zoomOutBtn, panToggle, resetBtn);
        return dock;
    }

    private void setupLighting() {
        AmbientLight fixtureAmbient = new AmbientLight(Color.rgb(200, 200, 200));
        fixtureAmbient.getScope().add(fixtureGroup);

        AmbientLight stageAmbient = new AmbientLight(Color.rgb(90, 90, 90));

        PointLight pointLight = new PointLight(Color.rgb(180, 180, 180));
        pointLight.setTranslateX(0);
        pointLight.setTranslateY(-10);
        pointLight.setTranslateZ(-5);

        root3D.getChildren().addAll(fixtureAmbient, stageAmbient, pointLight);
    }

    private void setupStageFloor() {
        Box floor = new Box(12.0, 0.2, 8.0);
        floor.setTranslateY(0.1);

        PhongMaterial floorMat = new PhongMaterial();
        floorMat.setDiffuseColor(Color.web("#1E1E1E"));
        floorMat.setSpecularColor(Color.web("#333333"));
        floor.setMaterial(floorMat);

        root3D.getChildren().addAll(floor, MeshFactory.createStageGridMesh(12.0, 8.0, 1.0));
    }

    private void setupCoordinateAxes() {
        Group axes = new Group();

        Cylinder axisX = new Cylinder(0.02, 1.0, 8);
        axisX.setMaterial(new PhongMaterial(Color.web("#EF5350")));
        axisX.setRotate(90);
        axisX.setTranslateX(0.5);
        axisX.setTranslateY(-0.05);

        Cylinder axisY = new Cylinder(0.02, 1.0, 8);
        axisY.setMaterial(new PhongMaterial(Color.web("#66BB6A")));
        axisY.setTranslateY(-0.55);

        Cylinder axisZ = new Cylinder(0.02, 1.0, 8);
        axisZ.setMaterial(new PhongMaterial(Color.web("#42A5F5")));
        axisZ.setRotationAxis(javafx.scene.transform.Rotate.X_AXIS);
        axisZ.setRotate(90);
        axisZ.setTranslateZ(0.5);
        axisZ.setTranslateY(-0.05);

        axes.getChildren().addAll(axisX, axisY, axisZ);
        root3D.getChildren().add(axes);
    }

    public void setSelectionListener(Consumer<Fixture> selectionListener) {
        this.selectionListener = selectionListener;
    }

    public void setFixtureModifiedListener(Consumer<Fixture> fixtureModifiedListener) {
        this.fixtureModifiedListener = fixtureModifiedListener;
    }

    public void addTruss(TrussElement truss) {
        Group trussNode = MeshFactory.createTrussNode(truss);
        trussGroup.getChildren().add(trussNode);
    }

    public void addFixture(Fixture fixture) {
        Fixture3DNode node = new Fixture3DNode(
                fixture,
                f -> {
                    if (selectionListener != null) {
                        selectionListener.accept(f);
                    }
                },
                f -> {
                    if (fixtureModifiedListener != null) {
                        fixtureModifiedListener.accept(f);
                    }
                }
        );
        fixtureNodes.add(node);
        fixtureGroup.getChildren().add(node);
    }

    /**
     * Updates an existing 3D fixture node's transform and properties in-place without rebuilding the stage.
     *
     * @param updated updated fixture data
     */
    public void updateFixture(Fixture updated) {
        if (updated == null || updated.id() == null) {
            return;
        }
        for (Fixture3DNode node : fixtureNodes) {
            if (Objects.equals(node.getFixture().id(), updated.id())) {
                node.setFixtureAndTransform(updated);
                break;
            }
        }
    }

    public void updateSelection(Fixture selected) {
        for (Fixture3DNode node : fixtureNodes) {
            boolean isSelected = (selected != null && node.getFixture().id() != null
                    && node.getFixture().id().equals(selected.id()));
            node.setSelected(isSelected);
        }
    }

    public void clearStage() {
        trussGroup.getChildren().clear();
        fixtureGroup.getChildren().clear();
        fixtureNodes.clear();
    }

    public void renderDmxFrame(DmxUniverseBuffer buffer) {
        if (buffer == null) {
            return;
        }

        for (int i = 0; i < universeCache.length; i++) {
            universeCache[i] = null;
        }

        for (Fixture3DNode node : fixtureNodes) {
            int u = node.getFixture().universe();
            if (u >= 0 && u < DmxUniverseBuffer.MAX_UNIVERSES) {
                if (universeCache[u] == null) {
                    universeCache[u] = buffer.getUniverse(u);
                }
                node.applyDmx(universeCache[u]);
            }
        }
    }

    public OrbitCameraController getCameraController() {
        return cameraController;
    }

    public List<Fixture3DNode> getFixtureNodes() {
        return fixtureNodes;
    }
}
