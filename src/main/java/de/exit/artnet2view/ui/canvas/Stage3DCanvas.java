package de.exit.artnet2view.ui.canvas;

import de.exit.artnet2view.core.artnet.DmxUniverseBuffer;
import de.exit.artnet2view.core.model.Fixture;
import de.exit.artnet2view.core.model.TrussElement;
import de.exit.artnet2view.ui.theme.ThemeConstants;
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
import java.util.function.Consumer;

/**
 * Responsive 3D Stage Visualizer Canvas container.
 * Hosts the JavaFX 3D {@link SubScene}, Orbit Camera, Stage Floor, Lighting Rig, and Touchpad Quick Controls.
 */
public class Stage3DCanvas extends Pane {

    private final SubScene subScene;
    private final Group root3D;
    private final Group trussGroup;
    private final Group fixtureGroup;
    private final OrbitCameraController cameraController;

    private final List<Fixture3DNode> fixtureNodes = new ArrayList<>();
    private Consumer<Fixture> selectionListener;

    public Stage3DCanvas() {
        this.root3D = new Group();
        this.trussGroup = new Group();
        this.fixtureGroup = new Group();

        PerspectiveCamera camera = new PerspectiveCamera(true);
        this.cameraController = new OrbitCameraController(camera);

        this.subScene = new SubScene(root3D, 800, 600, true, SceneAntialiasing.BALANCED);
        this.subScene.setFill(Color.web("#0D0D0D"));
        this.subScene.setCamera(camera);

        cameraController.attachListeners(subScene);

        setupLighting();
        setupStageFloor();
        setupCoordinateAxes();

        root3D.getChildren().addAll(trussGroup, fixtureGroup);
        getChildren().add(subScene);

        // Touchpad Quick Control Floating Dock (Bottom Left)
        HBox touchpadDock = createTouchpadDock();
        getChildren().add(touchpadDock);

        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());

        // Keep Touchpad Dock positioned at bottom left
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
        AmbientLight ambient = new AmbientLight(Color.rgb(80, 80, 80));

        PointLight pointLight = new PointLight(Color.WHITE);
        pointLight.setTranslateX(0);
        pointLight.setTranslateY(-10);
        pointLight.setTranslateZ(-5);

        root3D.getChildren().addAll(ambient, pointLight);
    }

    private void setupStageFloor() {
        Box floor = new Box(12.0, 0.2, 8.0);
        floor.setTranslateY(0.1);

        PhongMaterial floorMat = new PhongMaterial();
        floorMat.setDiffuseColor(Color.web("#1E1E1E"));
        floorMat.setSpecularColor(Color.web("#333333"));
        floor.setMaterial(floorMat);

        root3D.getChildren().add(floor);

        PhongMaterial gridMat = new PhongMaterial(Color.web("#2A2A2A"));
        Group gridGroup = new Group();

        for (double x = -6.0; x <= 6.0; x += 1.0) {
            Box line = new Box(0.02, 0.01, 8.0);
            line.setTranslateX(x);
            line.setTranslateY(-0.01);
            line.setMaterial(gridMat);
            gridGroup.getChildren().add(line);
        }

        for (double z = -4.0; z <= 4.0; z += 1.0) {
            Box line = new Box(12.0, 0.01, 0.02);
            line.setTranslateZ(z);
            line.setTranslateY(-0.01);
            line.setMaterial(gridMat);
            gridGroup.getChildren().add(line);
        }

        root3D.getChildren().add(gridGroup);
    }

    private void setupCoordinateAxes() {
        Group axes = new Group();

        Cylinder axisX = new Cylinder(0.02, 1.0);
        axisX.setMaterial(new PhongMaterial(Color.web("#EF5350")));
        axisX.setRotate(90);
        axisX.setTranslateX(0.5);
        axisX.setTranslateY(-0.05);

        Cylinder axisY = new Cylinder(0.02, 1.0);
        axisY.setMaterial(new PhongMaterial(Color.web("#66BB6A")));
        axisY.setTranslateY(-0.55);

        Cylinder axisZ = new Cylinder(0.02, 1.0);
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

    public void addTruss(TrussElement truss) {
        Group trussNode = MeshFactory.createTrussNode(truss);
        trussGroup.getChildren().add(trussNode);
    }

    public void addFixture(Fixture fixture) {
        Fixture3DNode node = new Fixture3DNode(fixture, f -> {
            if (selectionListener != null) {
                selectionListener.accept(f);
            }
        });
        fixtureNodes.add(node);
        fixtureGroup.getChildren().add(node);
    }

    public void updateSelection(Fixture selected) {
        for (Fixture3DNode node : fixtureNodes) {
            boolean isSelected = (selected != null && node.getFixture().id() != null &&
                    node.getFixture().id().equals(selected.id()));
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

        byte[][] universeCache = new byte[DmxUniverseBuffer.MAX_UNIVERSES][];

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
