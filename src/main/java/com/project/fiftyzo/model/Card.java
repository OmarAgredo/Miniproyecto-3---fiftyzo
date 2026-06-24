package com.project.fiftyzo.model;

import java.util.List;

/** An immutable playing card with its Cincuentazo values. */
public final class Card {
    private final Rank rank;
    private final Suit suit;
    public Card(Rank rank, Suit suit) { this.rank = java.util.Objects.requireNonNull(rank); this.suit = java.util.Objects.requireNonNull(suit); }
    public Rank getRank() { return rank; }
    public Suit getSuit() { return suit; }
    public List<Integer> getPossibleValues() {
        return switch (rank) {
            case ACE -> List.of(1, 10);
            case JACK, QUEEN, KING -> List.of(-10);
            case NINE -> List.of(0);
            case TEN -> List.of(10);
            default -> List.of(rank.ordinal() + 2);
        };
    }
    public boolean isAce() { return rank == Rank.ACE; }
    public boolean isNine() { return rank == Rank.NINE; }
    public boolean isFaceCard() { return rank == Rank.JACK || rank == Rank.QUEEN || rank == Rank.KING; }
    @Override public String toString() { return rank + " of " + suit; }
}
