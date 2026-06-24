package com.project.fiftyzo.exception;
/** Indicates an action was attempted after the game ended. */
public class GameOverException extends IllegalStateException { public GameOverException(String message) { super(message); } }
