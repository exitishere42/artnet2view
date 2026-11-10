package de.exit.artnet2rig.core.artnet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArtNetPacketParser}.
 */
class ArtNetPacketParserTest {

    @Test
    @DisplayName("Parse valid ArtDmx packet successfully")
    void testParseValidArtDmxPacket() {
        ByteBuffer bb = ByteBuffer.allocate(18 + 512);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        // Header: "Art-Net\0"
        bb.put("Art-Net\0".getBytes());
        // OpCode: 0x5000 (OpDmx) Little Endian
        bb.putShort((short) 0x5000);
        // ProtVer: 14 Big Endian
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putShort((short) 14);
        // Sequence: 1, Physical: 0
        bb.put((byte) 1);
        bb.put((byte) 0);
        // SubUni (Little Endian): Universe 3
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.put((byte) 3);
        // Net: 0
        bb.put((byte) 0);
        // Length: 512 Big Endian
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putShort((short) 512);

        // Payload
        byte[] dmxData = new byte[512];
        dmxData[0] = (byte) 255;
        dmxData[1] = (byte) 128;
        bb.put(dmxData);

        byte[] rawBytes = bb.array();
        Optional<ArtNetDmxPacket> result = ArtNetPacketParser.parse(rawBytes, rawBytes.length);

        assertThat(result).isPresent();
        ArtNetDmxPacket packet = result.get();
        assertThat(packet.universe()).isEqualTo(3);
        assertThat(packet.length()).isEqualTo(512);
        assertThat(packet.data()[0] & 0xFF).isEqualTo(255);
        assertThat(packet.data()[1] & 0xFF).isEqualTo(128);
    }

    @Test
    @DisplayName("Reject packet with invalid header or opcode")
    void testRejectInvalidPacket() {
        byte[] invalidHeader = "Invalid\0".getBytes();
        Optional<ArtNetDmxPacket> result = ArtNetPacketParser.parse(invalidHeader, invalidHeader.length);
        assertThat(result).isEmpty();
    }
}
