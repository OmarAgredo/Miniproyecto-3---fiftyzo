package com.project.fiftyzo.model;
/**
 * Represents the user-controlled participant in the game model.
 * Human card choices are supplied by the JavaFX controller.
 */
public final class HumanPlayer extends Player {
    /**
     * Creates a human player with the supplied display name.
     *
     * @param name the player name shown in the interface and event log
     */
    public HumanPlayer(String name) { super(name); }
}
