package de.exit.artnet2view;

import javafx.application.Application;

/**
 * Standard Java main launcher avoiding module/JavaFX classpath issues.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Application.launch(Artnet2viewApplication.class, args);
    }
}
