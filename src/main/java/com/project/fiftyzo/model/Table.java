package com.project.fiftyzo.model;

import com.project.fiftyzo.exception.InvalidMoveException;
import com.project.fiftyzo.util.CardValueResolver;
import com.project.fiftyzo.util.GameConstants;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Tracks the played-card pile and current Cincuentazo table sum.
 * The table validates each selected card value before applying it.
 */
public final class Table {
    private final Deque<Card> playedCards = new ArrayDeque<>();
    private int currentSum;

    /**
     * Places the first card on the table and initializes the current sum.
     *
     * @param card the initial table card
     * @throws IllegalStateException if an initial card was already placed
     */
    public void placeInitialCard(Card card) {
        if (!playedCards.isEmpty()) throw new IllegalStateException("An initial card has already been placed.");
        card = Objects.requireNonNull(card, "card");
        playedCards.addLast(card);
        currentSum = CardValueResolver.initialValue(card);
    }

    /**
     * Applies a played card value to the table if it is legal.
     *
     * @param card the card being played
     * @param selectedValue the value selected for that card
     * @throws InvalidMoveException if the value does not belong to the card or would exceed the table limit
     */
    public void playCard(Card card, int selectedValue) throws InvalidMoveException {
        card = Objects.requireNonNull(card, "card");
        if (!CardValueResolver.isValidValueForCard(card, selectedValue)) throw new InvalidMoveException("The selected value is not valid for this card.");
        if (!canApplyValue(selectedValue)) throw new InvalidMoveException("The move would make the table sum exceed " + GameConstants.MAX_TABLE_SUM + ".");
        playedCards.addLast(card); currentSum += selectedValue;
    }
    public int getCurrentSum() { return currentSum; }
    public Card getTopCard() { return playedCards.peekLast(); }

    /**
     * Returns the played cards without exposing the mutable pile.
     *
     * @return immutable snapshot of the played-card pile
     */
    public List<Card> getPlayedCardsSnapshot() { return List.copyOf(playedCards); }

    /**
     * Removes all played cards except the visible top card for deck recycling.
     *
     * @return cards removed from the bottom of the table pile
     */
    public List<Card> removeRecycleCardsExceptTop() {
        List<Card> recycle = new ArrayList<>();
        while (playedCards.size() > 1) recycle.add(playedCards.removeFirst());
        return recycle;
    }

    /**
     * Checks whether applying the supplied value would keep the table sum legal.
     *
     * @param value value to test
     * @return true when the resulting sum is at or below the maximum table sum
     */
    public boolean canApplyValue(int value) { return currentSum + value <= GameConstants.MAX_TABLE_SUM; }
}
