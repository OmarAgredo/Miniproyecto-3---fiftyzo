package com.project.fiftyzo.model.turn;

import com.project.fiftyzo.model.Player;
import java.util.List;
import java.util.Objects;

/**
 * Coordinates the active-player cycle for the game model.
 * It wraps the circular player list with turn-oriented operations used by {@code Game}.
 */
public final class TurnManager {
    private final CircularPlayerList players = new CircularPlayerList();

    /**
     * Creates a turn manager from the initial active players.
     *
     * @param players initial turn order
     */
    public TurnManager(List<? extends Player> players) { Objects.requireNonNull(players).forEach(this.players::add); }
    public Player getCurrentPlayer() { return players.getCurrentPlayer(); }

    /**
     * Advances to the next active player.
     */
    public void advanceTurn() { players.moveNext(); }

    /**
     * Removes and returns the current player from the active order.
     *
     * @return removed player, or null if the list is empty
     */
    public Player removeCurrentPlayer() { return players.removeCurrent(); }

    /**
     * Checks whether only one active player remains.
     *
     * @return true when the active turn cycle has one player
     */
    public boolean hasWinner() { return players.size() == 1 && players.getOnlyPlayer() != null; }
    public Player getWinner() { return hasWinner() ? players.getOnlyPlayer() : null; }

    /**
     * Returns the remaining active players in current turn order.
     *
     * @return immutable active-player list
     */
    public List<Player> getActivePlayers() { return players.toList(); }
}
