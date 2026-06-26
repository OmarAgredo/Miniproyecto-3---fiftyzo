package com.project.fiftyzo.util;

import com.project.fiftyzo.model.Card;
import com.project.fiftyzo.model.PlayableMove;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for resolving and validating card values under Cincuentazo rules.
 * It centralizes card-value checks so model classes use the same rule set.
 */
public final class CardValueResolver {
    private CardValueResolver() { }

    /**
     * Returns every selectable Cincuentazo value for a card.
     *
     * @param card card to inspect
     * @return legal values for the card
     * @throws IllegalArgumentException if the card is null
     */
    public static List<Integer> getPossibleValues(Card card) {
        if (card == null) throw new IllegalArgumentException("Card cannot be null.");
        return card.getPossibleValues();
    }

    /**
     * Checks whether a value is one of the permitted values for the card.
     *
     * @param card card to validate
     * @param value selected value
     * @return true when the value belongs to the card
     */
    public static boolean isValidValueForCard(Card card, int value) { return card != null && getPossibleValues(card).contains(value); }

    /**
     * Produces the legal moves for one card at the supplied table sum.
     *
     * @param card card to evaluate
     * @param currentSum current table sum
     * @return legal moves that keep the table sum at or below the limit
     */
    public static List<PlayableMove> getValidMovesForCard(Card card, int currentSum) {
        List<PlayableMove> moves = new ArrayList<>();
        for (int value : getPossibleValues(card)) if (currentSum + value <= GameConstants.MAX_TABLE_SUM) moves.add(new PlayableMove(card, value, currentSum + value));
        return List.copyOf(moves);
    }

    /**
     * Returns the deterministic value used when a card initializes the table.
     * An Ace starts as one because it is the first value in its possible values.
     *
     * @param card initial table card
     * @return value applied to the starting table sum
     */
    public static int initialValue(Card card) { return getPossibleValues(card).get(0); }
}
