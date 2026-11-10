package de.exit.artnet2view.core.model;

/**
 * Types of modular 4-point truss elements (F34 standard).
 */
public enum TrussType {
    STRAIGHT_1M(1.0, "Straight 1m"),
    STRAIGHT_2M(2.0, "Straight 2m"),
    STRAIGHT_3M(3.0, "Straight 3m"),
    CORNER_90(0.5, "90° Corner"),
    T_JOINT(0.5, "T-Joint"),
    BASE_PLATE(0.1, "Base Plate");

    private final double lengthMeters;
    private final String displayName;

    TrussType(double lengthMeters, String displayName) {
        this.lengthMeters = lengthMeters;
        this.displayName = displayName;
    }

    public double getLengthMeters() {
        return lengthMeters;
    }

    public String getDisplayName() {
        return displayName;
    }
}
