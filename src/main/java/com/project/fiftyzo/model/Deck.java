package com.project.fiftyzo.model;

import com.project.fiftyzo.exception.EmptyDeckException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** A shuffled standard deck, with draws taken from its front. */
public final class Deck {
    private final ArrayDeque<Card> cards = new ArrayDeque<>();
    public Deck() { initializeStandardDeck(); }
    public void initializeStandardDeck() {
        cards.clear();
        for (Suit suit : Suit.values()) for (Rank rank : Rank.values()) cards.addLast(new Card(rank, suit));
    }
    public void shuffle() { List<Card> shuffled = new ArrayList<>(cards); Collections.shuffle(shuffled); cards.clear(); cards.addAll(shuffled); }
    public Card drawCard() throws EmptyDeckException {
        Card card = cards.pollFirst();
        if (card == null) throw new EmptyDeckException("The deck is empty.");
        return card;
    }
    public void addCardsToBottom(Collection<Card> newCards) { cards.addAll(newCards); }
    public boolean isEmpty() { return cards.isEmpty(); }
    public int size() { return cards.size(); }
    public List<Card> getCardsSnapshot() { return List.copyOf(cards); }
}
