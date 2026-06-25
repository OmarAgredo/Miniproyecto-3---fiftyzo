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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;
import javafx.util.Duration;

/** Renders game state and delegates player actions to the model. */
public final class GameController {
    @FXML private HBox machinePlayersContainer;
    @FXML private Label currentSumLabel;
    @FXML private Label turnMessageLabel;
    @FXML private VBox deckContainer;
    @FXML private VBox tableCardContainer;
    @FXML private VBox eventLogBox;
    @FXML private HBox humanHandContainer;
    @FXML private ScrollPane eventLogScrollPane;
    @FXML private Label humanNameLabel;

    private Game game;
    private boolean machineTurnScheduled;
    private boolean endScreenShown;
    private boolean gameOverReasonLogged;
    private final List<String> eventMessages = new ArrayList<>();
    private static final double CARD_WIDTH = 88;
    private static final double CARD_HEIGHT = 124;
    private static final double DECK_IMAGE_HEIGHT = 140;
    private static final double DECK_IMAGE_SCALE = 0.93;
    private static final double FACE_UP_VIEWPORT_INSET = 3;
    private static final int SPRITE_COLUMNS = 5;
    private static final String CARD_IMAGE_ROOT = "/com/project/fiftyzo/images/cards/";
    private static final String CARD_BACK_PATH = CARD_IMAGE_ROOT + "machine-card-back-red.png";
    private static final String DECK_IMAGE_PATH = CARD_IMAGE_ROOT + "deck-pile-red.png";

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
            VBox panel = new VBox(7);
            panel.getStyleClass().add("machine-panel");
            panel.getChildren().add(new Label(machine.getName()));
            if (!machine.isActive()) {
                panel.getStyleClass().add("machine-eliminated");
                panel.getChildren().add(new Label("ELIMINATED"));
            } else {
                HBox hand = new HBox(5);
                for (Card ignored : machine.getHandSnapshot()) hand.getChildren().add(createCardView(null, false, false));
                panel.getChildren().add(hand);
            }
            machinePlayersContainer.getChildren().add(panel);
        }
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
        VBox tableCardGroup = new VBox(12);
        tableCardGroup.setAlignment(Pos.CENTER);
        tableCardGroup.getStyleClass().add("table-card-group");
        tableCardGroup.getChildren().add(new Label("TABLE CARD"));
        StackPane tableCard = createCardView(game.getTable().getTopCard(), true, false);
        tableCard.getStyleClass().add("table-card");
        tableCardGroup.getChildren().add(tableCard);
        tableCardContainer.getChildren().add(tableCardGroup);
        int sum = game.getTable().getCurrentSum();
        currentSumLabel.setText(String.valueOf(sum));
        currentSumLabel.getStyleClass().removeAll("sum-safe", "sum-warning", "sum-danger");
        currentSumLabel.getStyleClass().add(sum >= 45 ? "sum-danger" : sum >= 40 ? "sum-warning" : "sum-safe");
    }

    /** Renders the remaining deck count. */
    public void renderDeck() {
        deckContainer.getChildren().clear();
        deckContainer.getChildren().add(new Label("DECK"));
        deckContainer.getChildren().add(createDeckView());
        deckContainer.getChildren().add(new Label(game.getDeck().size() + " cards remaining"));
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
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Choose Ace Value");
        dialog.setHeaderText("Choose how the Ace should affect the table sum.");
        ButtonType one = new ButtonType("Use as 1");
        ButtonType ten = new ButtonType("Use as 10");
        dialog.getDialogPane().getButtonTypes().addAll(one, ten, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == one ? 1 : button == ten ? 10 : null);
        dialog.showAndWait().ifPresent(value -> playHumanCard(ace, value));
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
        view.getStyleClass().addAll("card", "card-back");
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

    private ImageView createCardImageView(Card card) {
        if (card == null) return null;
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
    private void showError(String title, String message) { Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK); alert.setTitle(title); alert.setHeaderText(null); alert.showAndWait(); }
}
