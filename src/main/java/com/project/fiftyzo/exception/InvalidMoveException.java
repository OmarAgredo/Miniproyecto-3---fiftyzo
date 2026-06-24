package com.project.fiftyzo.exception;
/** Indicates a card value cannot legally be played. */
public class InvalidMoveException extends Exception { public InvalidMoveException(String message) { super(message); } }
