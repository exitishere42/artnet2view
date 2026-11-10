package de.exit.artnet2view.core.artnet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DmxUniverseBuffer}.
 */
class DmxUniverseBufferTest {

    private DmxUniverseBuffer buffer;

    @BeforeEach
    void setUp() {
        buffer = new DmxUniverseBuffer();
    }

    @Test
    @DisplayName("Initial buffer channels should all be zero")
    void testInitialChannelsAreZero() {
        for (int u = 0; u < DmxUniverseBuffer.MAX_UNIVERSES; u++) {
            byte[] universeData = buffer.getUniverse(u);
            assertThat(universeData).hasSize(512);
            for (byte b : universeData) {
                assertThat(b).isEqualTo((byte) 0);
            }
        }
    }

    @Test
    @DisplayName("Set and get channel values within boundary limits")
    void testSetAndGetChannel() {
        buffer.setChannel(0, 1, 255);
        buffer.setChannel(0, 512, 128);

        assertThat(buffer.getChannel(0, 1)).isEqualTo(255);
        assertThat(buffer.getChannel(0, 512)).isEqualTo(128);
        assertThat(buffer.getChannel(0, 2)).isEqualTo(0);
    }

    @Test
    @DisplayName("Updating whole universe payload")
    void testUpdateUniversePayload() {
        byte[] payload = new byte[512];
        payload[0] = (byte) 255;
        payload[10] = (byte) 100;
        payload[511] = (byte) 42;

        buffer.updateUniverse(2, payload, 512);

        assertThat(buffer.getChannel(2, 1)).isEqualTo(255);
        assertThat(buffer.getChannel(2, 11)).isEqualTo(100);
        assertThat(buffer.getChannel(2, 512)).isEqualTo(42);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 16, 100})
    @DisplayName("Invalid universe index throws IllegalArgumentException")
    void testInvalidUniverseThrows(int invalidUniverse) {
        assertThatThrownBy(() -> buffer.getUniverse(invalidUniverse))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 513, -5})
    @DisplayName("Invalid channel index throws IllegalArgumentException")
    void testInvalidChannelThrows(int invalidChannel) {
        assertThatThrownBy(() -> buffer.getChannel(0, invalidChannel))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
