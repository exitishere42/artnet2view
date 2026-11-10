package de.exit.artnet2rig.ui.canvas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for spherical camera coordinate math.
 */
class CameraMathTest {

    @Test
    @DisplayName("Calculate Cartesian coordinates from spherical orbit parameters")
    void testSphericalToCartesian() {
        double radius = 10.0;
        double pitch = 0.0; // horizontal
        double yaw = 0.0;   // facing Z

        double x = radius * Math.cos(Math.toRadians(pitch)) * Math.sin(Math.toRadians(yaw));
        double y = radius * Math.sin(Math.toRadians(pitch));
        double z = -radius * Math.cos(Math.toRadians(pitch)) * Math.cos(Math.toRadians(yaw));

        assertThat(x).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.001));
        assertThat(y).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.001));
        assertThat(z).isCloseTo(-10.0, org.assertj.core.data.Offset.offset(0.001));
    }
}
