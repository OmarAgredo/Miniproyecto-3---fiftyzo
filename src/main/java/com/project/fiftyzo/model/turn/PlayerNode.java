package com.project.fiftyzo.model.turn;

import com.project.fiftyzo.model.Player;

/**
 * Internal link used by {@link CircularPlayerList}.
 * It stores a player and a reference to the next active player in turn order.
 */
final class PlayerNode {
    final Player player;
    PlayerNode next;

    /**
     * Creates a node for a player.
     *
     * @param player player stored in the circular list
     */
    PlayerNode(Player player) { this.player = player; }
}
