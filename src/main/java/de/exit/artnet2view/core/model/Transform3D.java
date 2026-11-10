package de.exit.artnet2view.core.model;

/**
 * Immutable 3D spatial transformation (position and Euler rotation angles in degrees).
 *
 * @param x     X translation in meters
 * @param y     Y translation in meters (upwards)
 * @param z     Z translation in meters (depth)
 * @param pitch Pitch rotation around X axis (degrees)
 * @param yaw   Yaw rotation around Y axis (degrees)
 * @param roll  Roll rotation around Z axis (degrees)
 */
public record Transform3D(
        double x,
        double y,
        double z,
        double pitch,
        double yaw,
        double roll
) {
    public static final Transform3D ZERO = new Transform3D(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

    public Transform3D withPosition(double newX, double newY, double newZ) {
        return new Transform3D(newX, newY, newZ, this.pitch, this.yaw, this.roll);
    }

    public Transform3D withRotation(double newPitch, double newYaw, double newRoll) {
        return new Transform3D(this.x, this.y, this.z, newPitch, newYaw, newRoll);
    }
}
