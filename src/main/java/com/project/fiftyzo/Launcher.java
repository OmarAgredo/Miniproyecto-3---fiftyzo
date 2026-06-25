package com.project.fiftyzo;

/**
 * Standard launcher used by IDE run configurations.
 * This class intentionally does not extend {@code javafx.application.Application}.
 */
public final class Launcher {
    private Launcher() {
        throw new UnsupportedOperationException("Utility launcher class cannot be instantiated.");
    }

    /**
     * Starts the JavaFX application through the real Application class.
     *
     * @param args command-line arguments passed by the IDE or Maven.
     */
    public static void main(String[] args) {
        Main.main(args);
    }
}
