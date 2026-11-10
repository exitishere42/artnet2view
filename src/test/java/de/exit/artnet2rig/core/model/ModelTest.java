package de.exit.artnet2rig.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Transform3D} and {@link Fixture}.
 */
class ModelTest {

    @Test
    @DisplayName("Transform3D coordinates and rotations")
    void testTransform3D() {
        Transform3D t = new Transform3D(1.5, 2.0, -3.2, 45.0, 90.0, 0.0);
        assertThat(t.x()).isEqualTo(1.5);
        assertThat(t.y()).isEqualTo(2.0);
        assertThat(t.z()).isEqualTo(-3.2);
        assertThat(t.pitch()).isEqualTo(45.0);
        assertThat(t.yaw()).isEqualTo(90.0);
        assertThat(t.roll()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Fixture validation and channel calculations")
    void testFixtureValidation() {
        Fixture f = new Fixture(
                1L,
                "Spot 1",
                FixtureType.MOVING_HEAD_SPOT,
                0,
                1,
                16,
                Transform3D.ZERO,
                false,
                false,
                15.0
        );

        assertThat(f.universe()).isEqualTo(0);
        assertThat(f.startChannel()).isEqualTo(1);
        assertThat(f.channelCount()).isEqualTo(16);
        assertThat(f.endChannel()).isEqualTo(16);
    }

    @Test
    @DisplayName("Fixture with invalid DMX address throws IllegalArgumentException")
    void testInvalidFixtureThrows() {
        assertThatThrownBy(() -> new Fixture(
                1L,
                "Invalid Spot",
                FixtureType.MOVING_HEAD_SPOT,
                0,
                510,
                16, // 510 + 16 - 1 = 525 > 512!
                Transform3D.ZERO,
                false,
                false,
                15.0
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
