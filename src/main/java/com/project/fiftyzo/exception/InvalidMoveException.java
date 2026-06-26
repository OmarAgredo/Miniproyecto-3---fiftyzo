package com.project.fiftyzo.exception;

/**
 * Indicates that a selected card value cannot legally be played.
 * This includes values that do not belong to the card and moves that would exceed 50.
 */
public class InvalidMoveException extends Exception {
    /**
     * Creates an exception with a move-validation message.
     *
     * @param message explanation of why the move is invalid
     */
    public InvalidMoveException(String message) { super(message); }
}
