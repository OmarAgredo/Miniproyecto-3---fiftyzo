package com.project.fiftyzo.model.turn;

import com.project.fiftyzo.model.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Circular linked list that owns active-player turn order. */
public final class CircularPlayerList {
    private PlayerNode current;
    private int size;
    public void add(Player player) {
        PlayerNode node = new PlayerNode(Objects.requireNonNull(player));
        if (current == null) { node.next = node; current = node; }
        else { PlayerNode tail = findTail(); node.next = current; tail.next = node; }
        size++;
    }
    public Player getCurrentPlayer() { return current == null ? null : current.player; }
    public void moveNext() { if (current != null) current = current.next; }
    /** Removes the current node and makes its successor current. */
    public Player removeCurrent() {
        if (current == null) return null;
        Player removed = current.player;
        if (size == 1) current = null;
        else { PlayerNode tail = findTail(); tail.next = current.next; current = current.next; }
        size--; return removed;
    }
    public int size() { return size; }
    public boolean hasOnlyOnePlayer() { return size == 1; }
    public Player getOnlyPlayer() { return size == 1 ? current.player : null; }
    public List<Player> toList() {
        List<Player> result = new ArrayList<>();
        if (current == null) return result;
        PlayerNode node = current;
        do { result.add(node.player); node = node.next; } while (node != current);
        return List.copyOf(result);
    }
    private PlayerNode findTail() { PlayerNode tail = current; while (tail.next != current) tail = tail.next; return tail; }
}
