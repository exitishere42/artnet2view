package de.exit.artnet2rig.ui.canvas;

import de.exit.artnet2rig.core.model.Fixture;
import de.exit.artnet2rig.core.model.TrussElement;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;

import java.util.List;

/**
 * Responsive 3D Stage Visualizer Canvas container.
 * Hosts the JavaFX 3D {@link SubScene}, Orbit Camera, Stage Floor, and Lighting Rig elements.
 */
public class Stage3DCanvas extends Pane {

    private final SubScene subScene;
    private final Group root3D;
    private final Group trussGroup;
    private final Group fixtureGroup;
    private final OrbitCameraController cameraController;

    public Stage3DCanvas() {
        this.root3D = new Group();
        this.trussGroup = new Group();
        this.fixtureGroup = new Group();

        // Setup 3D Scene Graph
        PerspectiveCamera camera = new PerspectiveCamera(true);
        this.cameraController = new OrbitCameraController(camera);

        this.subScene = new SubScene(root3D, 800, 600, true, SceneAntialiasing.BALANCED);
        this.subScene.setFill(Color.web("#0D0D0D"));
        this.subScene.setCamera(camera);

        cameraController.attachListeners(subScene);

        // Build Environment
        setupLighting();
        setupStageFloor();
        setupCoordinateAxes();

        root3D.getChildren().addAll(trussGroup, fixtureGroup);
        getChildren().add(subScene);

        // Responsive resizing: bind SubScene dimensions to container Pane
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());
    }

    private void setupLighting() {
        // Soft ambient illumination so shadow areas remain visible
        AmbientLight ambient = new AmbientLight(Color.rgb(80, 80, 80));

        // Overhead point light for subtle 3D depth and specular highlights
        PointLight pointLight = new PointLight(Color.WHITE);
        pointLight.setTranslateX(0);
        pointLight.setTranslateY(-10);
        pointLight.setTranslateZ(-5);

        root3D.getChildren().addAll(ambient, pointLight);
    }

    private void setupStageFloor() {
        // Main stage platform: 12m wide x 0.2m high x 8m deep
        Box floor = new Box(12.0, 0.2, 8.0);
        floor.setTranslateY(0.1); // Place top of floor exactly at Y=0

        PhongMaterial floorMat = new PhongMaterial();
        floorMat.setDiffuseColor(Color.web("#1E1E1E"));
        floorMat.setSpecularColor(Color.web("#333333"));
        floor.setMaterial(floorMat);

        root3D.getChildren().add(floor);

        // Grid lines across the floor (every 1 meter)
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

        // X Axis = Red (Width)
        Cylinder axisX = new Cylinder(0.02, 1.0);
        axisX.setMaterial(new PhongMaterial(Color.web("#EF5350")));
        axisX.setRotate(90);
        axisX.setTranslateX(0.5);
        axisX.setTranslateY(-0.05);

        // Y Axis = Green (Height / Up)
        Cylinder axisY = new Cylinder(0.02, 1.0);
        axisY.setMaterial(new PhongMaterial(Color.web("#66BB6A")));
        axisY.setTranslateY(-0.55);

        // Z Axis = Blue (Depth)
        Cylinder axisZ = new Cylinder(0.02, 1.0);
        axisZ.setMaterial(new PhongMaterial(Color.web("#42A5F5")));
        axisZ.setRotationAxis(javafx.scene.transform.Rotate.X_AXIS);
        axisZ.setRotate(90);
        axisZ.setTranslateZ(0.5);
        axisZ.setTranslateY(-0.05);

        axes.getChildren().addAll(axisX, axisY, axisZ);
        root3D.getChildren().add(axes);
    }

    public OrbitCameraController getCameraController() {
        return cameraController;
    }

    public Group getTrussGroup() {
        return trussGroup;
    }

    public Group getFixtureGroup() {
        return fixtureGroup;
    }
}
