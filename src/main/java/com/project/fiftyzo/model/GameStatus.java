package com.project.fiftyzo.model;
/**
 * Represents the lifecycle and current-turn state of a {@link Game}.
 * Controllers use this value to distinguish setup, human turns, machine turns, and game completion.
 */
public enum GameStatus { NOT_STARTED, IN_PROGRESS, WAITING_FOR_HUMAN_MOVE, MACHINE_TURN, GAME_OVER }
