package de.exit.artnet2view.core.dmx;

/**
 * Utility functions for decoding DMX512 channel values into physical angles, normalized percentages, and strobe rates.
 */
public final class ChannelMapping {

    private ChannelMapping() {
    }

    /**
     * Calculates Pan rotation angle in degrees from coarse and fine 8-bit DMX channels.
     *
     * @param coarse       coarse byte value (0 - 255)
     * @param fine         fine byte value (0 - 255)
     * @param maxRangeDeg  total Pan range in degrees (e.g. 540.0)
     * @param invert       whether rotation direction is inverted
     * @return calculated angle in degrees centered around 0
     */
    public static double calculatePanAngle(int coarse, int fine, double maxRangeDeg, boolean invert) {
        int raw16 = ((coarse & 0xFF) << 8) | (fine & 0xFF);
        double normalized = raw16 / 65535.0; // 0.0 to 1.0
        if (invert) {
            normalized = 1.0 - normalized;
        }
        return (normalized * maxRangeDeg) - (maxRangeDeg / 2.0);
    }

    /**
     * Calculates Tilt rotation angle in degrees from coarse and fine 8-bit DMX channels.
     *
     * @param coarse       coarse byte value (0 - 255)
     * @param fine         fine byte value (0 - 255)
     * @param maxRangeDeg  total Tilt range in degrees (e.g. 270.0)
     * @param invert       whether rotation direction is inverted
     * @return calculated angle in degrees centered around 0
     */
    public static double calculateTiltAngle(int coarse, int fine, double maxRangeDeg, boolean invert) {
        int raw16 = ((coarse & 0xFF) << 8) | (fine & 0xFF);
        double normalized = raw16 / 65535.0; // 0.0 to 1.0
        if (invert) {
            normalized = 1.0 - normalized;
        }
        return (normalized * maxRangeDeg) - (maxRangeDeg / 2.0);
    }

    /**
     * Converts an 8-bit unsigned DMX channel value (0 - 255) to a normalized float (0.0 - 1.0).
     */
    public static double toNormalized(int dmxValue) {
        return Math.max(0.0, Math.min(1.0, (dmxValue & 0xFF) / 255.0));
    }

    /**
     * Determines whether the shutter/strobe channel value triggers strobing.
     * Values 0-15 are typically shutter open or closed; 16-255 represent increasing strobe speed.
     */
    public static boolean isStrobing(int strobeDmx) {
        return (strobeDmx & 0xFF) >= 16;
    }

    /**
     * Calculates strobe frequency in Hertz (flashes per second) from DMX value (16 - 255).
     */
    public static double calculateStrobeHz(int strobeDmx) {
        int val = strobeDmx & 0xFF;
        if (val < 16) {
            return 0.0;
        }
        // Map 16..255 to 1.0Hz .. 20.0Hz
        double factor = (val - 16) / 239.0;
        return 1.0 + (factor * 19.0);
    }
}
