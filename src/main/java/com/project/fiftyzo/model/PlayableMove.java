package com.project.fiftyzo.model;

import java.util.Objects;

/**
 * Describes one legal card-value option for the current table sum.
 * Machine strategies and player validation use this object to compare possible plays.
 */
public final class PlayableMove {
    private final Card card;
    private final int value;
    private final int resultingSum;

    /**
     * Creates a playable move snapshot.
     *
     * @param card card that can be played
     * @param value selected value for the card
     * @param resultingSum table sum after applying the value
     */
    public PlayableMove(Card card, int value, int resultingSum) { this.card = Objects.requireNonNull(card); this.value = value; this.resultingSum = resultingSum; }
    public Card getCard() { return card; }
    public int getValue() { return value; }
    public int getResultingSum() { return resultingSum; }
    @Override public String toString() { return card + " (" + value + ") -> " + resultingSum; }
}
