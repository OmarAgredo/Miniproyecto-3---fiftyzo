package com.project.fiftyzo.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests the Cincuentazo value rules represented by {@link Card}. */
class CardTest {
    @Test
    void numberCardsFromTwoToEightReturnTheirNumericValue() {
        assertEquals(List.of(2), card(Rank.TWO).getPossibleValues());
        assertEquals(List.of(3), card(Rank.THREE).getPossibleValues());
        assertEquals(List.of(8), card(Rank.EIGHT).getPossibleValues());
    }

    @Test void tenReturnsTen() { assertEquals(List.of(10), card(Rank.TEN).getPossibleValues()); }
    @Test void nineReturnsZero() { assertEquals(List.of(0), card(Rank.NINE).getPossibleValues()); }

    @Test
    void faceCardsReturnNegativeTen() {
        assertEquals(List.of(-10), card(Rank.JACK).getPossibleValues());
        assertEquals(List.of(-10), card(Rank.QUEEN).getPossibleValues());
        assertEquals(List.of(-10), card(Rank.KING).getPossibleValues());
    }

    @Test void aceReturnsOneAndTen() { assertEquals(List.of(1, 10), card(Rank.ACE).getPossibleValues()); }

    @Test
    void isAceIsTrueOnlyForAnAce() {
        assertTrue(card(Rank.ACE).isAce());
        assertFalse(card(Rank.KING).isAce());
    }

    @Test
    void isNineIsTrueOnlyForANine() {
        assertTrue(card(Rank.NINE).isNine());
        assertFalse(card(Rank.TEN).isNine());
    }

    @Test
    void isFaceCardIsTrueOnlyForJackQueenAndKing() {
        assertTrue(card(Rank.JACK).isFaceCard());
        assertTrue(card(Rank.QUEEN).isFaceCard());
        assertTrue(card(Rank.KING).isFaceCard());
        assertFalse(card(Rank.ACE).isFaceCard());
    }

    private Card card(Rank rank) { return new Card(rank, Suit.HEARTS); }
}
