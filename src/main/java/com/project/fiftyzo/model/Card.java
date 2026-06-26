package com.project.fiftyzo.model;

import java.util.List;

/**
 * Represents an immutable playing card in the model layer.
 * The card stores its rank and suit; Cincuentazo values are derived from the rank.
 */
public final class Card {
    private final Rank rank;
    private final Suit suit;

    /**
     * Creates a card with the supplied rank and suit.
     *
     * @param rank the card rank
     * @param suit the card suit
     */
    public Card(Rank rank, Suit suit) { this.rank = java.util.Objects.requireNonNull(rank); this.suit = java.util.Objects.requireNonNull(suit); }
    public Rank getRank() { return rank; }
    public Suit getSuit() { return suit; }

    /**
     * Resolves every value this card can contribute to the table sum.
     *
     * @return immutable list of legal values for this card
     */
    public List<Integer> getPossibleValues() {
        return switch (rank) {
            case ACE -> List.of(1, 10);
            case JACK, QUEEN, KING -> List.of(-10);
            case NINE -> List.of(0);
            case TEN -> List.of(10);
            default -> List.of(rank.ordinal() + 2);
        };
    }

    /**
     * Checks whether this card is an Ace, which can be played as 1 or 10.
     *
     * @return true when the card rank is Ace
     */
    public boolean isAce() { return rank == Rank.ACE; }

    /**
     * Checks whether this card is a Nine, which adds zero to the table sum.
     *
     * @return true when the card rank is Nine
     */
    public boolean isNine() { return rank == Rank.NINE; }

    /**
     * Checks whether this card is a Jack, Queen, or King.
     *
     * @return true when the card is a face card
     */
    public boolean isFaceCard() { return rank == Rank.JACK || rank == Rank.QUEEN || rank == Rank.KING; }
    @Override public String toString() { return rank + " of " + suit; }
}
