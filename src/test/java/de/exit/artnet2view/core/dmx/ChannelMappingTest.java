package de.exit.artnet2view.core.dmx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

/**
 * Unit tests for DMX channel decoding and 16-bit Pan/Tilt angular calculations.
 */
class ChannelMappingTest {

    @Test
    @DisplayName("Calculate 16-bit Pan angle (-270° to +270°)")
    void testPan16Bit() {
        // Center position: coarse = 128, fine = 0 -> ~ 0 degrees
        double centerAngle = ChannelMapping.calculatePanAngle(128, 0, 540.0, false);
        assertThat(centerAngle).isCloseTo(0.0, offset(1.5));

        // Minimum position: coarse = 0, fine = 0 -> -270 degrees
        double minAngle = ChannelMapping.calculatePanAngle(0, 0, 540.0, false);
        assertThat(minAngle).isCloseTo(-270.0, offset(0.01));

        // Maximum position: coarse = 255, fine = 255 -> +270 degrees
        double maxAngle = ChannelMapping.calculatePanAngle(255, 255, 540.0, false);
        assertThat(maxAngle).isCloseTo(270.0, offset(0.01));

        // Inverted: min becomes max
        double invertedMin = ChannelMapping.calculatePanAngle(0, 0, 540.0, true);
        assertThat(invertedMin).isCloseTo(270.0, offset(0.01));
    }

    @Test
    @DisplayName("Calculate 16-bit Tilt angle (-135° to +135°)")
    void testTilt16Bit() {
        // Center: ~ 0 degrees
        double center = ChannelMapping.calculateTiltAngle(128, 0, 270.0, false);
        assertThat(center).isCloseTo(0.0, offset(1.5));

        // Minimum: -135 degrees
        double min = ChannelMapping.calculateTiltAngle(0, 0, 270.0, false);
        assertThat(min).isCloseTo(-135.0, offset(0.01));

        // Maximum: +135 degrees
        double max = ChannelMapping.calculateTiltAngle(255, 255, 270.0, false);
        assertThat(max).isCloseTo(135.0, offset(0.01));
    }

    @Test
    @DisplayName("Normalize 8-bit DMX value to 0.0 - 1.0 range")
    void testNormalize8Bit() {
        assertThat(ChannelMapping.toNormalized(0)).isEqualTo(0.0);
        assertThat(ChannelMapping.toNormalized(255)).isEqualTo(1.0);
        assertThat(ChannelMapping.toNormalized(127)).isCloseTo(0.498, offset(0.01));
    }

    @Test
    @DisplayName("Shutter Strobe detection")
    void testStrobeDetection() {
        // 0-15: Shutter open (no strobe)
        assertThat(ChannelMapping.isStrobing(0)).isFalse();
        assertThat(ChannelMapping.isStrobing(15)).isFalse();

        // 16-255: Active strobe
        assertThat(ChannelMapping.isStrobing(16)).isTrue();
        assertThat(ChannelMapping.isStrobing(255)).isTrue();
    }
}
