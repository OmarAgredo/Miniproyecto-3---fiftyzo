package com.project.fiftyzo.util;

import com.project.fiftyzo.model.Card;
import com.project.fiftyzo.model.PlayableMove;
import java.util.ArrayList;
import java.util.List;

/** Resolves and validates the selectable value of a card. */
public final class CardValueResolver {
    private CardValueResolver() { }
    /** Returns every selectable Cincuentazo value for a card. */
    public static List<Integer> getPossibleValues(Card card) {
        if (card == null) throw new IllegalArgumentException("Card cannot be null.");
        return card.getPossibleValues();
    }
    /** Returns whether a value is one of the permitted values for the card. */
    public static boolean isValidValueForCard(Card card, int value) { return card != null && getPossibleValues(card).contains(value); }
    /** Produces the legal moves for one card at the supplied table sum. */
    public static List<PlayableMove> getValidMovesForCard(Card card, int currentSum) {
        List<PlayableMove> moves = new ArrayList<>();
        for (int value : getPossibleValues(card)) if (currentSum + value <= GameConstants.MAX_TABLE_SUM) moves.add(new PlayableMove(card, value, currentSum + value));
        return List.copyOf(moves);
    }
    /** Returns the deterministic initial-table value; an ace starts as one. */
    public static int initialValue(Card card) { return getPossibleValues(card).get(0); }
}
