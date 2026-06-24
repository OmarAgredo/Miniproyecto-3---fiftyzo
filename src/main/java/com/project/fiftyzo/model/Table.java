package com.project.fiftyzo.model;

import com.project.fiftyzo.exception.InvalidMoveException;
import com.project.fiftyzo.util.CardValueResolver;
import com.project.fiftyzo.util.GameConstants;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/** Played cards and their current arithmetic total. */
public final class Table {
    private final Deque<Card> playedCards = new ArrayDeque<>();
    private int currentSum;
    public void placeInitialCard(Card card) {
        if (!playedCards.isEmpty()) throw new IllegalStateException("An initial card has already been placed.");
        card = Objects.requireNonNull(card, "card");
        playedCards.addLast(card);
        currentSum = CardValueResolver.initialValue(card);
    }
    public void playCard(Card card, int selectedValue) throws InvalidMoveException {
        card = Objects.requireNonNull(card, "card");
        if (!CardValueResolver.isValidValueForCard(card, selectedValue)) throw new InvalidMoveException("The selected value is not valid for this card.");
        if (!canApplyValue(selectedValue)) throw new InvalidMoveException("The move would make the table sum exceed " + GameConstants.MAX_TABLE_SUM + ".");
        playedCards.addLast(card); currentSum += selectedValue;
    }
    public int getCurrentSum() { return currentSum; }
    public Card getTopCard() { return playedCards.peekLast(); }
    public List<Card> getPlayedCardsSnapshot() { return List.copyOf(playedCards); }
    public List<Card> removeRecycleCardsExceptTop() {
        List<Card> recycle = new ArrayList<>();
        while (playedCards.size() > 1) recycle.add(playedCards.removeFirst());
        return recycle;
    }
    public boolean canApplyValue(int value) { return currentSum + value <= GameConstants.MAX_TABLE_SUM; }
}
