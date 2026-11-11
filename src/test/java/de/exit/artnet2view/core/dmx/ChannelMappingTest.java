package de.exit.artnet2view.core.dmx;

import javafx.scene.paint.Color;
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
        double centerAngle = ChannelMapping.calculatePanAngle(128, 0, 540.0, false);
        assertThat(centerAngle).isCloseTo(0.0, offset(1.5));

        double minAngle = ChannelMapping.calculatePanAngle(0, 0, 540.0, false);
        assertThat(minAngle).isCloseTo(-270.0, offset(0.01));

        double maxAngle = ChannelMapping.calculatePanAngle(255, 255, 540.0, false);
        assertThat(maxAngle).isCloseTo(270.0, offset(0.01));

        double invertedMin = ChannelMapping.calculatePanAngle(0, 0, 540.0, true);
        assertThat(invertedMin).isCloseTo(270.0, offset(0.01));
    }

    @Test
    @DisplayName("Calculate 16-bit Tilt angle (-135° to +135°)")
    void testTilt16Bit() {
        double center = ChannelMapping.calculateTiltAngle(128, 0, 270.0, false);
        assertThat(center).isCloseTo(0.0, offset(1.5));

        double min = ChannelMapping.calculateTiltAngle(0, 0, 270.0, false);
        assertThat(min).isCloseTo(-135.0, offset(0.01));

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
    @DisplayName("Shutter Strobe detection for ZQ02040 and standard moving heads")
    void testStrobeDetection() {
        assertThat(ChannelMapping.isStrobing(0)).isFalse();
        assertThat(ChannelMapping.isStrobing(7)).isFalse();
        assertThat(ChannelMapping.isStrobing(8)).isTrue();
        assertThat(ChannelMapping.isStrobing(16)).isTrue();
        assertThat(ChannelMapping.isStrobing(255)).isTrue();
    }

    @Test
    @DisplayName("Decode Color Wheel to RGB Color")
    void testColorWheelDecoding() {
        // 0-15: Open White
        Color openWhite = ChannelMapping.decodeColorWheel(0);
        assertThat(openWhite.getRed()).isCloseTo(1.0, offset(0.01));
        assertThat(openWhite.getGreen()).isCloseTo(1.0, offset(0.01));
        assertThat(openWhite.getBlue()).isCloseTo(1.0, offset(0.01));

        // Red wheel slot
        Color red = ChannelMapping.decodeColorWheel(20);
        assertThat(red.getRed()).isGreaterThan(0.8);
        assertThat(red.getGreen()).isLessThan(0.3);

        // Blue wheel slot
        Color blue = ChannelMapping.decodeColorWheel(55);
        assertThat(blue.getBlue()).isGreaterThan(0.8);
    }

    @Test
    @DisplayName("Verify U'King ZQ02040 11-Channel calculations")
    void testZq02040ChannelCalculations() {
        // Pan center
        double pan = ChannelMapping.calculatePanAngle(128, 0, 540.0, false);
        assertThat(pan).isCloseTo(0.0, offset(1.5));

        // Tilt center
        double tilt = ChannelMapping.calculateTiltAngle(128, 0, 270.0, false);
        assertThat(tilt).isCloseTo(0.0, offset(1.5));

        // Strobe threshold on ZQ02040 is >= 8
        assertThat(ChannelMapping.isStrobing(7)).isFalse();
        assertThat(ChannelMapping.isStrobing(8)).isTrue();
        assertThat(ChannelMapping.calculateStrobeHz(8)).isCloseTo(1.0, offset(0.1));
        assertThat(ChannelMapping.calculateStrobeHz(255)).isCloseTo(20.0, offset(0.1));
    }
}
