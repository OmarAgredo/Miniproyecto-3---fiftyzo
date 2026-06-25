package com.project.fiftyzo.model;

import com.project.fiftyzo.exception.EmptyDeckException;
import com.project.fiftyzo.exception.GameOverException;
import com.project.fiftyzo.exception.InvalidMoveException;
import com.project.fiftyzo.exception.NoPlayableCardException;
import com.project.fiftyzo.model.turn.TurnManager;
import com.project.fiftyzo.util.GameConstants;
import java.util.ArrayList;
import java.util.List;

/** Central, UI-independent implementation of the Fiftyzo game rules. */
public final class Game {
    private Deck deck;
    private Table table;
    private TurnManager turnManager;
    private GameStatus status = GameStatus.NOT_STARTED;
    private final HumanPlayer humanPlayer;
    private final List<MachinePlayer> machinePlayers;

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

    /** Initializes, shuffles, deals, and prepares the human player's first turn. */
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

    public void dealInitialCards() throws EmptyDeckException {
        ensureStarted();
        for (int i = 0; i < GameConstants.INITIAL_HAND_SIZE; i++) for (Player player : getPlayers()) player.receiveCard(deck.drawCard());
    }
    public void placeInitialTableCard() throws EmptyDeckException { ensureStarted(); table.placeInitialCard(deck.drawCard()); }

    public PlayResult playHumanCard(Card card, int selectedValue) throws InvalidMoveException, GameOverException {
        ensureActionable();
        if (getCurrentPlayer() != humanPlayer) throw new InvalidMoveException("It is not the human player's turn.");
        if (!humanPlayer.hasPlayableCard(table.getCurrentSum())) return eliminateForNoMove();
        return playCurrentCardSafely(card, selectedValue);
    }

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
     */
    public PlayResult playMachineCard() throws NoPlayableCardException, InvalidMoveException, GameOverException {
        ensureActionable();
        if (!(getCurrentPlayer() instanceof MachinePlayer machine)) throw new InvalidMoveException("It is not a machine player's turn.");
        if (!machine.hasPlayableCard(table.getCurrentSum())) return eliminateForNoMove();
        PlayableMove move = machine.chooseMove(table.getCurrentSum());
        return playCardWithoutDrawing(move.getCard(), move.getValue());
    }

    public void drawCardForCurrentPlayer() throws EmptyDeckException {
        ensureStarted(); recycleDeckIfNeeded(); getCurrentPlayer().receiveCard(deck.drawCard());
    }
    public void recycleDeckIfNeeded() {
        ensureStarted();
        if (deck.isEmpty()) { List<Card> recycle = table.removeRecycleCardsExceptTop(); deck.addCardsToBottom(recycle); deck.shuffle(); }
    }
    public void eliminateCurrentPlayer() {
        ensureStarted();
        if (isGameOver()) throw new GameOverException("The game is over.");
        Player player = getCurrentPlayer();
        deck.addCardsToBottom(player.getHandSnapshot()); player.clearHand(); player.eliminate(); turnManager.removeCurrentPlayer();
        if (isGameOver()) status = GameStatus.GAME_OVER; else updateTurnStatus();
    }
    public void processNoPlayableCardForCurrentPlayer() { eliminateCurrentPlayer(); }
    public void advanceTurn() { ensureStarted(); if (!isGameOver()) { turnManager.advanceTurn(); updateTurnStatus(); } }
    /** Returns whether exactly one active player remains in the turn cycle. */
    public boolean isGameOver() {
        return turnManager != null && turnManager.hasWinner() && turnManager.getActivePlayers().size() == 1;
    }
    /** Returns the sole active player only after the game is over. */
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
