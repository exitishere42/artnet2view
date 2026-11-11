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
 * Supports independent Pan/Tilt rotations, lens emission, volumetric beam cone,
 * selection highlighting, and 3D transformation gizmo arrows (X/Y/Z).
 */
public class Fixture3DNode extends Group {

    private Fixture fixture;
    private final Rotate panRotate;
    private final Rotate tiltRotate;
    private final BeamNode beamNode;
    private final Cylinder lensNode;
    private final PhongMaterial lensMaterial;

    private final Cylinder selectionRing;
    private final PhongMaterial selectionMaterial;
    private final TransformGizmo3D transformGizmo;

    public Fixture3DNode(Fixture fixture, Consumer<Fixture> onSelect) {
        this(fixture, onSelect, null);
    }

    public Fixture3DNode(Fixture fixture, Consumer<Fixture> onSelect, Consumer<Fixture> onTransformUpdated) {
        this.fixture = fixture;

        setTranslateX(fixture.transform().x());
        setTranslateY(fixture.transform().y());
        setTranslateZ(fixture.transform().z());

        getTransforms().addAll(
                new Rotate(fixture.transform().yaw(), Rotate.Y_AXIS),
                new Rotate(fixture.transform().pitch(), Rotate.X_AXIS),
                new Rotate(fixture.transform().roll(), Rotate.Z_AXIS)
        );

        this.panRotate = new Rotate(0, Rotate.Y_AXIS);
        this.tiltRotate = new Rotate(0, Rotate.X_AXIS);

        this.beamNode = new BeamNode(9.0, 0.06, 0.70);
        this.lensMaterial = new PhongMaterial(Color.BLACK);
        this.lensMaterial.setSpecularColor(Color.TRANSPARENT);

        this.lensNode = new Cylinder(0.08, 0.02);
        this.lensNode.setMaterial(lensMaterial);

        this.selectionMaterial = new PhongMaterial(Color.web("#00E676"));
        this.selectionMaterial.setSpecularColor(Color.WHITE);
        this.selectionRing = new Cylinder(0.24, 0.015);
        this.selectionRing.setMaterial(selectionMaterial);
        this.selectionRing.setVisible(false);

        // 3D Axis Transformation Gizmo with Arrows (X=Red, Y=Green, Z=Blue)
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
        PhongMaterial housingMat = new PhongMaterial(Color.web("#1E1E1E"));
        housingMat.setSpecularColor(Color.web("#424242"));

        getChildren().addAll(selectionRing, transformGizmo);

        if (fixture.type().isMovingHead()) {
            Box base = new Box(0.32, 0.12, 0.32);
            base.setMaterial(housingMat);
            base.setTranslateY(0.06);

            Group yokeGroup = new Group();
            yokeGroup.setTranslateY(0.12);
            yokeGroup.getTransforms().add(panRotate);

            Box yokeBottom = new Box(0.30, 0.06, 0.12);
            yokeBottom.setMaterial(housingMat);
            yokeBottom.setTranslateY(0.03);

            Box yokeLeft = new Box(0.06, 0.26, 0.10);
            yokeLeft.setMaterial(housingMat);
            yokeLeft.setTranslateX(-0.13);
            yokeLeft.setTranslateY(0.16);

            Box yokeRight = new Box(0.06, 0.26, 0.10);
            yokeRight.setMaterial(housingMat);
            yokeRight.setTranslateX(0.13);
            yokeRight.setTranslateY(0.16);

            yokeGroup.getChildren().addAll(yokeBottom, yokeLeft, yokeRight);

            Group headGroup = new Group();
            headGroup.setTranslateY(0.24);
            headGroup.getTransforms().add(tiltRotate);

            Sphere headBody = new Sphere(0.13);
            headBody.setMaterial(housingMat);

            Cylinder snout = new Cylinder(0.10, 0.16);
            snout.setMaterial(housingMat);
            snout.setTranslateY(0.10);

            lensNode.setTranslateY(0.18);
            beamNode.setTranslateY(0.19);

            headGroup.getChildren().addAll(headBody, snout, lensNode, beamNode);
            yokeGroup.getChildren().add(headGroup);

            getChildren().addAll(base, yokeGroup);

        } else if (fixture.type() == FixtureType.LED_PAR) {
            Cylinder can = new Cylinder(0.12, 0.24);
            can.setMaterial(housingMat);
            can.setTranslateY(0.12);

            lensNode.setTranslateY(0.24);
            beamNode.setTranslateY(0.25);

            getChildren().addAll(can, lensNode, beamNode);

        } else {
            Box bar = new Box(1.0, 0.08, 0.08);
            bar.setMaterial(housingMat);
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
        transformGizmo.setTransform(newTransform);
    }

    public void applyDmx(byte[] universeData) {
        if (universeData == null) {
            return;
        }

        int start = fixture.startChannel() - 1;
        if (start < 0 || start >= universeData.length) {
            return;
        }

        if (fixture.type().isMovingHead()) {
            // U'King ZQ02040 DMX Layout (11CH / 13CH):
            // CH 1: Pan Coarse (0-540°)
            // CH 2: Pan Fine (16-bit)
            // CH 3: Tilt Coarse (0-270°)
            // CH 4: Tilt Fine (16-bit)
            // CH 5: Pan/Tilt Speed
            // CH 6: Master Dimmer (0-255)
            // CH 7: Strobe (0-7 open, 8-255 variable strobe)
            // CH 8: Red (0-255)
            // CH 9: Green (0-255)
            // CH 10: Blue (0-255)
            // CH 11: White (0-255)
            int panCoarse = (start < universeData.length) ? (universeData[start] & 0xFF) : 128;
            int panFine = (start + 1 < universeData.length) ? (universeData[start + 1] & 0xFF) : 0;
            int tiltCoarse = (start + 2 < universeData.length) ? (universeData[start + 2] & 0xFF) : 128;
            int tiltFine = (start + 3 < universeData.length) ? (universeData[start + 3] & 0xFF) : 0;

            int dimmer = (start + 5 < universeData.length) ? (universeData[start + 5] & 0xFF) : 0;
            int strobe = (start + 6 < universeData.length) ? (universeData[start + 6] & 0xFF) : 0;

            int r = (start + 7 < universeData.length) ? (universeData[start + 7] & 0xFF) : 0;
            int g = (start + 8 < universeData.length) ? (universeData[start + 8] & 0xFF) : 0;
            int b = (start + 9 < universeData.length) ? (universeData[start + 9] & 0xFF) : 0;
            int w = (start + 10 < universeData.length) ? (universeData[start + 10] & 0xFF) : 0;

            double rNorm;
            double gNorm;
            double bNorm;
            double wNorm = ChannelMapping.toNormalized(w);

            if (r > 0 || g > 0 || b > 0 || w > 0) {
                rNorm = ChannelMapping.toNormalized(r);
                gNorm = ChannelMapping.toNormalized(g);
                bNorm = ChannelMapping.toNormalized(b);
            } else {
                // If desk only brings up Dimmer without explicit RGB, default to Warm White
                rNorm = 1.0;
                gNorm = 0.95;
                bNorm = 0.90;
            }

            double panDeg = ChannelMapping.calculatePanAngle(panCoarse, panFine, 540.0, fixture.panInvert());
            double tiltDeg = ChannelMapping.calculateTiltAngle(tiltCoarse, tiltFine, 270.0, fixture.tiltInvert());

            panRotate.setAngle(panDeg);
            tiltRotate.setAngle(tiltDeg);

            double dimNorm = ChannelMapping.toNormalized(dimmer);
            boolean strobing = ChannelMapping.isStrobing(strobe);
            double strobeHz = ChannelMapping.calculateStrobeHz(strobe);

            beamNode.update(dimNorm, rNorm, gNorm, bNorm, wNorm, strobing, strobeHz);
            updateLens(dimNorm, rNorm, gNorm, bNorm, wNorm);

        } else {
            // Pars & Bars: Ch 1=Dimmer, Ch 2=Strobe, Ch 3=Red, Ch 4=Green, Ch 5=Blue, Ch 6=White
            int dimmer = (start < universeData.length) ? (universeData[start] & 0xFF) : 0;
            int strobe = (start + 1 < universeData.length) ? (universeData[start + 1] & 0xFF) : 0;
            int r = (start + 2 < universeData.length) ? (universeData[start + 2] & 0xFF) : 0;
            int g = (start + 3 < universeData.length) ? (universeData[start + 3] & 0xFF) : 0;
            int b = (start + 4 < universeData.length) ? (universeData[start + 4] & 0xFF) : 0;
            int w = (start + 5 < universeData.length) ? (universeData[start + 5] & 0xFF) : 0;

            double rNorm;
            double gNorm;
            double bNorm;
            double wNorm = ChannelMapping.toNormalized(w);

            if (r > 0 || g > 0 || b > 0 || w > 0) {
                rNorm = ChannelMapping.toNormalized(r);
                gNorm = ChannelMapping.toNormalized(g);
                bNorm = ChannelMapping.toNormalized(b);
            } else {
                rNorm = 1.0;
                gNorm = 0.95;
                bNorm = 0.90;
            }

            double dimNorm = ChannelMapping.toNormalized(dimmer);
            boolean strobing = ChannelMapping.isStrobing(strobe);
            double strobeHz = ChannelMapping.calculateStrobeHz(strobe);

            beamNode.update(dimNorm, rNorm, gNorm, bNorm, wNorm, strobing, strobeHz);
            updateLens(dimNorm, rNorm, gNorm, bNorm, wNorm);
        }
    }

    private void updateLens(double dimNorm, double rNorm, double gNorm, double bNorm, double wNorm) {
        if (dimNorm > 0.01) {
            Color lensColor = Color.color(
                    Math.min(1.0, rNorm + (wNorm * 0.95)),
                    Math.min(1.0, gNorm + (wNorm * 0.95)),
                    Math.min(1.0, bNorm + (wNorm * 0.85))
            );
            lensMaterial.setDiffuseColor(lensColor);
            lensMaterial.setSpecularColor(Color.WHITE);
        } else {
            lensMaterial.setDiffuseColor(Color.web("#1A1A1A"));
            lensMaterial.setSpecularColor(Color.TRANSPARENT);
        }
    }

    public Fixture getFixture() {
        return fixture;
    }
}
