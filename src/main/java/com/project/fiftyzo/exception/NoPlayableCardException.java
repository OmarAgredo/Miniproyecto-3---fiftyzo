package com.project.fiftyzo.exception;

/**
 * Indicates that a player, usually a machine player, has no legal card to play.
 * The game flow handles this state by eliminating the player.
 */
public class NoPlayableCardException extends Exception {
    /**
     * Creates an exception with a no-move explanation.
     *
     * @param message explanation of why no move can be selected
     */
    public NoPlayableCardException(String message) { super(message); }
}
