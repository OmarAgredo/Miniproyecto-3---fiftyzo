package com.project.fiftyzo.model;

import com.project.fiftyzo.exception.EmptyDeckException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents the standard card deck used by the game model.
 * Cards are drawn from the front of an {@link ArrayDeque}, and recycled table
 * cards can be added back to the bottom when the deck is empty.
 */
public final class Deck {
    private final ArrayDeque<Card> cards = new ArrayDeque<>();

    /**
     * Creates and initializes a standard 52-card deck.
     */
    public Deck() { initializeStandardDeck(); }

    /**
     * Clears the current deck and rebuilds all rank and suit combinations.
     */
    public void initializeStandardDeck() {
        cards.clear();
        for (Suit suit : Suit.values()) for (Rank rank : Rank.values()) cards.addLast(new Card(rank, suit));
    }

    /**
     * Randomizes the current deck while preserving the same cards and size.
     */
    public void shuffle() { List<Card> shuffled = new ArrayList<>(cards); Collections.shuffle(shuffled); cards.clear(); cards.addAll(shuffled); }

    /**
     * Draws the next card from the top/front of the deck.
     *
     * @return the drawn card
     * @throws EmptyDeckException if no cards remain in the deck
     */
    public Card drawCard() throws EmptyDeckException {
        Card card = cards.pollFirst();
        if (card == null) throw new EmptyDeckException("The deck is empty.");
        return card;
    }

    /**
     * Adds recycled or returned cards to the bottom of the deck.
     *
     * @param newCards cards to append to the deck
     */
    public void addCardsToBottom(Collection<Card> newCards) { cards.addAll(newCards); }
    public boolean isEmpty() { return cards.isEmpty(); }
    public int size() { return cards.size(); }

    /**
     * Returns an immutable snapshot of the cards currently in deck order.
     *
     * @return immutable copy of the deck contents
     */
    public List<Card> getCardsSnapshot() { return List.copyOf(cards); }
}
