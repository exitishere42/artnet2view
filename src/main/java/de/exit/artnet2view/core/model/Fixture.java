package de.exit.artnet2view.core.model;

/**
 * Lighting fixture with 3D transform, Art-Net universe patch, and DMX channel configuration.
 *
 * @param id           unique entity ID
 * @param name         user-assigned name
 * @param type         fixture type
 * @param universe     Art-Net universe index (0 - 15)
 * @param startChannel 1-based start channel (1 - 512)
 * @param channelCount number of consecutive channels
 * @param transform    3D position and rotation
 * @param panInvert    whether Pan rotation is inverted
 * @param tiltInvert   whether Tilt rotation is inverted
 * @param beamAngle    beam cone angle in degrees
 */
public record Fixture(
        Long id,
        String name,
        FixtureType type,
        int universe,
        int startChannel,
        int channelCount,
        Transform3D transform,
        boolean panInvert,
        boolean tiltInvert,
        double beamAngle
) {
    public Fixture {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Fixture name must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Fixture type must not be null");
        }
        if (universe < 0 || universe > 15) {
            throw new IllegalArgumentException("Universe must be between 0 and 15");
        }
        if (startChannel < 1 || startChannel > 512) {
            throw new IllegalArgumentException("Start channel must be between 1 and 512");
        }
        if (channelCount < 1 || channelCount > 512) {
            throw new IllegalArgumentException("Channel count must be between 1 and 512");
        }
        if (startChannel + channelCount - 1 > 512) {
            throw new IllegalArgumentException("Fixture channels exceed 512-channel universe boundary: " + (startChannel + channelCount - 1));
        }
        if (transform == null) {
            transform = Transform3D.ZERO;
        }
    }

    public int endChannel() {
        return startChannel + channelCount - 1;
    }
}
