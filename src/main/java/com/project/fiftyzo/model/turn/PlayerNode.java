package com.project.fiftyzo.model.turn;

import com.project.fiftyzo.model.Player;

/** Link used internally by {@link CircularPlayerList}. */
final class PlayerNode {
    final Player player;
    PlayerNode next;
    PlayerNode(Player player) { this.player = player; }
}
