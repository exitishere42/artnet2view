package de.exit.artnet2view.ui.canvas;

import de.exit.artnet2view.core.model.Fixture;
import de.exit.artnet2view.core.model.FixtureType;
import de.exit.artnet2view.core.model.Transform3D;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying dirty-state skipping and back-face culling optimization in BeamNode & Fixture3DNode.
 */
class BeamNodeAndFixtureOptimizationTest {

    @Test
    @DisplayName("BeamNode uses BACK culling and skips redundant updates on identical values")
    void testBeamDirtySkipping() {
        BeamNode beam = new BeamNode(9.0, 0.06, 0.70);
        MeshView cone = (MeshView) beam.getChildren().get(0);
        assertThat(cone.getCullFace()).isEqualTo(CullFace.BACK);

        // Initial state with dimmer = 0 should skip unnecessary material updates after first hide
        assertThat(beam.update(0.0, 1.0, 0.0, 0.0, 0.0, false, 0.0)).isFalse();
        assertThat(beam.isVisible()).isFalse();

        // Activating dimmer applies update
        assertThat(beam.update(0.8, 1.0, 0.2, 0.1, 0.0, false, 0.0)).isTrue();
        assertThat(beam.isVisible()).isTrue();

        // Identical next frame skips GPU/material update
        assertThat(beam.update(0.8, 1.0, 0.2, 0.1, 0.0, false, 0.0)).isFalse();

        // Changing color applies update
        assertThat(beam.update(0.8, 0.1, 0.9, 0.1, 0.0, false, 0.0)).isTrue();
    }

    @Test
    @DisplayName("Fixture3DNode.applyDmx skips scene-graph mutations when DMX channels are unchanged")
    void testFixtureApplyDmxDirtyCache() {
        Fixture fix = new Fixture(
                10L, "Spot 1", FixtureType.MOVING_HEAD_ZQ02040,
                0, 1, 11, new Transform3D(0, -3.5, 0, 0, 0, 0),
                false, false, 6.0
        );
        Fixture3DNode node = new Fixture3DNode(fix, null);

        byte[] dmx = new byte[512];
        dmx[0] = (byte) 128; // Pan
        dmx[2] = (byte) 128; // Tilt
        dmx[5] = (byte) 255; // Dimmer
        dmx[7] = (byte) 200; // Red

        assertThat(node.applyDmx(dmx)).isTrue();
        // Second call with identical DMX buffer must be skipped
        assertThat(node.applyDmx(dmx)).isFalse();

        // Mutate Blue channel -> must apply update
        dmx[9] = (byte) 255;
        assertThat(node.applyDmx(dmx)).isTrue();
        assertThat(node.applyDmx(dmx)).isFalse();

        // Null buffer returns false safely
        assertThat(node.applyDmx(null)).isFalse();
    }
}
