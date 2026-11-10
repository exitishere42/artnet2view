package de.exit.artnet2view.ui.canvas;

import de.exit.artnet2view.core.dmx.ChannelMapping;
import de.exit.artnet2view.core.model.Fixture;
import de.exit.artnet2view.core.model.FixtureType;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * Interactive 3D scene-graph representation of a lighting fixture.
 * Supports independent Pan/Tilt rotations, lens emission, and volumetric beam cone.
 */
public class Fixture3DNode extends Group {

    private final Fixture fixture;
    private final Rotate panRotate;
    private final Rotate tiltRotate;
    private final BeamNode beamNode;
    private final Cylinder lensNode;
    private final PhongMaterial lensMaterial;

    public Fixture3DNode(Fixture fixture) {
        this.fixture = fixture;

        // Position root at fixture coordinates
        setTranslateX(fixture.transform().x());
        setTranslateY(fixture.transform().y());
        setTranslateZ(fixture.transform().z());

        // Base rotations
        getTransforms().addAll(
                new Rotate(fixture.transform().yaw(), Rotate.Y_AXIS),
                new Rotate(fixture.transform().pitch(), Rotate.X_AXIS),
                new Rotate(fixture.transform().roll(), Rotate.Z_AXIS)
        );

        this.panRotate = new Rotate(0, Rotate.Y_AXIS);
        this.tiltRotate = new Rotate(0, Rotate.X_AXIS);

        this.beamNode = new BeamNode(9.0, 0.08, 1.4);
        this.lensMaterial = new PhongMaterial(Color.BLACK);
        this.lensNode = new Cylinder(0.08, 0.02);
        this.lensNode.setMaterial(lensMaterial);

        buildGeometry();
    }

    private void buildGeometry() {
        PhongMaterial housingMat = new PhongMaterial(Color.web("#1E1E1E"));
        housingMat.setSpecularColor(Color.web("#424242"));

        if (fixture.type() == FixtureType.MOVING_HEAD_SPOT) {
            // 1. Static Base
            Box base = new Box(0.32, 0.12, 0.32);
            base.setMaterial(housingMat);
            base.setTranslateY(0.06);

            // 2. Rotating Yoke (Pan)
            Group yokeGroup = new Group();
            yokeGroup.setTranslateY(0.12);
            yokeGroup.getTransforms().add(panRotate);

            // Yoke Crossbar
            Box yokeBottom = new Box(0.30, 0.06, 0.12);
            yokeBottom.setMaterial(housingMat);
            yokeBottom.setTranslateY(0.03);

            // Yoke Left & Right Arms
            Box yokeLeft = new Box(0.06, 0.26, 0.10);
            yokeLeft.setMaterial(housingMat);
            yokeLeft.setTranslateX(-0.13);
            yokeLeft.setTranslateY(0.16);

            Box yokeRight = new Box(0.06, 0.26, 0.10);
            yokeRight.setMaterial(housingMat);
            yokeRight.setTranslateX(0.13);
            yokeRight.setTranslateY(0.16);

            yokeGroup.getChildren().addAll(yokeBottom, yokeLeft, yokeRight);

            // 3. Tilting Head (Tilt) inside Yoke
            Group headGroup = new Group();
            headGroup.setTranslateY(0.24);
            headGroup.getTransforms().add(tiltRotate);

            // Head Body
            Sphere headBody = new Sphere(0.13);
            headBody.setMaterial(housingMat);

            Cylinder snout = new Cylinder(0.10, 0.16);
            snout.setMaterial(housingMat);
            snout.setTranslateY(0.10);

            // Front Lens
            lensNode.setTranslateY(0.18);

            // Volumetric Beam attached to Head tip pointing downwards along Y
            beamNode.setTranslateY(0.19);

            headGroup.getChildren().addAll(headBody, snout, lensNode, beamNode);
            yokeGroup.getChildren().add(headGroup);

            getChildren().addAll(base, yokeGroup);

        } else if (fixture.type() == FixtureType.LED_PAR) {
            // Static LED Par Can
            Cylinder can = new Cylinder(0.12, 0.24);
            can.setMaterial(housingMat);
            can.setTranslateY(0.12);

            lensNode.setTranslateY(0.24);
            beamNode.setTranslateY(0.25);

            getChildren().addAll(can, lensNode, beamNode);

        } else {
            // LED Bar
            Box bar = new Box(1.0, 0.08, 0.08);
            bar.setMaterial(housingMat);
            bar.setTranslateY(0.04);

            lensNode.setTranslateY(0.08);
            beamNode.setTranslateY(0.09);

            getChildren().addAll(bar, lensNode, beamNode);
        }
    }

    /**
     * Decodes and applies incoming DMX universe channel data in real-time.
     *
     * @param universeData full 512-byte universe snapshot
     */
    public void applyDmx(byte[] universeData) {
        if (universeData == null) {
            return;
        }

        int start = fixture.startChannel() - 1; // 0-based
        if (start < 0 || start >= universeData.length) {
            return;
        }

        if (fixture.type() == FixtureType.MOVING_HEAD_SPOT) {
            int panCoarse = (start < universeData.length) ? (universeData[start] & 0xFF) : 128;
            int panFine = (start + 1 < universeData.length) ? (universeData[start + 1] & 0xFF) : 0;
            int tiltCoarse = (start + 2 < universeData.length) ? (universeData[start + 2] & 0xFF) : 128;
            int tiltFine = (start + 3 < universeData.length) ? (universeData[start + 3] & 0xFF) : 0;

            int dimmer = (start + 5 < universeData.length) ? (universeData[start + 5] & 0xFF) : 0;
            int strobe = (start + 6 < universeData.length) ? (universeData[start + 6] & 0xFF) : 0;
            int r = (start + 7 < universeData.length) ? (universeData[start + 7] & 0xFF) : 255;
            int g = (start + 8 < universeData.length) ? (universeData[start + 8] & 0xFF) : 255;
            int b = (start + 9 < universeData.length) ? (universeData[start + 9] & 0xFF) : 255;
            int w = (start + 10 < universeData.length) ? (universeData[start + 10] & 0xFF) : 0;

            double panDeg = ChannelMapping.calculatePanAngle(panCoarse, panFine, 540.0, fixture.panInvert());
            double tiltDeg = ChannelMapping.calculateTiltAngle(tiltCoarse, tiltFine, 270.0, fixture.tiltInvert());

            panRotate.setAngle(panDeg);
            tiltRotate.setAngle(tiltDeg);

            double dimNorm = ChannelMapping.toNormalized(dimmer);
            double rNorm = ChannelMapping.toNormalized(r);
            double gNorm = ChannelMapping.toNormalized(g);
            double bNorm = ChannelMapping.toNormalized(b);
            double wNorm = ChannelMapping.toNormalized(w);

            boolean strobing = ChannelMapping.isStrobing(strobe);
            double strobeHz = ChannelMapping.calculateStrobeHz(strobe);

            beamNode.update(dimNorm, rNorm, gNorm, bNorm, wNorm, strobing, strobeHz);

            // Update glowing lens color
            if (dimNorm > 0.01) {
                Color lensColor = Color.color(Math.min(1.0, rNorm + wNorm), Math.min(1.0, gNorm + wNorm), Math.min(1.0, bNorm + wNorm));
                lensMaterial.setDiffuseColor(lensColor);
                lensMaterial.setSpecularColor(Color.WHITE);
            } else {
                lensMaterial.setDiffuseColor(Color.BLACK);
            }

        } else {
            // Pars & Bars: Channels 1=Dimmer, 2=Strobe, 3=Red, 4=Green, 5=Blue, 6=White
            int dimmer = (start < universeData.length) ? (universeData[start] & 0xFF) : 0;
            int strobe = (start + 1 < universeData.length) ? (universeData[start + 1] & 0xFF) : 0;
            int r = (start + 2 < universeData.length) ? (universeData[start + 2] & 0xFF) : 255;
            int g = (start + 3 < universeData.length) ? (universeData[start + 3] & 0xFF) : 255;
            int b = (start + 4 < universeData.length) ? (universeData[start + 4] & 0xFF) : 255;
            int w = (start + 5 < universeData.length) ? (universeData[start + 5] & 0xFF) : 0;

            double dimNorm = ChannelMapping.toNormalized(dimmer);
            double rNorm = ChannelMapping.toNormalized(r);
            double gNorm = ChannelMapping.toNormalized(g);
            double bNorm = ChannelMapping.toNormalized(b);
            double wNorm = ChannelMapping.toNormalized(w);

            boolean strobing = ChannelMapping.isStrobing(strobe);
            double strobeHz = ChannelMapping.calculateStrobeHz(strobe);

            beamNode.update(dimNorm, rNorm, gNorm, bNorm, wNorm, strobing, strobeHz);

            if (dimNorm > 0.01) {
                Color lensColor = Color.color(Math.min(1.0, rNorm + wNorm), Math.min(1.0, gNorm + wNorm), Math.min(1.0, bNorm + wNorm));
                lensMaterial.setDiffuseColor(lensColor);
            } else {
                lensMaterial.setDiffuseColor(Color.BLACK);
            }
        }
    }

    public Fixture getFixture() {
        return fixture;
    }
}
