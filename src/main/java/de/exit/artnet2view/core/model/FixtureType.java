package de.exit.artnet2view.core.model;

/**
 * Types of supported lighting fixtures.
 */
public enum FixtureType {
    MOVING_HEAD_ZQ02040(11, "U'King ZQ02040 (11CH)"),
    MOVING_HEAD_ZQ02040_13CH(13, "U'King ZQ02040 (13CH)"),
    MOVING_HEAD_SPOT(11, "Moving Head Spot (ZQ02040 Layout)"),
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

    /**
     * Checks if this fixture type represents a moving head with Pan/Tilt capability.
     */
    public boolean isMovingHead() {
        return this == MOVING_HEAD_ZQ02040 || this == MOVING_HEAD_ZQ02040_13CH || this == MOVING_HEAD_SPOT;
    }
}
