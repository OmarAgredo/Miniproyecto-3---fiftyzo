package com.project.fiftyzo.model;

import com.project.fiftyzo.exception.EmptyDeckException;
import com.project.fiftyzo.exception.GameOverException;
import com.project.fiftyzo.exception.InvalidMoveException;
import com.project.fiftyzo.exception.NoPlayableCardException;
import com.project.fiftyzo.model.turn.TurnManager;
import com.project.fiftyzo.util.GameConstants;
import java.util.ArrayList;
import java.util.List;

/**
 * Central, UI-independent implementation of the 50ZO game rules.
 * This model owns the deck, table, players, turn manager, elimination flow,
 * and winner detection while remaining independent from JavaFX.
 */
public final class Game {
    private Deck deck;
    private Table table;
    private TurnManager turnManager;
    private GameStatus status = GameStatus.NOT_STARTED;
    private final HumanPlayer humanPlayer;
    private final List<MachinePlayer> machinePlayers;

    /**
     * Creates a new game with one human player and the requested machine players.
     *
     * @param machineCount number of machine opponents, from 1 to 3
     * @throws IllegalArgumentException if the machine count is outside the supported range
     */
    public Game(int machineCount) {
        if (machineCount < GameConstants.MIN_MACHINE_PLAYERS || machineCount > GameConstants.MAX_MACHINE_PLAYERS) {
            throw new IllegalArgumentException("Machine player count must be between 1 and 3.");
        }
        humanPlayer = new HumanPlayer("Human Player");
        machinePlayers = new ArrayList<>();
        for (int i = 1; i <= machineCount; i++) machinePlayers.add(new MachinePlayer("Machine " + i));
    }

    /**
     * Creates an already initialized game state for package-level model tests.
     * The supplied table must already contain its initial card.
     */
    Game(Deck deck, Table table, HumanPlayer humanPlayer, List<MachinePlayer> machinePlayers) {
        this.deck = java.util.Objects.requireNonNull(deck, "deck");
        this.table = java.util.Objects.requireNonNull(table, "table");
        this.humanPlayer = java.util.Objects.requireNonNull(humanPlayer, "humanPlayer");
        this.machinePlayers = new ArrayList<>(java.util.Objects.requireNonNull(machinePlayers, "machinePlayers"));
        List<Player> players = new ArrayList<>();
        players.add(humanPlayer);
        players.addAll(machinePlayers);
        this.turnManager = new TurnManager(players);
        this.status = GameStatus.IN_PROGRESS;
        updateTurnStatus();
    }

    /**
     * Initializes, shuffles, deals, places the first table card, and prepares the first turn.
     *
     * @throws IllegalStateException if the game has already been started
     */
    public void start() {
        if (status != GameStatus.NOT_STARTED) throw new IllegalStateException("A game can only be started once.");
        deck = new Deck(); deck.shuffle(); table = new Table();
        List<Player> players = new ArrayList<>(); players.add(humanPlayer); players.addAll(machinePlayers);
        turnManager = new TurnManager(players);
        status = GameStatus.IN_PROGRESS;
        try {
            dealInitialCards();
            placeInitialTableCard();
        } catch (EmptyDeckException exception) {
            throw new IllegalStateException("A standard deck could not initialize the game.", exception);
        }
        updateTurnStatus();
    }

    /**
     * Deals the starting hand to every player.
     *
     * @throws EmptyDeckException if the deck cannot provide enough cards
     */
    public void dealInitialCards() throws EmptyDeckException {
        ensureStarted();
        for (int i = 0; i < GameConstants.INITIAL_HAND_SIZE; i++) for (Player player : getPlayers()) player.receiveCard(deck.drawCard());
    }

    /**
     * Draws and places the first table card.
     *
     * @throws EmptyDeckException if the deck is empty
     */
    public void placeInitialTableCard() throws EmptyDeckException { ensureStarted(); table.placeInitialCard(deck.drawCard()); }

    /**
     * Attempts to play a selected card for the human player.
     *
     * @param card card selected by the human player
     * @param selectedValue selected value for the card, especially relevant for Aces
     * @return result describing the move or elimination
     * @throws InvalidMoveException if it is not the human turn or the move is illegal
     * @throws GameOverException if the game has already ended
     */
    public PlayResult playHumanCard(Card card, int selectedValue) throws InvalidMoveException, GameOverException {
        ensureActionable();
        if (getCurrentPlayer() != humanPlayer) throw new InvalidMoveException("It is not the human player's turn.");
        if (!humanPlayer.hasPlayableCard(table.getCurrentSum())) return eliminateForNoMove();
        return playCurrentCardSafely(card, selectedValue);
    }

    /**
     * Performs a complete machine turn, including choosing a card, drawing, and advancing.
     *
     * @return result of the machine play
     * @throws NoPlayableCardException if no machine move can be selected
     * @throws InvalidMoveException if the selected strategy move is rejected by the rules
     * @throws GameOverException if the game has already ended
     */
    public PlayResult playMachineTurn() throws NoPlayableCardException, InvalidMoveException, GameOverException {
        PlayResult playResult = playMachineCard();
        if (playResult.isPlayerEliminated()) return playResult;
        try {
            drawCardForCurrentPlayer();
        } catch (EmptyDeckException exception) {
            throw new IllegalStateException("No card was available after deck recycling.", exception);
        }
        advanceTurn();
        return new PlayResult(playResult.getPlayer(), playResult.getPlayedCard(), playResult.getSelectedValue(),
                playResult.getPreviousSum(), playResult.getNewSum(), false, isGameOver(), getWinner(), playResult.getMessage());
    }

    /**
     * Plays the machine's chosen card without drawing or advancing the turn.
     * This supports timed presentation layers while keeping game rules in the model.
     *
     * @return result of the machine card play
     * @throws NoPlayableCardException if the machine has no legal move
     * @throws InvalidMoveException if the chosen move is invalid
     * @throws GameOverException if the game has already ended
     */
    public PlayResult playMachineCard() throws NoPlayableCardException, InvalidMoveException, GameOverException {
        ensureActionable();
        if (!(getCurrentPlayer() instanceof MachinePlayer machine)) throw new InvalidMoveException("It is not a machine player's turn.");
        if (!machine.hasPlayableCard(table.getCurrentSum())) return eliminateForNoMove();
        PlayableMove move = machine.chooseMove(table.getCurrentSum());
        return playCardWithoutDrawing(move.getCard(), move.getValue());
    }

    /**
     * Draws one card for the current player, recycling table cards first if necessary.
     *
     * @throws EmptyDeckException if no card can be drawn after recycling
     */
    public void drawCardForCurrentPlayer() throws EmptyDeckException {
        ensureStarted(); recycleDeckIfNeeded(); getCurrentPlayer().receiveCard(deck.drawCard());
    }

    /**
     * Rebuilds the deck from older table cards when the deck is empty.
     * The visible top table card remains on the table.
     */
    public void recycleDeckIfNeeded() {
        ensureStarted();
        if (deck.isEmpty()) { List<Card> recycle = table.removeRecycleCardsExceptTop(); deck.addCardsToBottom(recycle); deck.shuffle(); }
    }
    /**
     * Eliminates the current player and removes them from the active turn cycle.
     *
     * @throws GameOverException if elimination is requested after the game has ended
     */
    public void eliminateCurrentPlayer() {
        ensureStarted();
        if (isGameOver()) throw new GameOverException("The game is over.");
        Player player = getCurrentPlayer();
        deck.addCardsToBottom(player.getHandSnapshot()); player.clearHand(); player.eliminate(); turnManager.removeCurrentPlayer();
        if (isGameOver()) status = GameStatus.GAME_OVER; else updateTurnStatus();
    }
    /**
     * Processes the current player's lack of legal moves by eliminating them.
     */
    public void processNoPlayableCardForCurrentPlayer() { eliminateCurrentPlayer(); }

    /**
     * Moves the turn pointer to the next active player when the game is still running.
     */
    public void advanceTurn() { ensureStarted(); if (!isGameOver()) { turnManager.advanceTurn(); updateTurnStatus(); } }
    /**
     * Returns whether exactly one active player remains in the turn cycle.
     *
     * @return true when the game has a single active winner
     */
    public boolean isGameOver() {
        return turnManager != null && turnManager.hasWinner() && turnManager.getActivePlayers().size() == 1;
    }

    /**
     * Returns the sole active player only after the game is over.
     *
     * @return the winner, or null while the game is still in progress
     */
    public Player getWinner() { return isGameOver() ? turnManager.getWinner() : null; }
    public Player getCurrentPlayer() { return turnManager == null ? null : turnManager.getCurrentPlayer(); }
    public Deck getDeck() { return deck; }
    public Table getTable() { return table; }
    public HumanPlayer getHumanPlayer() { return humanPlayer; }
    public List<MachinePlayer> getMachinePlayers() { return List.copyOf(machinePlayers); }
    public List<Player> getPlayers() { List<Player> players = new ArrayList<>(); players.add(humanPlayer); players.addAll(machinePlayers); return List.copyOf(players); }
    public List<Player> getActivePlayers() { return turnManager == null ? List.of() : turnManager.getActivePlayers(); }
    public GameStatus getStatus() { return status; }

    private PlayResult playCurrentCard(Card card, int selectedValue) throws InvalidMoveException, EmptyDeckException {
        PlayResult result = playCardWithoutDrawing(card, selectedValue);
        drawCardForCurrentPlayer();
        advanceTurn();
        return new PlayResult(result.getPlayer(), result.getPlayedCard(), result.getSelectedValue(), result.getPreviousSum(),
                result.getNewSum(), false, isGameOver(), getWinner(), result.getMessage());
    }
    private PlayResult playCardWithoutDrawing(Card card, int selectedValue) throws InvalidMoveException {
        Player player = getCurrentPlayer();
        if (!player.getHandSnapshot().contains(card)) throw new InvalidMoveException("The selected card is not in the current player's hand.");
        int previousSum = table.getCurrentSum();
        table.playCard(card, selectedValue);
        player.removeCard(card);
        return new PlayResult(player, card, selectedValue, previousSum, table.getCurrentSum(), false, false, null,
                player.getName() + " played " + card + ".");
    }
    private PlayResult playCurrentCardSafely(Card card, int selectedValue) throws InvalidMoveException {
        try {
            return playCurrentCard(card, selectedValue);
        } catch (EmptyDeckException exception) {
            throw new IllegalStateException("No card was available after deck recycling.", exception);
        }
    }
    private PlayResult eliminateForNoMove() {
        Player player = getCurrentPlayer(); int sum = table.getCurrentSum(); eliminateCurrentPlayer();
        return new PlayResult(player, null, 0, sum, sum, true, isGameOver(), getWinner(), player.getName() + " was eliminated because no card could be played.");
    }
    private void ensureStarted() { if (status == GameStatus.NOT_STARTED) throw new IllegalStateException("The game has not started."); }
    private void ensureActionable() { ensureStarted(); if (isGameOver()) throw new GameOverException("The game is over."); }
    private void updateTurnStatus() { status = getCurrentPlayer() instanceof HumanPlayer ? GameStatus.WAITING_FOR_HUMAN_MOVE : GameStatus.MACHINE_TURN; }
}
