package de.exit.artnet2view.ui.canvas;

import de.exit.artnet2view.core.model.Transform3D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;

import java.util.function.Consumer;

/**
 * Interactive 3D Transformation Gizmo with colored axis arrows (X=Red, Y=Green, Z=Blue)
 * enabling precise axis-constrained translation, inspired by JDMX.
 */
public class TransformGizmo3D extends Group {

    private static final Color COLOR_X = Color.web("#EF4444"); // Signal Red
    private static final Color COLOR_X_HOVER = Color.web("#F87171");

    private static final Color COLOR_Y = Color.web("#22C55E"); // Signal Green
    private static final Color COLOR_Y_HOVER = Color.web("#4ADE80");

    private static final Color COLOR_Z = Color.web("#3B82F6"); // Signal Blue
    private static final Color COLOR_Z_HOVER = Color.web("#60A5FA");

    private static final double SHAFT_RADIUS = 0.015;
    private static final double SHAFT_LENGTH = 0.50;
    private static final double HEAD_BASE_RADIUS = 0.045;
    private static final double HEAD_LENGTH = 0.14;

    private final Consumer<Transform3D> onTransformChanged;
    private Transform3D currentTransform = Transform3D.ZERO;

    private double dragAnchorSceneX;
    private double dragAnchorSceneY;
    private double startX;
    private double startY;
    private double startZ;

    public TransformGizmo3D(Consumer<Transform3D> onTransformChanged) {
        this.onTransformChanged = onTransformChanged;

        Node xArrow = createArrow(COLOR_X, COLOR_X_HOVER, Axis.X);
        Node yArrow = createArrow(COLOR_Y, COLOR_Y_HOVER, Axis.Y);
        Node zArrow = createArrow(COLOR_Z, COLOR_Z_HOVER, Axis.Z);

        getChildren().addAll(xArrow, yArrow, zArrow);
        setVisible(false);
    }

    public void setTransform(Transform3D transform) {
        this.currentTransform = transform;
    }

    private enum Axis {
        X, Y, Z
    }

    private Node createArrow(Color baseColor, Color hoverColor, Axis axis) {
        Group arrowGroup = new Group();

        PhongMaterial mat = new PhongMaterial(baseColor);
        mat.setSpecularColor(Color.WHITE);

        // Cylinder Shaft
        Cylinder shaft = new Cylinder(SHAFT_RADIUS, SHAFT_LENGTH);
        shaft.setMaterial(mat);

        // Arrow Cone Tip
        MeshView tip = createCone(HEAD_BASE_RADIUS, HEAD_LENGTH, mat);

        if (axis == Axis.X) {
            // Point along +X axis (rotate 90 deg around Z)
            arrowGroup.getTransforms().add(new Rotate(-90, Rotate.Z_AXIS));
            shaft.setTranslateY(SHAFT_LENGTH / 2.0);
            tip.setTranslateY(SHAFT_LENGTH);
        } else if (axis == Axis.Y) {
            // Point along -Y axis (upwards in stage coords)
            arrowGroup.getTransforms().add(new Rotate(180, Rotate.Z_AXIS));
            shaft.setTranslateY(SHAFT_LENGTH / 2.0);
            tip.setTranslateY(SHAFT_LENGTH);
        } else {
            // Point along +Z axis (rotate 90 deg around X)
            arrowGroup.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
            shaft.setTranslateY(SHAFT_LENGTH / 2.0);
            tip.setTranslateY(SHAFT_LENGTH);
        }

        arrowGroup.getChildren().addAll(shaft, tip);
        arrowGroup.setCursor(Cursor.HAND);

        // Hover feedback
        arrowGroup.setOnMouseEntered(e -> {
            mat.setDiffuseColor(hoverColor);
            e.consume();
        });
        arrowGroup.setOnMouseExited(e -> {
            mat.setDiffuseColor(baseColor);
            e.consume();
        });

        // Mouse Drag translation
        arrowGroup.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragAnchorSceneX = e.getSceneX();
                dragAnchorSceneY = e.getSceneY();
                startX = currentTransform.x();
                startY = currentTransform.y();
                startZ = currentTransform.z();
                e.consume();
            }
        });

        arrowGroup.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                double deltaScreenX = e.getSceneX() - dragAnchorSceneX;
                double deltaScreenY = e.getSceneY() - dragAnchorSceneY;
                double speed = 0.015;

                double newX = startX;
                double newY = startY;
                double newZ = startZ;

                if (axis == Axis.X) {
                    newX = startX + (deltaScreenX * speed);
                } else if (axis == Axis.Y) {
                    newY = startY + (deltaScreenY * speed);
                } else {
                    newZ = startZ + ((deltaScreenX - deltaScreenY) * 0.5 * speed);
                }

                currentTransform = currentTransform.withPosition(newX, newY, newZ);
                if (onTransformChanged != null) {
                    onTransformChanged.accept(currentTransform);
                }
                e.consume();
            }
        });

        return arrowGroup;
    }

    private MeshView createCone(double baseRadius, double length, PhongMaterial material) {
        int segments = 16;
        TriangleMesh mesh = new TriangleMesh();

        // Points: 0 is apex (Y = length), 1..segments are base ring at Y = 0
        float[] points = new float[(segments + 2) * 3];
        points[0] = 0;
        points[1] = (float) length;
        points[2] = 0;

        // Base center at index segments + 1
        points[(segments + 1) * 3] = 0;
        points[(segments + 1) * 3 + 1] = 0;
        points[(segments + 1) * 3 + 2] = 0;

        for (int i = 0; i < segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            points[(i + 1) * 3] = (float) (Math.cos(angle) * baseRadius);
            points[(i + 1) * 3 + 1] = 0;
            points[(i + 1) * 3 + 2] = (float) (Math.sin(angle) * baseRadius);
        }

        float[] texCoords = new float[]{0, 0, 1, 0, 0, 1, 1, 1};

        // Faces: Side triangles (Apex -> i -> i+1) and Base cap
        int[] faces = new int[segments * 2 * 6];
        int f = 0;
        for (int i = 0; i < segments; i++) {
            int p1 = i + 1;
            int p2 = ((i + 1) % segments) + 1;

            // Side: 0 -> p1 -> p2
            faces[f++] = 0;
            faces[f++] = 0;
            faces[f++] = p1;
            faces[f++] = 2;
            faces[f++] = p2;
            faces[f++] = 1;

            // Base cap: center (segments+1) -> p2 -> p1
            faces[f++] = segments + 1;
            faces[f++] = 0;
            faces[f++] = p2;
            faces[f++] = 1;
            faces[f++] = p1;
            faces[f++] = 2;
        }

        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(texCoords);
        mesh.getFaces().setAll(faces);

        MeshView meshView = new MeshView(mesh);
        meshView.setMaterial(material);
        meshView.setCullFace(CullFace.NONE);
        return meshView;
    }
}
