package de.exit.artnet2view.ui.canvas;

import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * Touchpad-optimized 3D Orbit Camera Controller.
 * Fully supports laptop touchpad gestures: 2-finger smooth scrolling, pinch-to-zoom,
 * single-finger orbiting, and multi-modifier panning (Shift / Ctrl / Alt / Right Click).
 */
public class OrbitCameraController {

    private final PerspectiveCamera camera;

    // Transform chain: Pivot (Pan) -> Yaw (Y) -> Pitch (X) -> Distance (Zoom)
    private final Translate pivotTranslate = new Translate(0, 0, 0);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate rotateX = new Rotate(-25, Rotate.X_AXIS);
    private final Translate cameraDistance = new Translate(0, 0, -15);

    private double mouseAnchorX;
    private double mouseAnchorY;

    // Touchpad Pan Mode Toggle
    private boolean forcePanMode = false;

    private static final double DEFAULT_DISTANCE = 15.0;
    private static final double DEFAULT_PITCH = -25.0;
    private static final double DEFAULT_YAW = 0.0;

    public OrbitCameraController(PerspectiveCamera camera) {
        this.camera = camera;
        this.camera.setNearClip(0.1);
        this.camera.setFarClip(1000.0);
        this.camera.setFieldOfView(45.0);

        this.camera.getTransforms().addAll(pivotTranslate, rotateY, rotateX, cameraDistance);
    }

    /**
     * Attaches mouse, touchpad scroll, and pinch-to-zoom gesture handlers to the canvas.
     */
    public void attachListeners(Node target) {
        target.setOnMousePressed(this::handleMousePressed);
        target.setOnMouseDragged(this::handleMouseDragged);
        target.setOnScroll(this::handleScroll);
        target.setOnZoom(this::handleZoom);
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

        boolean isPan = forcePanMode ||
                e.getButton() == MouseButton.SECONDARY ||
                e.getButton() == MouseButton.MIDDLE ||
                e.isShiftDown() ||
                e.isControlDown() ||
                e.isAltDown();

        if (isPan) {
            // Touchpad Pan: smooth translation proportional to camera distance
            double currentDist = Math.abs(cameraDistance.getZ());
            double panSpeed = 0.015 * (currentDist / 15.0);
            pivotTranslate.setX(pivotTranslate.getX() - (deltaX * panSpeed));
            pivotTranslate.setY(pivotTranslate.getY() - (deltaY * panSpeed));
        } else {
            // Touchpad Orbit: smooth rotation
            double orbitSpeed = 0.35;
            rotateY.setAngle(rotateY.getAngle() + deltaX * orbitSpeed);
            double newPitch = rotateX.getAngle() - deltaY * orbitSpeed;
            if (newPitch > -89.0 && newPitch < 89.0) {
                rotateX.setAngle(newPitch);
            }
        }
    }

    /**
     * Damped, linear touchpad 2-finger scroll zoom preventing wild jumping.
     */
    private void handleScroll(ScrollEvent e) {
        double delta = e.getDeltaY();
        if (Math.abs(delta) > 0.001) {
            double currentDist = Math.abs(cameraDistance.getZ());
            double zoomFactor = 0.025 * (currentDist / 15.0);
            double newZ = cameraDistance.getZ() + (delta * zoomFactor);
            // Clamp distance between 1m and 150m
            cameraDistance.setZ(Math.max(-150.0, Math.min(-1.0, newZ)));
        }
    }

    /**
     * Native touchpad Pinch-to-Zoom gesture.
     */
    private void handleZoom(ZoomEvent e) {
        double factor = e.getZoomFactor();
        if (factor > 0.0) {
            double newZ = cameraDistance.getZ() / factor;
            cameraDistance.setZ(Math.max(-150.0, Math.min(-1.0, newZ)));
        }
        e.consume();
    }

    public void zoomIn() {
        double current = cameraDistance.getZ();
        cameraDistance.setZ(Math.min(-1.0, current * 0.85));
    }

    public void zoomOut() {
        double current = cameraDistance.getZ();
        cameraDistance.setZ(Math.max(-150.0, current * 1.15));
    }

    public boolean isForcePanMode() {
        return forcePanMode;
    }

    public void setForcePanMode(boolean forcePanMode) {
        this.forcePanMode = forcePanMode;
    }

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
