package com.project.fiftyzo.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.project.fiftyzo.exception.EmptyDeckException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Tests standard deck construction, drawing, and recycling operations. */
class DeckTest {
    @Test void newDeckContainsFiftyTwoCards() { assertEquals(52, new Deck().size()); }

    @Test
    void drawingACardDecreasesDeckSize() throws EmptyDeckException {
        Deck deck = new Deck();
        deck.drawCard();
        assertEquals(51, deck.size());
    }

    @Test
    void drawingEveryCardEmptiesTheDeck() throws EmptyDeckException {
        Deck deck = new Deck();
        for (int index = 0; index < 52; index++) deck.drawCard();
        assertTrue(deck.isEmpty());
    }

    @Test
    void drawingFromAnEmptyDeckThrowsAnException() throws EmptyDeckException {
        Deck deck = new Deck();
        for (int index = 0; index < 52; index++) deck.drawCard();
        assertThrows(EmptyDeckException.class, deck::drawCard);
    }

    @Test
    void addingCardsToBottomIncreasesDeckSize() {
        Deck deck = new Deck();
        deck.addCardsToBottom(List.of(new Card(Rank.ACE, Suit.SPADES), new Card(Rank.KING, Suit.CLUBS)));
        assertEquals(54, deck.size());
    }

    @Test
    void newDeckContainsEverySuitAndRank() {
        List<Card> cards = new Deck().getCardsSnapshot();
        Set<Suit> suits = cards.stream().map(Card::getSuit).collect(Collectors.toSet());
        Set<Rank> ranks = cards.stream().map(Card::getRank).collect(Collectors.toSet());
        assertEquals(Set.of(Suit.values()), suits);
        assertEquals(Set.of(Rank.values()), ranks);
    }

    @Test
    void shufflePreservesTheSameCardsAndSize() {
        Deck deck = new Deck();
        Set<String> before = identities(deck.getCardsSnapshot());
        deck.shuffle();
        assertEquals(52, deck.size());
        assertEquals(before, identities(deck.getCardsSnapshot()));
        assertFalse(deck.isEmpty());
    }

    private Set<String> identities(List<Card> cards) { return cards.stream().map(card -> card.getRank() + ":" + card.getSuit()).collect(Collectors.toSet()); }
}
