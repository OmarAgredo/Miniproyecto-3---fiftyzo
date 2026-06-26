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
import java.util.concurrent.ThreadLocalRandom;
import javafx.animation.PauseTransition;
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
import javafx.util.Duration;

/** Renders game state and delegates player actions to the model. */
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
    private boolean machineTurnScheduled;
    private boolean endScreenShown;
    private boolean gameOverReasonLogged;
    private Card pendingAceCard;
    private Runnable pendingConfirmationAction;
    private final List<String> eventMessages = new ArrayList<>();
    private static final double CARD_WIDTH = 88;
    private static final double CARD_HEIGHT = 124;
    private static final double MACHINE_CARD_WIDTH = 58;
    private static final double MACHINE_CARD_HEIGHT = 82;
    private static final double DECK_IMAGE_HEIGHT = 140;
    private static final double DECK_IMAGE_SCALE = 0.93;
    private static final double BASE_CANVAS_WIDTH = 1100;
    private static final double BASE_CANVAS_HEIGHT = 760;
    private static final double FACE_UP_VIEWPORT_INSET = 3;
    private static final int SPRITE_COLUMNS = 5;
    private static final String CARD_IMAGE_ROOT = "/com/project/fiftyzo/images/cards/";
    private static final String SPECIAL_CARD_IMAGE_ROOT = CARD_IMAGE_ROOT + "special/";
    private static final String CARD_BACK_PATH = CARD_IMAGE_ROOT + "machine-card-back-red.png";
    private static final String DECK_IMAGE_PATH = CARD_IMAGE_ROOT + "deck-pile-red.png";

    @FXML
    private void initialize() {
        gameCanvas.sceneProperty().addListener((observable, oldScene, scene) -> {
            if (scene != null) bindGameCanvasScale(scene);
        });
    }

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

    /** Receives a started game from the start controller. */
    public void setGame(Game game) {
        this.game = Objects.requireNonNull(game, "game");
        appendLog("Game started with " + game.getMachinePlayers().size() + " machine player(s).");
        appendLog("Initial table card: " + formatCard(game.getTable().getTopCard()) + ". Current sum: " + game.getTable().getCurrentSum() + ".");
        renderGameState();
        continueTurnFlow();
    }

    /** Renders every dynamic part of the game screen. */
    public void renderGameState() {
        renderMachinePlayers();
        renderHumanHand();
        renderTable();
        renderDeck();
    }

    /** Renders every machine hand as face-down cards or an eliminated panel. */
    public void renderMachinePlayers() {
        machinePlayersContainer.getChildren().clear();
        for (MachinePlayer machine : game.getMachinePlayers()) {
            machinePlayersContainer.getChildren().add(createMachinePlayerSlot(machine));
        }
    }

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

    /** Renders the human hand and marks cards that cannot be played. */
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

    /** Renders the top card and current sum with its risk color. */
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

    /** Renders the remaining deck count. */
    public void renderDeck() {
        deckContainer.getChildren().clear();
        deckContainer.getChildren().add(createDeckView());
        deckCountLabel.setText(game.getDeck().size() + " cards remaining");
    }

    /** Adds one message to the on-screen event log. */
    public void appendLog(String message) {
        eventMessages.add(message);
        Label entry = new Label(message);
        entry.setWrapText(true);
        entry.getStyleClass().add("log-entry");
        eventLogBox.getChildren().add(entry);
        if (eventLogBox.getChildren().size() > 40) eventLogBox.getChildren().remove(0);
        eventLogScrollPane.setVvalue(1.0);
    }

    /** Handles a click on a currently valid human card. */
    public void handleHumanCardClick(Card card) {
        if (game.getCurrentPlayer() != game.getHumanPlayer() || !isPlayable(card)) return;
        if (card.isAce()) askAceValue(card);
        else playHumanCard(card, card.getPossibleValues().get(0));
    }

    /** Offers only the legal value choices for an ace. */
    public void askAceValue(Card ace) {
        List<Integer> values = ace.getPossibleValues().stream().filter(game.getTable()::canApplyValue).toList();
        if (values.isEmpty()) return;
        if (values.size() == 1) { playHumanCard(ace, values.get(0)); return; }
        pendingAceCard = ace;
        aceOneButton.setDisable(!values.contains(1));
        aceTenButton.setDisable(!values.contains(10));
        showOverlay(aceModal);
    }

    /** Shows non-invasive game screen options without changing turn flow. */
    @FXML
    public void showOptionsMenu() {
        showOverlay(optionsModal);
    }

    @FXML
    public void resumeGame() {
        hideOverlays();
    }

    @FXML
    public void requestRestartGame() {
        showConfirmation("Restart Game", "Restart the current game?", "Restart", this::loadStartScreen);
    }

    @FXML
    public void requestBackToMainMenu() {
        showConfirmation("Main Menu", "Return to main menu? Current progress will be lost.", "Main Menu", this::loadStartScreen);
    }

    @FXML
    public void requestExitGame() {
        showConfirmation("Exit", "Exit the game?", "Exit", () -> ((Stage) currentSumLabel.getScene().getWindow()).close());
    }

    @FXML
    public void showHowToPlayOverlay() {
        showOverlay(howToPlayModal);
    }

    @FXML
    public void closeHowToPlayOverlay() {
        hideOverlays();
    }

    @FXML
    public void chooseAceOne() {
        chooseAceValue(1);
    }

    @FXML
    public void chooseAceTen() {
        chooseAceValue(10);
    }

    @FXML
    public void acceptConfirmation() {
        Runnable action = pendingConfirmationAction;
        pendingConfirmationAction = null;
        hideOverlays();
        if (action != null) action.run();
    }

    @FXML
    public void cancelConfirmation() {
        pendingConfirmationAction = null;
        hideOverlays();
    }

    /** Advances turns, including automatic machine turns and eliminations. */
    public void continueTurnFlow() {
        if (game.isGameOver()) {
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
            turnMessageLabel.setText("Your turn. Select a valid card.");
            renderHumanHand();
            return;
        }
        turnMessageLabel.setText(current.getName() + " is thinking...");
        renderHumanHand();
        if (!machineTurnScheduled) scheduleMachineTurn();
    }

    /** Loads the end screen after the model identifies a winner. */
    public void showEndScreen() {
        if (endScreenShown) return;
        if (!game.isGameOver() || game.getActivePlayers().size() != 1 || game.getWinner() == null) return;
        logGameOverReason();
        endScreenShown = true;
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/com/project/fiftyzo/view/end-view.fxml")));
            Parent root = loader.load();
            loader.<EndController>getController().setResult(game.getWinner(), List.copyOf(eventMessages));
            ((Stage) currentSumLabel.getScene().getWindow()).setScene(FiftyzoApplication.createScene(root));
        } catch (IOException exception) {
            throw new IllegalStateException("The end screen could not be loaded.", exception);
        }
    }

    private void scheduleMachineTurn() {
        machineTurnScheduled = true;
        PauseTransition pause = new PauseTransition(Duration.seconds(randomDelay(2.0, 4.0)));
        pause.setOnFinished(event -> {
            machineTurnScheduled = false;
            if (game.isGameOver() || !(game.getCurrentPlayer() instanceof MachinePlayer)) return;
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
        });
        pause.play();
    }

    private void scheduleMachineDraw(MachinePlayer machine) {
        PauseTransition pause = new PauseTransition(Duration.seconds(randomDelay(1.0, 2.0)));
        pause.setOnFinished(event -> {
            if (game.isGameOver() || game.getCurrentPlayer() != machine) return;
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
        });
        pause.play();
    }

    private void playHumanCard(Card card, int value) {
        try {
            boolean deckWasEmpty = game.getDeck().isEmpty();
            PlayResult result = game.playHumanCard(card, value);
            appendPlayResult(result);
            if (deckWasEmpty) appendLog("Deck was rebuilt from table cards.");
            renderGameState();
            continueTurnFlow();
        } catch (InvalidMoveException exception) {
            showError("Invalid move", exception.getMessage());
            renderHumanHand();
        }
    }

    private boolean isPlayable(Card card) {
        return game.getHumanPlayer().getPlayableMoves(game.getTable().getCurrentSum()).stream().anyMatch(move -> move.getCard() == card);
    }

    private double randomDelay(double minSeconds, double maxSeconds) {
        return ThreadLocalRandom.current().nextDouble(minSeconds, maxSeconds);
    }

    private void appendPlayResult(PlayResult result) {
        if (result.isPlayerEliminated()) appendLog(result.getPlayer().getName() + " has no playable cards and was eliminated.");
        else appendLog(result.getPlayer().getName() + " played " + formatCard(result.getPlayedCard()) + " as " + result.getSelectedValue() + ". Current sum: " + result.getNewSum() + ".");
    }

    private void logGameOverReason() {
        if (gameOverReasonLogged || game.getWinner() == null) return;
        gameOverReasonLogged = true;
        appendLog("Game over because only one active player remains.");
        appendLog("Winner: " + game.getWinner().getName() + ".");
    }

    private void loadStartScreen() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/project/fiftyzo/view/start-view.fxml")));
            ((Stage) currentSumLabel.getScene().getWindow()).setScene(FiftyzoApplication.createScene(root));
        } catch (IOException exception) {
            throw new IllegalStateException("The start screen could not be loaded.", exception);
        }
    }

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

    private void showConfirmation(String title, String message, String confirmText, Runnable action) {
        confirmTitleLabel.setText(title);
        confirmMessageLabel.setText(message);
        confirmAcceptButton.setText(confirmText);
        confirmCancelButton.setVisible(true);
        confirmCancelButton.setManaged(true);
        pendingConfirmationAction = action;
        showOverlay(confirmModal);
    }

    private void showMessageOverlay(String title, String message) {
        confirmTitleLabel.setText(title);
        confirmMessageLabel.setText(message);
        confirmAcceptButton.setText("OK");
        confirmCancelButton.setVisible(false);
        confirmCancelButton.setManaged(false);
        pendingConfirmationAction = null;
        showOverlay(confirmModal);
    }

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

    private void hideOverlays() {
        hideOverlayPanel(optionsModal);
        hideOverlayPanel(aceModal);
        hideOverlayPanel(confirmModal);
        hideOverlayPanel(howToPlayModal);
        overlayLayer.setVisible(false);
        overlayLayer.setManaged(false);
        overlayLayer.setMouseTransparent(true);
    }

    private void hideOverlayPanel(VBox overlay) {
        overlay.setVisible(false);
        overlay.setManaged(false);
    }

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

    private ImageView createCardBackImageView() {
        return createImageView(CARD_BACK_PATH);
    }

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

    private String getSpriteSheetPath(Suit suit) {
        return switch (suit) {
            case DIAMONDS -> CARD_IMAGE_ROOT + "diamonds-top-down-88x124.png";
            case HEARTS -> CARD_IMAGE_ROOT + "hearts-top-down-88x124.png";
            case CLUBS -> CARD_IMAGE_ROOT + "clubs-top-down-88x124.png";
            case SPADES -> CARD_IMAGE_ROOT + "spades-top-down-88x124.png";
        };
    }

    private List<String> getSpecialAcePaths(Suit suit) {
        return switch (suit) {
            case CLUBS -> List.of(SPECIAL_CARD_IMAGE_ROOT + "ace-clubs.png", CARD_IMAGE_ROOT + "as-de-trebol.png");
            case DIAMONDS -> List.of(SPECIAL_CARD_IMAGE_ROOT + "ace-diamonds.png", CARD_IMAGE_ROOT + "as-de-diamante.png");
            case HEARTS -> List.of(SPECIAL_CARD_IMAGE_ROOT + "ace-hearts.png", CARD_IMAGE_ROOT + "as-de-corazon.png");
            case SPADES -> List.of(SPECIAL_CARD_IMAGE_ROOT + "ace-spades.png", CARD_IMAGE_ROOT + "as-de-pica.png");
        };
    }

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
