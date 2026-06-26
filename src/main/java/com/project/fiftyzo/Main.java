package com.project.fiftyzo;

import javafx.application.Application;

/**
 * Plain JVM entry point used by IntelliJ and Maven.
 * It delegates startup to {@link FiftyzoApplication} without extending JavaFX {@code Application}.
 */
public final class Main {
    /**
     * Prevents instantiation of the launcher class.
     */
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
