package de.exit.artnet2rig.core.model;

/**
 * Types of supported lighting fixtures.
 */
public enum FixtureType {
    MOVING_HEAD_SPOT(16, "Moving Head Spot"),
    LED_PAR(8, "LED Par 64"),
    LED_BAR(8, "LED Bar");

    private final int defaultChannelCount;
    private final String displayName;

    FixtureType(int defaultChannelCount, String displayName) {
        this.defaultChannelCount = defaultChannelCount;
        this.displayName = displayName;
    }

    public int getDefaultChannelCount() {
        return defaultChannelCount;
    }

    public String getDisplayName() {
        return displayName;
    }
}
