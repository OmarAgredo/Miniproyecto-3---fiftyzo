package com.project.fiftyzo.model;

import java.util.Objects;

/**
 * Immutable outcome of a played turn or an elimination.
 * Controllers use this value to update the UI and event log without mutating the model.
 */
public final class PlayResult {
    private final Player player;
    private final Card playedCard;
    private final int selectedValue;
    private final int previousSum;
    private final int newSum;
    private final boolean playerEliminated;
    private final boolean gameOver;
    private final Player winner;
    private final String message;

    /**
     * Creates a result object describing the effect of one game action.
     *
     * @param player player who acted or was eliminated
     * @param playedCard card that was played, or null for elimination
     * @param selectedValue selected card value, or zero for elimination
     * @param previousSum table sum before the action
     * @param newSum table sum after the action
     * @param playerEliminated whether the player was eliminated
     * @param gameOver whether this action ended the game
     * @param winner winner when the game is over, otherwise null
     * @param message human-readable event message
     */
    public PlayResult(Player player, Card playedCard, int selectedValue, int previousSum, int newSum,
                      boolean playerEliminated, boolean gameOver, Player winner, String message) {
        this.player = Objects.requireNonNull(player); this.playedCard = playedCard; this.selectedValue = selectedValue;
        this.previousSum = previousSum; this.newSum = newSum; this.playerEliminated = playerEliminated;
        this.gameOver = gameOver; this.winner = winner; this.message = Objects.requireNonNull(message);
    }
    public Player getPlayer() { return player; }
    public Card getPlayedCard() { return playedCard; }
    public int getSelectedValue() { return selectedValue; }
    public int getPreviousSum() { return previousSum; }
    public int getNewSum() { return newSum; }
    public boolean isPlayerEliminated() { return playerEliminated; }
    public boolean isGameOver() { return gameOver; }
    public Player getWinner() { return winner; }
    public String getMessage() { return message; }
    @Override public String toString() { return message; }
}
