package de.exit.artnet2view.core.artnet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Optional;

/**
 * High-performance parser for incoming Art-Net UDP datagram packets.
 */
public final class ArtNetPacketParser {

    private static final byte[] ARTNET_HEADER = new byte[]{'A', 'r', 't', '-', 'N', 'e', 't', 0};
    public static final int OP_DMX = 0x5000;
    public static final int MIN_PACKET_LENGTH = 18;

    private ArtNetPacketParser() {
    }

    /**
     * Parses an incoming UDP datagram packet into an {@link ArtNetDmxPacket}.
     *
     * @param bytes  raw UDP packet bytes
     * @param length packet length
     * @return parsed packet if valid OpDmx, empty otherwise
     */
    public static Optional<ArtNetDmxPacket> parse(byte[] bytes, int length) {
        if (bytes == null || length < MIN_PACKET_LENGTH) {
            return Optional.empty();
        }

        // Validate "Art-Net\0" header
        for (int i = 0; i < 8; i++) {
            if (bytes[i] != ARTNET_HEADER[i]) {
                return Optional.empty();
            }
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes, 0, length);
        buffer.position(8);

        // OpCode is Little-Endian
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int opCode = Short.toUnsignedInt(buffer.getShort());
        if (opCode != OP_DMX) {
            return Optional.empty();
        }

        // ProtVer is Big-Endian
        buffer.order(ByteOrder.BIG_ENDIAN);
        int protVer = Short.toUnsignedInt(buffer.getShort());
        if (protVer < 14) {
            return Optional.empty();
        }

        int sequence = Byte.toUnsignedInt(buffer.get());
        int physical = Byte.toUnsignedInt(buffer.get());

        // SubUni (Low 8 bits of universe) & Net (High 7 bits)
        int subUni = Byte.toUnsignedInt(buffer.get());
        int net = Byte.toUnsignedInt(buffer.get());
        int universe = (net << 8) | subUni;

        // Length (Big-Endian)
        int dataLength = Short.toUnsignedInt(buffer.getShort());
        if (dataLength < 2 || dataLength > 512) {
            return Optional.empty();
        }

        int remaining = buffer.remaining();
        int actualCopyLen = Math.min(dataLength, remaining);
        byte[] dmxData = new byte[actualCopyLen];
        buffer.get(dmxData);

        return Optional.of(new ArtNetDmxPacket(universe, sequence, physical, actualCopyLen, dmxData));
    }
}
