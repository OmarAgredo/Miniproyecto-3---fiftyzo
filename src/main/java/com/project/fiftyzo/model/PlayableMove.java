package com.project.fiftyzo.model;

import java.util.Objects;

/** One legal card and value selection. */
public final class PlayableMove {
    private final Card card;
    private final int value;
    private final int resultingSum;
    public PlayableMove(Card card, int value, int resultingSum) { this.card = Objects.requireNonNull(card); this.value = value; this.resultingSum = resultingSum; }
    public Card getCard() { return card; }
    public int getValue() { return value; }
    public int getResultingSum() { return resultingSum; }
    @Override public String toString() { return card + " (" + value + ") -> " + resultingSum; }
}
