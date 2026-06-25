package com.project.fiftyzo.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.project.fiftyzo.exception.EmptyDeckException;
import com.project.fiftyzo.exception.InvalidMoveException;
import com.project.fiftyzo.exception.NoPlayableCardException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests the game lifecycle, legal turns, elimination, and deck recycling rules. */
class GameTest {
    @Test void machineCountBelowOneIsRejected() { assertThrows(IllegalArgumentException.class, () -> new Game(0)); }
    @Test void machineCountAboveThreeIsRejected() { assertThrows(IllegalArgumentException.class, () -> new Game(4)); }

    @Test
    void startingGameDealsHandsPlacesTableCardAndWaitsForHuman() {
        Game game = new Game(2);
        game.start();
        assertEquals(1, game.getHumanPlayer() == null ? 0 : 1);
        assertEquals(2, game.getMachinePlayers().size());
        assertTrue(game.getPlayers().stream().allMatch(player -> player.getHandSnapshot().size() == 4));
        assertEquals(1, game.getTable().getPlayedCardsSnapshot().size());
        assertNotNull(game.getTable().getTopCard());
        assertTrue(game.getTable().getCurrentSum() <= 50);
        assertEquals(GameStatus.WAITING_FOR_HUMAN_MOVE, game.getStatus());
    }

    @Test
    void validHumanMoveUpdatesTableSumAndRestoresHandSize() throws InvalidMoveException {
        Table table = tableAt(10);
        HumanPlayer human = humanWith(new Card(Rank.FIVE, Suit.HEARTS));
        Game game = controlledGame(table, human);

        PlayResult result = game.playHumanCard(human.getHandSnapshot().get(0), 5);

        assertEquals(10, result.getPreviousSum());
        assertEquals(15, result.getNewSum());
        assertEquals(15, game.getTable().getCurrentSum());
        assertEquals(4, human.getHandSnapshot().size());
    }

    @Test
    void moveThatExceedsFiftyIsRejected() {
        HumanPlayer human = humanWith(new Card(Rank.EIGHT, Suit.HEARTS));
        Game game = controlledGame(tableAt(45), human);
        assertThrows(InvalidMoveException.class, () -> game.playHumanCard(human.getHandSnapshot().get(0), 8));
    }

    @Test
    void aceCanBePlayedAsOneWhenLegal() throws InvalidMoveException {
        Card ace = new Card(Rank.ACE, Suit.HEARTS);
        HumanPlayer human = humanWith(ace);
        Game game = controlledGame(tableAt(49), human);
        game.playHumanCard(ace, 1);
        assertEquals(50, game.getTable().getCurrentSum());
    }

    @Test
    void aceCanBePlayedAsTenWhenLegal() throws InvalidMoveException {
        Card ace = new Card(Rank.ACE, Suit.HEARTS);
        HumanPlayer human = humanWith(ace);
        Game game = controlledGame(tableAt(40), human);
        game.playHumanCard(ace, 10);
        assertEquals(50, game.getTable().getCurrentSum());
    }

    @Test
    void aceCannotBePlayedAsTenWhenItWouldExceedFifty() {
        Card ace = new Card(Rank.ACE, Suit.HEARTS);
        HumanPlayer human = humanWith(ace);
        Game game = controlledGame(tableAt(45), human);
        assertThrows(InvalidMoveException.class, () -> game.playHumanCard(ace, 10));
    }

    @Test
    void validAceMoveWithThreeMachinesDoesNotEndGameOrDeclareHumanWinner() throws InvalidMoveException {
        Card ace = new Card(Rank.ACE, Suit.HEARTS);
        HumanPlayer human = humanWith(ace);
        MachinePlayer firstMachine = new MachinePlayer("Machine 1");
        MachinePlayer secondMachine = new MachinePlayer("Machine 2");
        MachinePlayer thirdMachine = new MachinePlayer("Machine 3");
        Game game = new Game(new Deck(), tableAt(48), human, List.of(firstMachine, secondMachine, thirdMachine));

        game.playHumanCard(ace, 1);

        assertEquals(49, game.getTable().getCurrentSum());
        assertFalse(game.isGameOver());
        assertNull(game.getWinner());
        assertEquals(4, game.getActivePlayers().size());
        assertSame(firstMachine, game.getCurrentPlayer());
        assertTrue(game.getMachinePlayers().stream().allMatch(Player::isActive));
    }

    @Test
    void reachingExactlyFiftyDoesNotEndGameWhenMachineStillActive() throws InvalidMoveException {
        Card ten = new Card(Rank.TEN, Suit.HEARTS);
        HumanPlayer human = humanWith(ten);
        MachinePlayer machine = machineWith(card(Rank.KING), card(Rank.TWO), card(Rank.THREE), card(Rank.FOUR));
        Game game = new Game(new Deck(), tableAt(40), human, List.of(machine));

        game.playHumanCard(ten, 10);

        assertEquals(50, game.getTable().getCurrentSum());
        assertFalse(game.isGameOver());
        assertNull(game.getWinner());
        assertSame(machine, game.getCurrentPlayer());
    }

    @Test
    void machineAtFiftyWithOnlyPositiveCardsIsEliminated() {
        HumanPlayer human = humanWith(card(Rank.KING));
        MachinePlayer machine = machineWith(card(Rank.TWO), card(Rank.THREE), card(Rank.FOUR), card(Rank.FIVE));
        Game game = new Game(new Deck(), tableAt(50), human, List.of(machine));
        game.advanceTurn();

        game.processNoPlayableCardForCurrentPlayer();

        assertFalse(machine.isActive());
        assertTrue(game.isGameOver());
        assertSame(human, game.getWinner());
    }

    @Test
    void machineAtFiftyWithNineOrFaceCardCanContinue() throws NoPlayableCardException, InvalidMoveException {
        HumanPlayer human = humanWith(card(Rank.TWO));
        MachinePlayer machine = machineWith(card(Rank.KING), card(Rank.QUEEN), card(Rank.JACK), card(Rank.NINE));
        Game game = new Game(new Deck(), tableAt(50), human, List.of(machine));
        game.advanceTurn();

        PlayResult result = game.playMachineCard();

        assertFalse(result.isPlayerEliminated());
        assertTrue(machine.isActive());
        assertFalse(game.isGameOver());
        assertNull(game.getWinner());
        assertTrue(game.getTable().getCurrentSum() <= 50);
        assertEquals(3, machine.getHandSnapshot().size());
    }

    @Test
    void playerWithoutAPlayableCardIsEliminatedAndRemainingPlayerWins() throws InvalidMoveException {
        HumanPlayer human = new HumanPlayer("Human");
        human.receiveCards(List.of(card(Rank.TWO), card(Rank.THREE), card(Rank.FOUR), card(Rank.FIVE)));
        MachinePlayer machine = new MachinePlayer("Machine");
        Game game = new Game(new Deck(), tableAt(50), human, List.of(machine));

        PlayResult result = game.playHumanCard(human.getHandSnapshot().get(0), 2);

        assertTrue(result.isPlayerEliminated());
        assertFalse(human.isActive());
        assertTrue(game.isGameOver());
        assertSame(machine, game.getWinner());
    }

    @Test
    void emptyDeckRecyclesEveryTableCardExceptTheTopCard() throws EmptyDeckException {
        Deck emptyDeck = new Deck();
        for (int index = 0; index < 52; index++) emptyDeck.drawCard();
        Table table = tableAt(40);
        Card topBeforeRecycle = table.getTopCard();
        Game game = new Game(emptyDeck, table, new HumanPlayer("Human"), List.of(new MachinePlayer("Machine")));

        game.recycleDeckIfNeeded();

        assertEquals(3, emptyDeck.size());
        assertEquals(1, table.getPlayedCardsSnapshot().size());
        assertSame(topBeforeRecycle, table.getTopCard());
    }

    private Game controlledGame(Table table, HumanPlayer human) {
        return new Game(new Deck(), table, human, List.of(new MachinePlayer("Machine")));
    }

    private HumanPlayer humanWith(Card firstCard) {
        HumanPlayer human = new HumanPlayer("Human");
        human.receiveCards(List.of(firstCard, card(Rank.TWO), card(Rank.THREE), card(Rank.FOUR)));
        return human;
    }

    private MachinePlayer machineWith(Card firstCard, Card secondCard, Card thirdCard, Card fourthCard) {
        MachinePlayer machine = new MachinePlayer("Machine");
        machine.receiveCards(List.of(firstCard, secondCard, thirdCard, fourthCard));
        return machine;
    }

    private Table tableAt(int targetSum) {
        Table table = new Table();
        table.placeInitialCard(card(Rank.TEN));
        try {
            if (targetSum >= 20) table.playCard(card(Rank.TEN), 10);
            if (targetSum >= 30) table.playCard(card(Rank.TEN), 10);
            if (targetSum >= 40) table.playCard(card(Rank.TEN), 10);
            if (targetSum == 45) table.playCard(card(Rank.FIVE), 5);
            if (targetSum == 48) table.playCard(card(Rank.EIGHT), 8);
            if (targetSum == 49) { table.playCard(card(Rank.EIGHT), 8); table.playCard(card(Rank.ACE), 1); }
            if (targetSum == 50) table.playCard(card(Rank.TEN), 10);
        } catch (InvalidMoveException exception) {
            throw new AssertionError("Test table setup should be valid.", exception);
        }
        return table;
    }

    private Card card(Rank rank) { return new Card(rank, Suit.SPADES); }
}
