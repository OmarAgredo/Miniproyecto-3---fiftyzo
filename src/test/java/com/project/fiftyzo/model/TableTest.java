package com.project.fiftyzo.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.project.fiftyzo.exception.InvalidMoveException;
import org.junit.jupiter.api.Test;

/** Tests table sum boundary behavior. */
class TableTest {
    @Test
    void resultingSumEqualToFiftyIsValid() throws InvalidMoveException {
        Table table = tableAtForty();

        assertTrue(table.canApplyValue(10));

        table.playCard(new Card(Rank.TEN, Suit.HEARTS), 10);

        assertEquals(50, table.getCurrentSum());
    }

    @Test
    void resultingSumGreaterThanFiftyIsInvalid() throws InvalidMoveException {
        Table table = tableAtForty();
        table.playCard(new Card(Rank.TEN, Suit.HEARTS), 10);

        assertFalse(table.canApplyValue(1));
        assertThrows(InvalidMoveException.class, () -> table.playCard(new Card(Rank.ACE, Suit.SPADES), 1));
    }

    private Table tableAtForty() throws InvalidMoveException {
        Table table = new Table();
        table.placeInitialCard(new Card(Rank.TEN, Suit.SPADES));
        table.playCard(new Card(Rank.TEN, Suit.HEARTS), 10);
        table.playCard(new Card(Rank.TEN, Suit.DIAMONDS), 10);
        table.playCard(new Card(Rank.TEN, Suit.CLUBS), 10);
        return table;
    }
}
