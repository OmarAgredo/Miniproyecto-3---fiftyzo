package com.project.fiftyzo.exception;

/**
 * Indicates that a game action was attempted after the game had already ended.
 * It represents an invalid gameplay state rather than a recoverable move choice.
 */
public class GameOverException extends IllegalStateException {
    /**
     * Creates an exception with a game-over state message.
     *
     * @param message explanation of the invalid post-game action
     */
    public GameOverException(String message) { super(message); }
}
