package de.exit.artnet2view;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Linux XWayland/OpenGL platform defaults in Main.
 */
class MainDefaultsTest {

    private String originalVsync;

    @AfterEach
    void restoreProperty() {
        if (originalVsync == null) {
            System.clearProperty("prism.vsync");
        } else {
            System.setProperty("prism.vsync", originalVsync);
        }
    }

    @Test
    @DisplayName("Linux disables double Prism vsync by default unless explicitly overridden")
    void testLinuxVsyncDefault() {
        originalVsync = System.getProperty("prism.vsync");
        System.clearProperty("prism.vsync");

        Main.applyPlatformDefaults("Linux");
        assertThat(System.getProperty("prism.vsync")).isEqualTo("false");

        System.setProperty("prism.vsync", "true");
        Main.applyPlatformDefaults("Linux");
        assertThat(System.getProperty("prism.vsync")).isEqualTo("true");
    }

    @Test
    @DisplayName("Windows does not alter default prism.vsync")
    void testWindowsLeavesVsyncUntouched() {
        originalVsync = System.getProperty("prism.vsync");
        System.clearProperty("prism.vsync");

        Main.applyPlatformDefaults("Windows 11");
        assertThat(System.getProperty("prism.vsync")).isNull();
    }
}
