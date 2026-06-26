package com.project.fiftyzo.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Base model class for a game participant.
 * It owns the player's hand, active/eliminated state, and legal move discovery.
 */
public abstract class Player {
    private final String name;
    private final List<Card> hand = new ArrayList<>();
    private boolean active = true;

    /**
     * Creates a player with a non-null display name.
     *
     * @param name the player name used by logs and UI labels
     */
    protected Player(String name) { this.name = Objects.requireNonNull(name, "name"); }
    public String getName() { return name; }

    /**
     * Adds one card to this player's hand.
     *
     * @param card card to receive
     */
    public void receiveCard(Card card) { hand.add(Objects.requireNonNull(card)); }

    /**
     * Adds multiple cards to this player's hand in iteration order.
     *
     * @param cards cards to receive
     */
    public void receiveCards(Collection<Card> cards) { cards.forEach(this::receiveCard); }

    /**
     * Removes a played or transferred card from this player's hand.
     *
     * @param card card to remove
     * @return true when the card was present and removed
     */
    public boolean removeCard(Card card) { return hand.remove(card); }

    /**
     * Removes every card from the hand, usually during elimination.
     */
    public void clearHand() { hand.clear(); }
    /** Returns an immutable snapshot of the cards currently held. */
    public List<Card> getHand() { return List.copyOf(hand); }
    public List<Card> getHandSnapshot() { return List.copyOf(hand); }
    public boolean isActive() { return active; }

    /**
     * Marks the player as eliminated from the active turn cycle.
     */
    public void eliminate() { active = false; }

    /**
     * Checks whether at least one card can be legally played at the current sum.
     *
     * @param currentSum current table sum
     * @return true if any legal move exists
     */
    public boolean hasPlayableCard(int currentSum) { return !getPlayableMoves(currentSum).isEmpty(); }

    /**
     * Builds all legal moves available from the current hand.
     *
     * @param currentSum current table sum
     * @return list of playable moves
     */
    public List<PlayableMove> getPlayableMoves(int currentSum) {
        List<PlayableMove> moves = new ArrayList<>();
        for (Card card : hand) moves.addAll(com.project.fiftyzo.util.CardValueResolver.getValidMovesForCard(card, currentSum));
        return moves;
    }
}
