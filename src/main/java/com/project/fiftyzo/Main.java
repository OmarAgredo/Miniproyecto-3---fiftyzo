package com.project.fiftyzo;

import javafx.application.Application;

/** Plain JVM entry point used by IntelliJ and Maven. */
public final class Main {
    private Main() {
        throw new UnsupportedOperationException("Entry point class cannot be instantiated.");
    }

    /**
     * Launches the JavaFX application without making this class extend Application.
     *
     * @param args command-line arguments passed by the IDE or Maven.
     */
    public static void main(String[] args) {
        Application.launch(FiftyzoApplication.class, args);
    }
}
