package de.exit.artnet2view;

import javafx.application.Application;

import java.util.Locale;

/**
 * Standard Java main launcher avoiding module/JavaFX classpath issues and applying OS-specific GPU defaults.
 */
public final class Main {

    private Main() {
    }

    /**
     * Configures JavaFX Prism defaults before toolkit startup.
     * On Linux (especially XWayland + Mesa), disabling redundant Prism VSync prevents double-buffering frame stalls.
     *
     * @param osName operating system name ({@code System.getProperty("os.name")})
     */
    static void applyPlatformDefaults(String osName) {
        if (osName != null && osName.toLowerCase(Locale.ROOT).contains("linux")) {
            if (System.getProperty("prism.vsync") == null) {
                System.setProperty("prism.vsync", "false");
            }
        }
    }

    public static void main(String[] args) {
        applyPlatformDefaults(System.getProperty("os.name", ""));
        Application.launch(Artnet2viewApplication.class, args);
    }
}
