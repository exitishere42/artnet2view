package de.exit.artnet2rig;

import javafx.application.Application;

/**
 * Standard Java main launcher avoiding module/JavaFX classpath issues.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Application.launch(Artnet2rigApplication.class, args);
    }
}
