package de.exit.artnet2view.ui.canvas;

import de.exit.artnet2view.core.dmx.ChannelMapping;
import de.exit.artnet2view.core.model.Fixture;
import de.exit.artnet2view.core.model.FixtureType;
import de.exit.artnet2view.core.model.Transform3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

import java.util.function.Consumer;

/**
 * Interactive 3D scene-graph representation of a lighting fixture.
 * Optimized with shared housing materials, reduced primitive tessellation,
 * and strict DMX dirty-checking to avoid redundant scene-graph invalidation.
 */
public class Fixture3DNode extends Group {

    private static final PhongMaterial HOUSING_MAT = new PhongMaterial(Color.web("#1E1E1E"));
    private static final PhongMaterial SELECTION_MAT = new PhongMaterial(Color.web("#00E676"));
    private static final Color LENS_OFF_COLOR = Color.web("#1A1A1A");

    static {
        HOUSING_MAT.setSpecularColor(Color.web("#424242"));
        SELECTION_MAT.setSpecularColor(Color.WHITE);
    }

    private Fixture fixture;
    private final Rotate yawRotate;
    private final Rotate pitchRotate;
    private final Rotate rollRotate;
    private final Rotate panRotate;
    private final Rotate tiltRotate;
    private final BeamNode beamNode;
    private final Cylinder lensNode;
    private final PhongMaterial lensMaterial;

    private final Cylinder selectionRing;
    private final TransformGizmo3D transformGizmo;

    // DMX dirty-check state
    private int lastPanCoarse = -1;
    private int lastPanFine = -1;
    private int lastTiltCoarse = -1;
    private int lastTiltFine = -1;
    private int lastDimmer = -1;
    private int lastStrobe = -1;
    private int lastR = -1;
    private int lastG = -1;
    private int lastB = -1;
    private int lastW = -1;
    private boolean lastPanInvert;
    private boolean lastTiltInvert;
    private boolean lastLensLit = false;

    public Fixture3DNode(Fixture fixture, Consumer<Fixture> onSelect) {
        this(fixture, onSelect, null);
    }

    public Fixture3DNode(Fixture fixture, Consumer<Fixture> onSelect, Consumer<Fixture> onTransformUpdated) {
        this.fixture = fixture;
        this.lastPanInvert = fixture.panInvert();
        this.lastTiltInvert = fixture.tiltInvert();

        setTranslateX(fixture.transform().x());
        setTranslateY(fixture.transform().y());
        setTranslateZ(fixture.transform().z());

        this.yawRotate = new Rotate(fixture.transform().yaw(), Rotate.Y_AXIS);
        this.pitchRotate = new Rotate(fixture.transform().pitch(), Rotate.X_AXIS);
        this.rollRotate = new Rotate(fixture.transform().roll(), Rotate.Z_AXIS);
        getTransforms().addAll(yawRotate, pitchRotate, rollRotate);

        this.panRotate = new Rotate(0, Rotate.Y_AXIS);
        this.tiltRotate = new Rotate(0, Rotate.X_AXIS);

        this.beamNode = new BeamNode(9.0, 0.06, 0.70);
        this.lensMaterial = new PhongMaterial(LENS_OFF_COLOR);
        this.lensMaterial.setSpecularColor(Color.TRANSPARENT);

        this.lensNode = new Cylinder(0.08, 0.02, 12);
        this.lensNode.setMaterial(lensMaterial);

        this.selectionRing = new Cylinder(0.24, 0.015, 16);
        this.selectionRing.setMaterial(SELECTION_MAT);
        this.selectionRing.setVisible(false);

        this.transformGizmo = new TransformGizmo3D(newTransform -> {
            this.fixture = this.fixture.withTransform(newTransform);
            setTranslateX(newTransform.x());
            setTranslateY(newTransform.y());
            setTranslateZ(newTransform.z());
            if (onTransformUpdated != null) {
                onTransformUpdated.accept(this.fixture);
            }
        });
        this.transformGizmo.setTransform(fixture.transform());

        buildGeometry();

        if (onSelect != null) {
            setOnMouseClicked(e -> {
                onSelect.accept(this.fixture);
                e.consume();
            });
        }
    }

    private void buildGeometry() {
        getChildren().addAll(selectionRing, transformGizmo);

        if (fixture.type().isMovingHead()) {
            Box base = new Box(0.32, 0.12, 0.32);
            base.setMaterial(HOUSING_MAT);
            base.setTranslateY(0.06);

            Group yokeGroup = new Group();
            yokeGroup.setTranslateY(0.12);
            yokeGroup.getTransforms().add(panRotate);

            Box yokeBottom = new Box(0.30, 0.06, 0.12);
            yokeBottom.setMaterial(HOUSING_MAT);
            yokeBottom.setTranslateY(0.03);

            Box yokeLeft = new Box(0.06, 0.26, 0.10);
            yokeLeft.setMaterial(HOUSING_MAT);
            yokeLeft.setTranslateX(-0.13);
            yokeLeft.setTranslateY(0.16);

            Box yokeRight = new Box(0.06, 0.26, 0.10);
            yokeRight.setMaterial(HOUSING_MAT);
            yokeRight.setTranslateX(0.13);
            yokeRight.setTranslateY(0.16);

            yokeGroup.getChildren().addAll(yokeBottom, yokeLeft, yokeRight);

            Group headGroup = new Group();
            headGroup.setTranslateY(0.24);
            headGroup.getTransforms().add(tiltRotate);

            Sphere headBody = new Sphere(0.13, 14);
            headBody.setMaterial(HOUSING_MAT);

            Cylinder snout = new Cylinder(0.10, 0.16, 12);
            snout.setMaterial(HOUSING_MAT);
            snout.setTranslateY(0.10);

            lensNode.setTranslateY(0.18);
            beamNode.setTranslateY(0.19);

            headGroup.getChildren().addAll(headBody, snout, lensNode, beamNode);
            yokeGroup.getChildren().add(headGroup);

            getChildren().addAll(base, yokeGroup);

        } else if (fixture.type() == FixtureType.LED_PAR) {
            Cylinder can = new Cylinder(0.12, 0.24, 12);
            can.setMaterial(HOUSING_MAT);
            can.setTranslateY(0.12);

            lensNode.setTranslateY(0.24);
            beamNode.setTranslateY(0.25);

            getChildren().addAll(can, lensNode, beamNode);

        } else {
            Box bar = new Box(1.0, 0.08, 0.08);
            bar.setMaterial(HOUSING_MAT);
            bar.setTranslateY(0.04);

            lensNode.setTranslateY(0.08);
            beamNode.setTranslateY(0.09);

            getChildren().addAll(bar, lensNode, beamNode);
        }
    }

    public void setSelected(boolean selected) {
        selectionRing.setVisible(selected);
        transformGizmo.setVisible(selected);
        if (selected) {
            transformGizmo.setTransform(fixture.transform());
        }
    }

    public void updateTransform(Transform3D newTransform) {
        this.fixture = this.fixture.withTransform(newTransform);
        setTranslateX(newTransform.x());
        setTranslateY(newTransform.y());
        setTranslateZ(newTransform.z());
        yawRotate.setAngle(newTransform.yaw());
        pitchRotate.setAngle(newTransform.pitch());
        rollRotate.setAngle(newTransform.roll());
        transformGizmo.setTransform(newTransform);
    }

    /**
     * Updates fixture metadata and 3D transform in-place without rebuilding geometry.
     *
     * @param updated updated fixture state
     */
    public void setFixtureAndTransform(Fixture updated) {
        if (updated == null) {
            return;
        }
        if (updated.startChannel() != this.fixture.startChannel()
                || updated.universe() != this.fixture.universe()) {
            invalidateDmxCache();
        }
        this.fixture = updated;
        updateTransform(updated.transform());
    }

    private void invalidateDmxCache() {
        lastDimmer = -1;
        lastPanCoarse = -1;
        lastTiltCoarse = -1;
    }

    /**
     * Applies DMX channel values to this fixture only when channel values or strobe state changed.
     *
     * @param universeData 512-byte DMX universe array
     * @return {@code true} if the 3D scene graph was updated, {@code false} if skipped
     */
    public boolean applyDmx(byte[] universeData) {
        if (universeData == null) {
            return false;
        }

        int start = fixture.startChannel() - 1;
        if (start < 0 || start >= universeData.length) {
            return false;
        }

        if (fixture.type().isMovingHead()) {
            int panCoarse = (universeData[start] & 0xFF);
            int panFine = (start + 1 < universeData.length) ? (universeData[start + 1] & 0xFF) : 0;
            int tiltCoarse = (start + 2 < universeData.length) ? (universeData[start + 2] & 0xFF) : 128;
            int tiltFine = (start + 3 < universeData.length) ? (universeData[start + 3] & 0xFF) : 0;

            int dimmer = (start + 5 < universeData.length) ? (universeData[start + 5] & 0xFF) : 0;
            int strobe = (start + 6 < universeData.length) ? (universeData[start + 6] & 0xFF) : 0;

            int r = (start + 7 < universeData.length) ? (universeData[start + 7] & 0xFF) : 0;
            int g = (start + 8 < universeData.length) ? (universeData[start + 8] & 0xFF) : 0;
            int b = (start + 9 < universeData.length) ? (universeData[start + 9] & 0xFF) : 0;
            int w = (start + 10 < universeData.length) ? (universeData[start + 10] & 0xFF) : 0;

            boolean panInv = fixture.panInvert();
            boolean tiltInv = fixture.tiltInvert();
            boolean strobing = ChannelMapping.isStrobing(strobe) && dimmer > 0;

            boolean unchanged = panCoarse == lastPanCoarse && panFine == lastPanFine
                    && tiltCoarse == lastTiltCoarse && tiltFine == lastTiltFine
                    && dimmer == lastDimmer && strobe == lastStrobe
                    && r == lastR && g == lastG && b == lastB && w == lastW
                    && panInv == lastPanInvert && tiltInv == lastTiltInvert;

            if (unchanged && !strobing) {
                return false;
            }

            boolean sceneMutated = false;

            if (panCoarse != lastPanCoarse || panFine != lastPanFine || panInv != lastPanInvert) {
                panRotate.setAngle(ChannelMapping.calculatePanAngle(panCoarse, panFine, 540.0, panInv));
                lastPanCoarse = panCoarse;
                lastPanFine = panFine;
                lastPanInvert = panInv;
                sceneMutated = true;
            }
            if (tiltCoarse != lastTiltCoarse || tiltFine != lastTiltFine || tiltInv != lastTiltInvert) {
                tiltRotate.setAngle(ChannelMapping.calculateTiltAngle(tiltCoarse, tiltFine, 270.0, tiltInv));
                lastTiltCoarse = tiltCoarse;
                lastTiltFine = tiltFine;
                lastTiltInvert = tiltInv;
                sceneMutated = true;
            }

            boolean appearanceChanged = dimmer != lastDimmer || strobe != lastStrobe
                    || r != lastR || g != lastG || b != lastB || w != lastW || strobing;

            if (appearanceChanged) {
                double wNorm = ChannelMapping.toNormalized(w);
                double rNorm = (r > 0 || g > 0 || b > 0 || w > 0) ? ChannelMapping.toNormalized(r) : 1.0;
                double gNorm = (r > 0 || g > 0 || b > 0 || w > 0) ? ChannelMapping.toNormalized(g) : 0.95;
                double bNorm = (r > 0 || g > 0 || b > 0 || w > 0) ? ChannelMapping.toNormalized(b) : 0.90;

                double dimNorm = ChannelMapping.toNormalized(dimmer);
                double strobeHz = strobing ? ChannelMapping.calculateStrobeHz(strobe) : 0.0;

                if (beamNode.update(dimNorm, rNorm, gNorm, bNorm, wNorm, strobing, strobeHz)) {
                    sceneMutated = true;
                }
                if (dimmer != lastDimmer || r != lastR || g != lastG || b != lastB || w != lastW) {
                    if (updateLens(dimNorm, rNorm, gNorm, bNorm, wNorm)) {
                        sceneMutated = true;
                    }
                }
                lastDimmer = dimmer;
                lastStrobe = strobe;
                lastR = r;
                lastG = g;
                lastB = b;
                lastW = w;
            }

            return sceneMutated;

        } else {
            int dimmer = (universeData[start] & 0xFF);
            int strobe = (start + 1 < universeData.length) ? (universeData[start + 1] & 0xFF) : 0;
            int r = (start + 2 < universeData.length) ? (universeData[start + 2] & 0xFF) : 0;
            int g = (start + 3 < universeData.length) ? (universeData[start + 3] & 0xFF) : 0;
            int b = (start + 4 < universeData.length) ? (universeData[start + 4] & 0xFF) : 0;
            int w = (start + 5 < universeData.length) ? (universeData[start + 5] & 0xFF) : 0;

            boolean strobing = ChannelMapping.isStrobing(strobe) && dimmer > 0;
            boolean unchanged = dimmer == lastDimmer && strobe == lastStrobe
                    && r == lastR && g == lastG && b == lastB && w == lastW;

            if (unchanged && !strobing) {
                return false;
            }

            double wNorm = ChannelMapping.toNormalized(w);
            double rNorm = (r > 0 || g > 0 || b > 0 || w > 0) ? ChannelMapping.toNormalized(r) : 1.0;
            double gNorm = (r > 0 || g > 0 || b > 0 || w > 0) ? ChannelMapping.toNormalized(g) : 0.95;
            double bNorm = (r > 0 || g > 0 || b > 0 || w > 0) ? ChannelMapping.toNormalized(b) : 0.90;

            double dimNorm = ChannelMapping.toNormalized(dimmer);
            double strobeHz = strobing ? ChannelMapping.calculateStrobeHz(strobe) : 0.0;

            boolean sceneMutated = beamNode.update(dimNorm, rNorm, gNorm, bNorm, wNorm, strobing, strobeHz);
            if (dimmer != lastDimmer || r != lastR || g != lastG || b != lastB || w != lastW) {
                if (updateLens(dimNorm, rNorm, gNorm, bNorm, wNorm)) {
                    sceneMutated = true;
                }
            }

            lastDimmer = dimmer;
            lastStrobe = strobe;
            lastR = r;
            lastG = g;
            lastB = b;
            lastW = w;
            return sceneMutated;
        }
    }

    private boolean updateLens(double dimNorm, double rNorm, double gNorm, double bNorm, double wNorm) {
        if (dimNorm > 0.01) {
            Color lensColor = Color.color(
                    Math.min(1.0, rNorm + (wNorm * 0.95)),
                    Math.min(1.0, gNorm + (wNorm * 0.95)),
                    Math.min(1.0, bNorm + (wNorm * 0.85))
            );
            lensMaterial.setDiffuseColor(lensColor);
            if (!lastLensLit) {
                lensMaterial.setSpecularColor(Color.WHITE);
                lastLensLit = true;
            }
            return true;
        } else if (lastLensLit) {
            lensMaterial.setDiffuseColor(LENS_OFF_COLOR);
            lensMaterial.setSpecularColor(Color.TRANSPARENT);
            lastLensLit = false;
            return true;
        }
        return false;
    }

    public Fixture getFixture() {
        return fixture;
    }
}
