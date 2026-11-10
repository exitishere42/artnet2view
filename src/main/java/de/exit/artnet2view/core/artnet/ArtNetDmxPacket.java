package de.exit.artnet2view.core.artnet;

/**
 * Immutable representation of a parsed ArtDmx packet.
 *
 * @param universe universe number (0 - 15)
 * @param sequence packet sequence number (0 - 255)
 * @param physical physical port code
 * @param length   number of channels in payload (2 - 512)
 * @param data     DMX payload byte array
 */
public record ArtNetDmxPacket(
        int universe,
        int sequence,
        int physical,
        int length,
        byte[] data
) {
}
