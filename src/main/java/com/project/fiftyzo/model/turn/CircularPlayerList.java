package com.project.fiftyzo.model.turn;

import com.project.fiftyzo.model.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Circular linked list that owns active-player turn order.
 * Removing the current node automatically advances the current pointer to the next player.
 */
public final class CircularPlayerList {
    private PlayerNode current;
    private int size;

    /**
     * Adds a player to the circular turn list.
     *
     * @param player player to add to the active order
     */
    public void add(Player player) {
        PlayerNode node = new PlayerNode(Objects.requireNonNull(player));
        if (current == null) { node.next = node; current = node; }
        else { PlayerNode tail = findTail(); node.next = current; tail.next = node; }
        size++;
    }
    public Player getCurrentPlayer() { return current == null ? null : current.player; }

    /**
     * Advances the current pointer to the next active player.
     */
    public void moveNext() { if (current != null) current = current.next; }

    /**
     * Removes the current node and makes its successor current.
     *
     * @return removed current player, or null when the list is empty
     */
    public Player removeCurrent() {
        if (current == null) return null;
        Player removed = current.player;
        if (size == 1) {
            current = null;
            size = 0;
            return removed;
        }
        PlayerNode successor = current.next;
        PlayerNode tail = findTail();
        tail.next = successor;
        current = successor;
        size--;
        return removed;
    }
    public int size() { return size; }
    public boolean hasOnlyOnePlayer() { return size == 1; }
    public Player getOnlyPlayer() { return size == 1 ? current.player : null; }

    /**
     * Returns the current active turn order as an immutable list.
     *
     * @return active players starting from the current player
     */
    public List<Player> toList() {
        List<Player> result = new ArrayList<>();
        if (current == null) return result;
        PlayerNode node = current;
        do { result.add(node.player); node = node.next; } while (node != current);
        return List.copyOf(result);
    }
    /**
     * Finds the node whose next reference points back to the current node.
     *
     * @return tail node of the circular list
     */
    private PlayerNode findTail() { PlayerNode tail = current; while (tail.next != current) tail = tail.next; return tail; }
}
