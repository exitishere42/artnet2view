package de.exit.artnet2view.service;

import de.exit.artnet2view.core.artnet.DmxUniverseBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ArtNetService lifecycle and buffer linking.
 */
class ArtNetServiceTest {

    @Test
    @DisplayName("ArtNetService creates and manages DmxUniverseBuffer")
    void testServiceBuffer() {
        ArtNetService service = new ArtNetService(new DmxUniverseBuffer(), 6455); // Use alternate port for unit tests
        assertThat(service.getBuffer()).isNotNull();
        assertThat(service.getPort()).isEqualTo(6455);
        assertThat(service.isRunning()).isFalse();

        service.start();
        assertThat(service.isRunning()).isTrue();

        service.stop();
        assertThat(service.isRunning()).isFalse();
    }
}
