package com.project.fiftyzo.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Base class for a game participant. */
public abstract class Player {
    private final String name;
    private final List<Card> hand = new ArrayList<>();
    private boolean active = true;
    protected Player(String name) { this.name = Objects.requireNonNull(name, "name"); }
    public String getName() { return name; }
    public void receiveCard(Card card) { hand.add(Objects.requireNonNull(card)); }
    public void receiveCards(Collection<Card> cards) { cards.forEach(this::receiveCard); }
    public boolean removeCard(Card card) { return hand.remove(card); }
    public void clearHand() { hand.clear(); }
    /** Returns an immutable snapshot of the cards currently held. */
    public List<Card> getHand() { return List.copyOf(hand); }
    public List<Card> getHandSnapshot() { return List.copyOf(hand); }
    public boolean isActive() { return active; }
    public void eliminate() { active = false; }
    public boolean hasPlayableCard(int currentSum) { return !getPlayableMoves(currentSum).isEmpty(); }
    public List<PlayableMove> getPlayableMoves(int currentSum) {
        List<PlayableMove> moves = new ArrayList<>();
        for (Card card : hand) moves.addAll(com.project.fiftyzo.util.CardValueResolver.getValidMovesForCard(card, currentSum));
        return moves;
    }
}
