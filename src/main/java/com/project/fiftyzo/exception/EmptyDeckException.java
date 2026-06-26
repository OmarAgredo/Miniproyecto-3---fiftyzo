package com.project.fiftyzo.exception;

/**
 * Indicates that the deck cannot provide a card when a draw is requested.
 * The game model may attempt table-card recycling before this exception is allowed to surface.
 */
public class EmptyDeckException extends Exception {
    /**
     * Creates an exception with a gameplay-focused error message.
     *
     * @param message explanation of the empty-deck condition
     */
    public EmptyDeckException(String message) { super(message); }
}
