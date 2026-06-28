package com.project.fiftyzo.controller;

import com.project.fiftyzo.FiftyzoApplication;
import com.project.fiftyzo.exception.EmptyDeckException;
import com.project.fiftyzo.exception.InvalidMoveException;
import com.project.fiftyzo.exception.NoPlayableCardException;
import com.project.fiftyzo.model.Card;
import com.project.fiftyzo.model.Game;
import com.project.fiftyzo.model.HumanPlayer;
import com.project.fiftyzo.model.MachinePlayer;
import com.project.fiftyzo.model.PlayResult;
import com.project.fiftyzo.model.PlayableMove;
import com.project.fiftyzo.model.Player;
import com.project.fiftyzo.model.Rank;
import com.project.fiftyzo.model.Suit;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;

/**
 * Controller for the main game screen.
 * It renders model state into JavaFX nodes, handles human card interaction,
 * schedules asynchronous machine turns, manages overlays, and transitions to the end screen.
 */
public final class GameController {
    @FXML private HBox machinePlayersContainer;
    @FXML private StackPane gameCanvas;
    @FXML private Label currentSumLabel;
    @FXML private Label turnMessageLabel;
    @FXML private VBox deckContainer;
    @FXML private VBox tableCardContainer;
    @FXML private VBox eventLogBox;
    @FXML private HBox humanHandContainer;
    @FXML private ScrollPane eventLogScrollPane;
    @FXML private Label humanNameLabel;
    @FXML private Label deckCountLabel;
    @FXML private Label turnTimerLabel;
    @FXML private StackPane overlayLayer;
    @FXML private VBox optionsModal;
    @FXML private VBox aceModal;
    @FXML private VBox confirmModal;
    @FXML private VBox howToPlayModal;
    @FXML private Button aceOneButton;
    @FXML private Button aceTenButton;
    @FXML private Button confirmAcceptButton;
    @FXML private Button confirmCancelButton;
    @FXML private Label confirmTitleLabel;
    @FXML private Label confirmMessageLabel;

    private Game game;
    private volatile boolean machineTurnScheduled;
    private boolean endScreenShown;
    private boolean gameOverReasonLogged;
    private volatile boolean gameScreenActive = true;
    private volatile ScheduledFuture<?> currentTimerTask;
    private Card pendingAceCard;
    private Runnable pendingConfirmationAction;
    private final List<String> eventMessages = new ArrayList<>();
    private final ExecutorService machineTurnExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "fiftyzo-machine-turn");
        thread.setDaemon(true);
        return thread;
    });
    private final ScheduledExecutorService turnTimerExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "fiftyzo-human-turn-timer");
        thread.setDaemon(true);
        return thread;
    });
    private static final double CARD_WIDTH = 88;
    private static final double CARD_HEIGHT = 124;
    private static final double MACHINE_CARD_WIDTH = 58;
    private static final double MACHINE_CARD_HEIGHT = 82;
    private static final double DECK_IMAGE_HEIGHT = 140;
    private static final double DECK_IMAGE_SCALE = 0.93;
    private static final double BASE_CANVAS_WIDTH = 1100;
    private static final double BASE_CANVAS_HEIGHT = 760;
    private static final int HUMAN_TURN_SECONDS = 30;
    private static final double FACE_UP_VIEWPORT_INSET = 3;
    private static final int SPRITE_COLUMNS = 5;
    private static final String CARD_IMAGE_ROOT = "/com/project/fiftyzo/images/cards/";
    private static final String SPECIAL_CARD_IMAGE_ROOT = CARD_IMAGE_ROOT + "special/";
    private static final String CARD_BACK_PATH = CARD_IMAGE_ROOT + "machine-card-back-red.png";
    private static final String DECK_IMAGE_PATH = CARD_IMAGE_ROOT + "deck-pile-red.png";

    /**
     * Binds fixed-canvas scaling once the game canvas is attached to a scene.
     */
    @FXML
    private void initialize() {
        gameCanvas.sceneProperty().addListener((observable, oldScene, scene) -> {
            if (scene != null) bindGameCanvasScale(scene);
        });
    }

    /**
     * Binds the fixed 1100x760 game canvas to the current scene size.
     *
     * @param scene scene containing the game canvas
     */
    private void bindGameCanvasScale(Scene scene) {
        gameCanvas.scaleXProperty().unbind();
        gameCanvas.scaleYProperty().unbind();
        DoubleBinding canvasScale = Bindings.createDoubleBinding(
                () -> Math.min(scene.getWidth() / BASE_CANVAS_WIDTH, scene.getHeight() / BASE_CANVAS_HEIGHT),
                scene.widthProperty(),
                scene.heightProperty());
        gameCanvas.scaleXProperty().bind(canvasScale);
        gameCanvas.scaleYProperty().bind(canvasScale);
    }

    /**
     * Receives a started game from the start controller and performs the first render.
     *
     * @param game initialized game model
     */
    public void setGame(Game game) {
        this.game = Objects.requireNonNull(game, "game");
        appendLog("Game started with " + game.getMachinePlayers().size() + " machine player(s).");
        appendLog("Initial table card: " + formatCard(game.getTable().getTopCard()) + ". Current sum: " + game.getTable().getCurrentSum() + ".");
        renderGameState();
        continueTurnFlow();
    }

    /**
     * Renders every dynamic part of the game screen from the current model state.
     */
    public void renderGameState() {
        renderMachinePlayers();
        renderHumanHand();
        renderTable();
        renderDeck();
    }

    /**
     * Renders every machine hand as compact face-down slots or eliminated panels.
     */
    public void renderMachinePlayers() {
        machinePlayersContainer.getChildren().clear();
        for (MachinePlayer machine : game.getMachinePlayers()) {
            machinePlayersContainer.getChildren().add(createMachinePlayerSlot(machine));
        }
    }

    /**
     * Builds one machine player slot for the top player area.
     *
     * @param machine machine player to represent
     * @return visual slot containing the machine name and card backs
     */
    private VBox createMachinePlayerSlot(MachinePlayer machine) {
        VBox slot = new VBox(7);
        slot.getStyleClass().addAll("machine-slot", "machine-player-slot");

        Label machineName = new Label(machine.getName());
        machineName.getStyleClass().add("machine-name");
        slot.getChildren().add(machineName);

        if (!machine.isActive()) {
            slot.getStyleClass().add("machine-eliminated");
            slot.getChildren().add(new Label("ELIMINATED"));
            return slot;
        }

        HBox cardRow = new HBox(3);
        cardRow.getStyleClass().addAll("machine-hand", "machine-card-row");
        for (Card ignored : machine.getHandSnapshot()) {
            cardRow.getChildren().add(createMachineCardView());
        }
        slot.getChildren().add(cardRow);
        return slot;
    }

    /**
     * Renders the human hand and marks cards that cannot currently be played.
     */
    public void renderHumanHand() {
        humanHandContainer.getChildren().clear();
        HumanPlayer human = game.getHumanPlayer();
        humanNameLabel.setText(human.getName().toUpperCase() + " HAND");
        boolean humanTurn = game.getCurrentPlayer() == human && human.isActive() && !game.isGameOver();
        for (Card card : human.getHandSnapshot()) {
            boolean valid = humanTurn && isPlayable(card);
            StackPane cardView = createCardView(card, true, valid);
            if (valid) cardView.setOnMouseClicked(event -> handleHumanCardClick(card));
            humanHandContainer.getChildren().add(cardView);
        }
    }

    /**
     * Renders the current table card and updates the table sum risk styling.
     */
    public void renderTable() {
        tableCardContainer.getChildren().clear();
        StackPane tableCard = createCardView(game.getTable().getTopCard(), true, false);
        tableCard.getStyleClass().add("table-card");
        tableCardContainer.getChildren().add(tableCard);
        int sum = game.getTable().getCurrentSum();
        currentSumLabel.setText(String.valueOf(sum));
        currentSumLabel.getStyleClass().removeAll("sum-safe", "sum-warning", "sum-danger");
        currentSumLabel.getStyleClass().add(sum >= 45 ? "sum-danger" : sum >= 40 ? "sum-warning" : "sum-safe");
    }

    /**
     * Renders the deck image and the remaining deck count.
     */
    public void renderDeck() {
        deckContainer.getChildren().clear();
        deckContainer.getChildren().add(createDeckView());
        deckCountLabel.setText(game.getDeck().size() + " cards remaining");
    }

    /**
     * Adds one message to the on-screen event log and scrolls to the newest entry.
     *
     * @param message event message to append
     */
    public void appendLog(String message) {
        eventMessages.add(message);
        Label entry = new Label(message);
        entry.setWrapText(true);
        entry.getStyleClass().add("log-entry");
        eventLogBox.getChildren().add(entry);
        if (eventLogBox.getChildren().size() > 40) eventLogBox.getChildren().remove(0);
        eventLogScrollPane.setVvalue(1.0);
    }

    /**
     * Handles a click on a currently valid human card.
     *
     * @param card card selected by the human player
     */
    public void handleHumanCardClick(Card card) {
        if (game.getCurrentPlayer() != game.getHumanPlayer() || !isPlayable(card)) return;
        if (card.isAce()) askAceValue(card);
        else playHumanCard(card, card.getPossibleValues().get(0));
    }

    /**
     * Offers only the legal value choices for an Ace.
     *
     * @param ace Ace card selected by the human player
     */
    public void askAceValue(Card ace) {
        List<Integer> values = ace.getPossibleValues().stream().filter(game.getTable()::canApplyValue).toList();
        if (values.isEmpty()) return;
        if (values.size() == 1) { playHumanCard(ace, values.get(0)); return; }
        pendingAceCard = ace;
        aceOneButton.setDisable(!values.contains(1));
        aceTenButton.setDisable(!values.contains(10));
        showOverlay(aceModal);
    }

    /**
     * Shows the in-game options overlay without changing turn flow.
     */
    @FXML
    public void showOptionsMenu() {
        showOverlay(optionsModal);
    }

    /**
     * Hides the options overlay and resumes the visible game screen.
     */
    @FXML
    public void resumeGame() {
        hideOverlays();
    }

    /**
     * Opens a confirmation overlay for restarting the current game.
     */
    @FXML
    public void requestRestartGame() {
        showConfirmation("Restart Game", "Restart the current game?", "Restart", this::loadStartScreen);
    }

    /**
     * Opens a confirmation overlay for returning to the main menu.
     */
    @FXML
    public void requestBackToMainMenu() {
        showConfirmation("Main Menu", "Return to main menu? Current progress will be lost.", "Main Menu", this::loadStartScreen);
    }

    /**
     * Opens a confirmation overlay for exiting the game.
     */
    @FXML
    public void requestExitGame() {
        showConfirmation("Exit", "Exit the game?", "Exit", () -> {
            shutdownConcurrentWorkers();
            ((Stage) currentSumLabel.getScene().getWindow()).close();
        });
    }

    /**
     * Shows the How to Play overlay.
     */
    @FXML
    public void showHowToPlayOverlay() {
        showOverlay(howToPlayModal);
    }

    /**
     * Closes the How to Play overlay.
     */
    @FXML
    public void closeHowToPlayOverlay() {
        hideOverlays();
    }

    /**
     * Applies an Ace value of one from the Ace choice overlay.
     */
    @FXML
    public void chooseAceOne() {
        chooseAceValue(1);
    }

    /**
     * Applies an Ace value of ten from the Ace choice overlay.
     */
    @FXML
    public void chooseAceTen() {
        chooseAceValue(10);
    }

    /**
     * Runs the pending confirmation action and closes overlays.
     */
    @FXML
    public void acceptConfirmation() {
        Runnable action = pendingConfirmationAction;
        pendingConfirmationAction = null;
        hideOverlays();
        if (action != null) action.run();
    }

    /**
     * Cancels the pending confirmation action and closes overlays.
     */
    @FXML
    public void cancelConfirmation() {
        pendingConfirmationAction = null;
        hideOverlays();
    }

    /**
     * Advances turns, including automatic machine turns and eliminations.
     * This is the controller-level coordinator around the UI-independent game model.
     */
    public void continueTurnFlow() {
        if (game.isGameOver()) {
            cancelHumanTurnTimer();
            logGameOverReason();
            showEndScreen();
            return;
        }
        Player current = game.getCurrentPlayer();
        if (!current.hasPlayableCard(game.getTable().getCurrentSum())) {
            String playerName = current.getName();
            game.processNoPlayableCardForCurrentPlayer();
            appendLog(playerName + " has no playable cards and was eliminated.");
            renderGameState();
            continueTurnFlow();
            return;
        }
        if (current instanceof HumanPlayer) {
            startHumanTurnTimer();
            turnMessageLabel.setText("Your turn. Select a valid card.");
            renderHumanHand();
            return;
        }
        cancelHumanTurnTimer();
        turnTimerLabel.setText("MACHINE THINKING...");
        turnMessageLabel.setText(current.getName() + " is thinking...");
        renderHumanHand();
        if (!machineTurnScheduled) scheduleMachineTurn();
    }

    /**
     * Loads the end screen after the model identifies a winner.
     *
     * @throws IllegalStateException if the end screen cannot be loaded
     */
    public void showEndScreen() {
        if (endScreenShown) return;
        if (!game.isGameOver() || game.getActivePlayers().size() != 1 || game.getWinner() == null) return;
        logGameOverReason();
        endScreenShown = true;
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/com/project/fiftyzo/view/end-view.fxml")));
            Parent root = loader.load();
            loader.<EndController>getController().setResult(game.getWinner(), List.copyOf(eventMessages));
            shutdownConcurrentWorkers();
            ((Stage) currentSumLabel.getScene().getWindow()).setScene(FiftyzoApplication.createScene(root));
        } catch (IOException exception) {
            throw new IllegalStateException("The end screen could not be loaded.", exception);
        }
    }

    /**
     * Starts a real background worker for delayed machine thinking.
     * The game model and JavaFX nodes are still updated on the JavaFX Application Thread.
     */
    private void scheduleMachineTurn() {
        machineTurnScheduled = true;
        machineTurnExecutor.submit(() -> {
            if (!sleepForRandomDelay(2.0, 4.0)) return;
            Platform.runLater(this::applyScheduledMachineTurn);
        });
    }

    /**
     * Applies the machine card play after the background thinking delay.
     */
    private void applyScheduledMachineTurn() {
        machineTurnScheduled = false;
        if (!gameScreenActive || game.isGameOver() || !(game.getCurrentPlayer() instanceof MachinePlayer)) return;
        try {
            MachinePlayer machine = (MachinePlayer) game.getCurrentPlayer();
            PlayResult result = game.playMachineCard();
            appendPlayResult(result);
            renderGameState();
            if (result.isPlayerEliminated()) {
                continueTurnFlow();
                return;
            }
            turnMessageLabel.setText(machine.getName() + " is drawing a card...");
            appendLog(machine.getName() + " is drawing a card.");
            scheduleMachineDraw(machine);
        } catch (NoPlayableCardException | InvalidMoveException exception) {
            showError("Machine turn failed", exception.getMessage());
        }
    }

    /**
     * Schedules the delayed draw step after a machine has played a card on the machine executor.
     *
     * @param machine machine player that must draw before turn advancement
     */
    private void scheduleMachineDraw(MachinePlayer machine) {
        machineTurnExecutor.submit(() -> {
            if (!sleepForRandomDelay(1.0, 2.0)) return;
            Platform.runLater(() -> applyScheduledMachineDraw(machine));
        });
    }

    /**
     * Applies the machine draw step after the background draw delay.
     *
     * @param machine machine player that must draw before turn advancement
     */
    private void applyScheduledMachineDraw(MachinePlayer machine) {
        if (!gameScreenActive || game.isGameOver() || game.getCurrentPlayer() != machine) return;
        try {
            boolean deckWasEmpty = game.getDeck().isEmpty();
            game.drawCardForCurrentPlayer();
            game.advanceTurn();
            appendLog(machine.getName() + " drew a card.");
            if (deckWasEmpty) appendLog("Deck was rebuilt from previous table cards.");
            renderGameState();
            continueTurnFlow();
        } catch (EmptyDeckException exception) {
            showError("Deck error", exception.getMessage());
        }
    }

    /**
     * Plays a human-selected card through the model and refreshes the screen.
     *
     * @param card selected card
     * @param value selected card value
     */
    private void playHumanCard(Card card, int value) {
        try {
            boolean deckWasEmpty = game.getDeck().isEmpty();
            PlayResult result = game.playHumanCard(card, value);
            cancelHumanTurnTimer();
            appendPlayResult(result);
            if (deckWasEmpty) appendLog("Deck was rebuilt from table cards.");
            renderGameState();
            continueTurnFlow();
        } catch (InvalidMoveException exception) {
            showError("Invalid move", exception.getMessage());
            renderHumanHand();
        }
    }

    /**
     * Checks whether a human card is currently playable.
     *
     * @param card card to test
     * @return true when the card has at least one legal move
     */
    private boolean isPlayable(Card card) {
        return game.getHumanPlayer().getPlayableMoves(game.getTable().getCurrentSum()).stream().anyMatch(move -> move.getCard() == card);
    }

    /**
     * Generates a random delay for presentation timing.
     *
     * @param minSeconds minimum delay in seconds
     * @param maxSeconds maximum delay in seconds
     * @return random delay in the supplied range
     */
    private double randomDelay(double minSeconds, double maxSeconds) {
        return ThreadLocalRandom.current().nextDouble(minSeconds, maxSeconds);
    }

    /**
     * Sleeps on a background worker to simulate thinking without blocking JavaFX.
     *
     * @param minSeconds minimum delay in seconds
     * @param maxSeconds maximum delay in seconds
     */
    private boolean sleepForRandomDelay(double minSeconds, double maxSeconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(Math.round(randomDelay(minSeconds, maxSeconds) * 1000));
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Appends the user-facing message for a play or elimination result.
     *
     * @param result result returned by the game model
     */
    private void appendPlayResult(PlayResult result) {
        if (result.isPlayerEliminated()) appendLog(result.getPlayer().getName() + " has no playable cards and was eliminated.");
        else appendLog(result.getPlayer().getName() + " played " + formatCard(result.getPlayedCard()) + " as " + result.getSelectedValue() + ". Current sum: " + result.getNewSum() + ".");
    }

    /**
     * Logs the game-over reason once after the model reports a winner.
     */
    private void logGameOverReason() {
        if (gameOverReasonLogged || game.getWinner() == null) return;
        gameOverReasonLogged = true;
        appendLog("Game over because only one active player remains.");
        appendLog("Winner: " + game.getWinner().getName() + ".");
    }

    /**
     * Starts a scheduled human turn countdown on a real timer thread.
     */
    private void startHumanTurnTimer() {
        cancelHumanTurnTimer();
        AtomicInteger remainingSeconds = new AtomicInteger(HUMAN_TURN_SECONDS);
        turnTimerLabel.setText("TIME LEFT: " + HUMAN_TURN_SECONDS + "s");
        currentTimerTask = turnTimerExecutor.scheduleAtFixedRate(() -> {
            int seconds = remainingSeconds.getAndDecrement();
            if (seconds < 0 || !gameScreenActive) {
                cancelTimerFromWorker();
                return;
            }
            Platform.runLater(() -> updateHumanTurnTimer(seconds));
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Updates timer UI on the JavaFX Application Thread.
     *
     * @param seconds remaining seconds to display
     */
    private void updateHumanTurnTimer(int seconds) {
        if (!gameScreenActive || game.isGameOver() || !(game.getCurrentPlayer() instanceof HumanPlayer)) return;
        turnTimerLabel.setText("TIME LEFT: " + Math.max(seconds, 0) + "s");
        if (seconds == 0) appendLog("Timer expired. Please make a move.");
    }

    /**
     * Cancels the currently active human timer task.
     */
    private void cancelHumanTurnTimer() {
        ScheduledFuture<?> task = currentTimerTask;
        currentTimerTask = null;
        if (task != null) task.cancel(false);
        if (turnTimerLabel != null) turnTimerLabel.setText("");
    }

    /**
     * Cancels the current timer from the timer worker thread.
     */
    private void cancelTimerFromWorker() {
        ScheduledFuture<?> task = currentTimerTask;
        if (task != null) task.cancel(false);
    }

    /**
     * Stops background workers when this game screen is no longer active.
     */
    private void shutdownConcurrentWorkers() {
        gameScreenActive = false;
        cancelHumanTurnTimer();
        machineTurnExecutor.shutdownNow();
        turnTimerExecutor.shutdownNow();
    }

    /**
     * Loads the start screen, usually after confirmation from an overlay.
     *
     * @throws IllegalStateException if the start screen cannot be loaded
     */
    private void loadStartScreen() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/project/fiftyzo/view/start-view.fxml")));
            shutdownConcurrentWorkers();
            ((Stage) currentSumLabel.getScene().getWindow()).setScene(FiftyzoApplication.createScene(root));
        } catch (IOException exception) {
            throw new IllegalStateException("The start screen could not be loaded.", exception);
        }
    }

    /**
     * Creates a visual card node for human, table, or fallback card rendering.
     *
     * @param card card to render
     * @param faceUp whether the card face should be shown
     * @param valid whether the card should receive playable styling
     * @return configured card node
     */
    private StackPane createCardView(Card card, boolean faceUp, boolean valid) {
        StackPane view = new StackPane();
        view.getStyleClass().add("card");
        view.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        view.setMinSize(CARD_WIDTH, CARD_HEIGHT);
        if (!faceUp) {
            view.getStyleClass().add("card-back");
            ImageView imageView = createCardBackImageView();
            if (imageView == null) view.getChildren().add(new Label("50ZO"));
            else view.getChildren().add(imageView);
        } else {
            ImageView imageView = createCardImageView(card);
            if (imageView == null) view.getChildren().add(createLabelCardContent(card));
            else {
                view.getStyleClass().add("card-image");
                view.getChildren().add(imageView);
            }
            view.getStyleClass().add(valid ? "card-valid" : "card-invalid");
        }
        return view;
    }

    /**
     * Creates the deck pile visual node.
     *
     * @return configured deck node
     */
    private StackPane createDeckView() {
        StackPane view = new StackPane();
        view.getStyleClass().add("deck-card");
        view.setPrefSize(CARD_WIDTH, DECK_IMAGE_HEIGHT);
        view.setMinSize(CARD_WIDTH, DECK_IMAGE_HEIGHT);
        ImageView imageView = createImageView(DECK_IMAGE_PATH);
        if (imageView == null) view.getChildren().add(new Label("DECK"));
        else {
            imageView.setFitWidth(CARD_WIDTH * DECK_IMAGE_SCALE);
            imageView.setFitHeight(DECK_IMAGE_HEIGHT * DECK_IMAGE_SCALE);
            view.getChildren().add(imageView);
        }
        return view;
    }

    /**
     * Creates a compact face-down card for a machine hand.
     *
     * @return configured machine card-back node
     */
    private StackPane createMachineCardView() {
        StackPane view = new StackPane();
        view.getStyleClass().add("machine-card-back");
        view.setPrefSize(MACHINE_CARD_WIDTH, MACHINE_CARD_HEIGHT);
        view.setMinSize(MACHINE_CARD_WIDTH, MACHINE_CARD_HEIGHT);
        ImageView imageView = createCardBackImageView();
        if (imageView == null) view.getChildren().add(new Label("50ZO"));
        else {
            imageView.setFitWidth(MACHINE_CARD_WIDTH);
            imageView.setFitHeight(MACHINE_CARD_HEIGHT);
            view.getChildren().add(imageView);
        }
        return view;
    }

    /**
     * Creates the face-up image view for a card, including special Ace artwork.
     *
     * @param card card to render
     * @return image view for the card, or null if no resource is available
     */
    private ImageView createCardImageView(Card card) {
        if (card == null) return null;
        if (card.isAce()) {
            ImageView specialAceImage = createSpecialAceImageView(card.getSuit());
            if (specialAceImage != null) return specialAceImage;
        }
        ImageView imageView = createImageView(getSpriteSheetPath(card.getSuit()));
        if (imageView == null) return null;
        int rankIndex = getRankIndex(card.getRank());
        int column = rankIndex % SPRITE_COLUMNS;
        int row = rankIndex / SPRITE_COLUMNS;
        imageView.setViewport(new Rectangle2D(
                column * CARD_WIDTH + FACE_UP_VIEWPORT_INSET,
                row * CARD_HEIGHT + FACE_UP_VIEWPORT_INSET,
                CARD_WIDTH - FACE_UP_VIEWPORT_INSET * 2,
                CARD_HEIGHT - FACE_UP_VIEWPORT_INSET * 2));
        imageView.setFitWidth(CARD_WIDTH);
        imageView.setFitHeight(CARD_HEIGHT);
        return imageView;
    }

    /**
     * Applies the selected Ace value to the pending Ace card.
     *
     * @param value chosen Ace value
     */
    private void chooseAceValue(int value) {
        if (pendingAceCard == null) {
            hideOverlays();
            return;
        }
        Card ace = pendingAceCard;
        pendingAceCard = null;
        hideOverlays();
        playHumanCard(ace, value);
    }

    /**
     * Shows a reusable confirmation overlay.
     *
     * @param title overlay title
     * @param message confirmation message
     * @param confirmText text for the accept button
     * @param action action to run if accepted
     */
    private void showConfirmation(String title, String message, String confirmText, Runnable action) {
        confirmTitleLabel.setText(title);
        confirmMessageLabel.setText(message);
        confirmAcceptButton.setText(confirmText);
        confirmCancelButton.setVisible(true);
        confirmCancelButton.setManaged(true);
        pendingConfirmationAction = action;
        showOverlay(confirmModal);
    }

    /**
     * Shows a message overlay using the confirmation modal structure.
     *
     * @param title message title
     * @param message message body
     */
    private void showMessageOverlay(String title, String message) {
        confirmTitleLabel.setText(title);
        confirmMessageLabel.setText(message);
        confirmAcceptButton.setText("OK");
        confirmCancelButton.setVisible(false);
        confirmCancelButton.setManaged(false);
        pendingConfirmationAction = null;
        showOverlay(confirmModal);
    }

    /**
     * Shows one overlay panel and hides the others.
     *
     * @param overlay modal panel to show
     */
    private void showOverlay(VBox overlay) {
        hideOverlayPanel(optionsModal);
        hideOverlayPanel(aceModal);
        hideOverlayPanel(confirmModal);
        hideOverlayPanel(howToPlayModal);
        overlayLayer.setVisible(true);
        overlayLayer.setManaged(true);
        overlayLayer.setMouseTransparent(false);
        overlay.setVisible(true);
        overlay.setManaged(true);
    }

    /**
     * Hides all overlay panels and disables the overlay layer.
     */
    private void hideOverlays() {
        hideOverlayPanel(optionsModal);
        hideOverlayPanel(aceModal);
        hideOverlayPanel(confirmModal);
        hideOverlayPanel(howToPlayModal);
        overlayLayer.setVisible(false);
        overlayLayer.setManaged(false);
        overlayLayer.setMouseTransparent(true);
    }

    /**
     * Hides one overlay panel without changing the overlay layer state.
     *
     * @param overlay modal panel to hide
     */
    private void hideOverlayPanel(VBox overlay) {
        overlay.setVisible(false);
        overlay.setManaged(false);
    }

    /**
     * Creates a special Ace image for the supplied suit when an asset exists.
     *
     * @param suit Ace suit
     * @return special Ace image view, or null if no image can be loaded
     */
    private ImageView createSpecialAceImageView(Suit suit) {
        for (String path : getSpecialAcePaths(suit)) {
            ImageView imageView = createImageView(path);
            if (imageView != null) {
                imageView.setFitWidth(CARD_WIDTH);
                imageView.setFitHeight(CARD_HEIGHT);
                return imageView;
            }
        }
        return null;
    }

    /**
     * Creates the shared machine/deck card-back image.
     *
     * @return card-back image view, or null if unavailable
     */
    private ImageView createCardBackImageView() {
        return createImageView(CARD_BACK_PATH);
    }

    /**
     * Loads an image resource into an {@link ImageView}.
     *
     * @param resourcePath classpath path to the image resource
     * @return image view, or null when the image cannot be loaded
     */
    private ImageView createImageView(String resourcePath) {
        try {
            var resource = GameController.class.getResource(resourcePath);
            if (resource == null) return null;
            Image image = new Image(resource.toExternalForm());
            if (image.isError()) return null;
            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(false);
            return imageView;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /**
     * Selects the sprite sheet used for a card suit.
     *
     * @param suit card suit
     * @return classpath path to the suit sprite sheet
     */
    private String getSpriteSheetPath(Suit suit) {
        return switch (suit) {
            case DIAMONDS -> CARD_IMAGE_ROOT + "diamonds-top-down-88x124.png";
            case HEARTS -> CARD_IMAGE_ROOT + "hearts-top-down-88x124.png";
            case CLUBS -> CARD_IMAGE_ROOT + "clubs-top-down-88x124.png";
            case SPADES -> CARD_IMAGE_ROOT + "spades-top-down-88x124.png";
        };
    }

    /**
     * Returns fallback paths for special Ace artwork for a suit.
     *
     * @param suit Ace suit
     * @return ordered resource paths to try
     */
    private List<String> getSpecialAcePaths(Suit suit) {
        return switch (suit) {
            case CLUBS -> List.of(SPECIAL_CARD_IMAGE_ROOT + "ace-clubs.png", CARD_IMAGE_ROOT + "as-de-trebol.png");
            case DIAMONDS -> List.of(SPECIAL_CARD_IMAGE_ROOT + "ace-diamonds.png", CARD_IMAGE_ROOT + "as-de-diamante.png");
            case HEARTS -> List.of(SPECIAL_CARD_IMAGE_ROOT + "ace-hearts.png", CARD_IMAGE_ROOT + "as-de-corazon.png");
            case SPADES -> List.of(SPECIAL_CARD_IMAGE_ROOT + "ace-spades.png", CARD_IMAGE_ROOT + "as-de-pica.png");
        };
    }

    /**
     * Maps a rank to its position in the card sprite sheet.
     *
     * @param rank card rank
     * @return zero-based sprite index
     */
    private int getRankIndex(Rank rank) {
        return switch (rank) {
            case ACE -> 0;
            case TWO -> 1;
            case THREE -> 2;
            case FOUR -> 3;
            case FIVE -> 4;
            case SIX -> 5;
            case SEVEN -> 6;
            case EIGHT -> 7;
            case NINE -> 8;
            case TEN -> 9;
            case JACK -> 10;
            case QUEEN -> 11;
            case KING -> 12;
        };
    }

    /**
     * Creates a text fallback for a face-up card when an image is missing.
     *
     * @param card card to describe
     * @return fallback card content
     */
    private VBox createLabelCardContent(Card card) {
        VBox content = new VBox(4);
        content.getStyleClass().add("card-content");
        content.getChildren().addAll(new Label(rankSymbol(card.getRank())), new Label(suitSymbol(card.getSuit())));
        return content;
    }

    private String formatCard(Card card) { return rankSymbol(card.getRank()) + suitSymbol(card.getSuit()); }
    private String rankSymbol(Rank rank) {
        return switch (rank) {
            case JACK -> "J"; case QUEEN -> "Q"; case KING -> "K"; case ACE -> "A";
            default -> String.valueOf(rank.ordinal() + 2);
        };
    }
    private String suitSymbol(Suit suit) { return switch (suit) { case HEARTS -> "\u2665"; case DIAMONDS -> "\u2666"; case CLUBS -> "\u2663"; case SPADES -> "\u2660"; }; }
    private void showError(String title, String message) { showMessageOverlay(title, message); }
}
    /**
     * Hides the options overlay and resumes the visible game screen.
     */
    /**
     * Opens a confirmation overlay for restarting the current game.
     */
    /**
     * Opens a confirmation overlay for returning to the main menu.
     */
    /**
     * Opens a confirmation overlay for exiting the game.
     */
    /**
     * Shows the How to Play overlay.
     */
    /**
     * Closes the How to Play overlay.
     */
    /**
     * Applies an Ace value of one from the Ace choice overlay.
     */
    /**
     * Applies an Ace value of ten from the Ace choice overlay.
     */
    /**
     * Runs the pending confirmation action and closes overlays.
     */
    /**
     * Cancels the pending confirmation action and closes overlays.
     */
