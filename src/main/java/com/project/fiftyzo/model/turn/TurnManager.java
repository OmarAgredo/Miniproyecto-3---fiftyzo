package com.project.fiftyzo.model.turn;

import com.project.fiftyzo.model.Player;
import java.util.List;
import java.util.Objects;

/** Coordinates the active-player cycle. */
public final class TurnManager {
    private final CircularPlayerList players = new CircularPlayerList();
    public TurnManager(List<? extends Player> players) { Objects.requireNonNull(players).forEach(this.players::add); }
    public Player getCurrentPlayer() { return players.getCurrentPlayer(); }
    public void advanceTurn() { players.moveNext(); }
    public Player removeCurrentPlayer() { return players.removeCurrent(); }
    public boolean hasWinner() { return players.hasOnlyOnePlayer(); }
    public Player getWinner() { return players.getOnlyPlayer(); }
    public List<Player> getActivePlayers() { return players.toList(); }
}
