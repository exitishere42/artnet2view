package de.exit.artnet2rig.ui.canvas;

import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * High-precision 3D Orbit Camera Controller for the JavaFX 3D stage canvas.
 * Supports smooth mouse orbiting, panning, scroll zooming, and perspective presets.
 */
public class OrbitCameraController {

    private final PerspectiveCamera camera;

    // Transform hierarchy for the camera
    private final Translate pivotTranslate = new Translate(0, 0, 0);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate rotateX = new Rotate(-25, Rotate.X_AXIS);
    private final Translate cameraDistance = new Translate(0, 0, -15);

    private double mouseAnchorX;
    private double mouseAnchorY;

    // Default reset parameters
    private static final double DEFAULT_DISTANCE = 15.0;
    private static final double DEFAULT_PITCH = -25.0;
    private static final double DEFAULT_YAW = 0.0;

    public OrbitCameraController(PerspectiveCamera camera) {
        this.camera = camera;
        this.camera.setNearClip(0.1);
        this.camera.setFarClip(1000.0);
        this.camera.setFieldOfView(45.0);

        // Apply transform chain in order: Pivot -> Yaw (Y) -> Pitch (X) -> Distance (Z)
        this.camera.getTransforms().addAll(pivotTranslate, rotateY, rotateX, cameraDistance);
    }

    /**
     * Attaches mouse event listeners to the given interactive canvas node (e.g., SubScene).
     *
     * @param target the node capturing input
     */
    public void attachListeners(Node target) {
        target.setOnMousePressed(this::handleMousePressed);
        target.setOnMouseDragged(this::handleMouseDragged);
        target.setOnScroll(this::handleScroll);
    }

    private void handleMousePressed(MouseEvent e) {
        mouseAnchorX = e.getSceneX();
        mouseAnchorY = e.getSceneY();
    }

    private void handleMouseDragged(MouseEvent e) {
        double deltaX = e.getSceneX() - mouseAnchorX;
        double deltaY = e.getSceneY() - mouseAnchorY;
        mouseAnchorX = e.getSceneX();
        mouseAnchorY = e.getSceneY();

        if (e.getButton() == MouseButton.PRIMARY || e.getButton() == MouseButton.SECONDARY) {
            if (e.isShiftDown() || e.getButton() == MouseButton.MIDDLE) {
                // Pan
                double panSpeed = 0.02 * (Math.abs(cameraDistance.getZ()) / 15.0);
                pivotTranslate.setX(pivotTranslate.getX() - deltaX * panSpeed);
                pivotTranslate.setY(pivotTranslate.getY() - deltaY * panSpeed);
            } else {
                // Orbit
                double orbitSpeed = 0.3;
                rotateY.setAngle(rotateY.getAngle() + deltaX * orbitSpeed);
                double newPitch = rotateX.getAngle() - deltaY * orbitSpeed;
                // Clamp pitch between -89 and 89 degrees to avoid gimbal lock flip
                if (newPitch > -89.0 && newPitch < 89.0) {
                    rotateX.setAngle(newPitch);
                }
            }
        }
    }

    private void handleScroll(ScrollEvent e) {
        double zoomFactor = 1.1;
        double currentZ = cameraDistance.getZ();
        if (e.getDeltaY() > 0) {
            // Zoom in
            cameraDistance.setZ(Math.min(-1.0, currentZ / zoomFactor));
        } else {
            // Zoom out
            cameraDistance.setZ(Math.max(-200.0, currentZ * zoomFactor));
        }
    }

    /**
     * Resets camera back to default isometric 3D perspective view.
     */
    public void resetView() {
        pivotTranslate.setX(0);
        pivotTranslate.setY(0);
        pivotTranslate.setZ(0);
        rotateY.setAngle(DEFAULT_YAW);
        rotateX.setAngle(DEFAULT_PITCH);
        cameraDistance.setZ(-DEFAULT_DISTANCE);
    }

    public void setTopView() {
        pivotTranslate.setX(0);
        pivotTranslate.setY(0);
        pivotTranslate.setZ(0);
        rotateY.setAngle(0);
        rotateX.setAngle(-89.0);
    }

    public void setFrontView() {
        pivotTranslate.setX(0);
        pivotTranslate.setY(0);
        pivotTranslate.setZ(0);
        rotateY.setAngle(0);
        rotateX.setAngle(0);
    }

    public void setSideView() {
        pivotTranslate.setX(0);
        pivotTranslate.setY(0);
        pivotTranslate.setZ(0);
        rotateY.setAngle(90.0);
        rotateX.setAngle(0);
    }

    public PerspectiveCamera getCamera() {
        return camera;
    }
}
