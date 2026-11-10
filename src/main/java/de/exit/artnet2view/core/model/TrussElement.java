package de.exit.artnet2view.core.model;

/**
 * Physical truss segment positioned in 3D space.
 *
 * @param id        unique entity ID
 * @param type      truss type
 * @param transform 3D position and rotation
 */
public record TrussElement(
        Long id,
        TrussType type,
        Transform3D transform
) {
    public TrussElement {
        if (type == null) {
            throw new IllegalArgumentException("Truss type must not be null");
        }
        if (transform == null) {
            transform = Transform3D.ZERO;
        }
    }
}
