package com.project.fiftyzo.util;

/**
 * Shared configuration values for the 50ZO rules.
 * These constants keep rule limits consistent across the model and controllers.
 */
public final class GameConstants {
    public static final int MAX_TABLE_SUM = 50;
    public static final int INITIAL_HAND_SIZE = 4;
    public static final int MIN_MACHINE_PLAYERS = 1;
    public static final int MAX_MACHINE_PLAYERS = 3;

    /**
     * Prevents instantiation of the constants holder.
     */
    private GameConstants() { }
}
