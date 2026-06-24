package com.project.fiftyzo.controller;

import com.project.fiftyzo.Main;
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
import java.util.List;
import java.util.Objects;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
        tableCardContainer.getChildren().add(new Label("TABLE CARD"));
        tableCardContainer.getChildren().add(createCardView(game.getTable().getTopCard(), true, false));
        int sum = game.getTable().getCurrentSum();
        currentSumLabel.setText(String.valueOf(sum));
        currentSumLabel.getStyleClass().removeAll("sum-safe", "sum-warning", "sum-danger");
        currentSumLabel.getStyleClass().add(sum >= 45 ? "sum-danger" : sum >= 40 ? "sum-warning" : "sum-safe");
    }

    /** Renders the remaining deck count. */
    public void renderDeck() {
        deckContainer.getChildren().clear();
        deckContainer.getChildren().add(new Label("DECK"));
        deckContainer.getChildren().add(createCardView(null, false, false));
        deckContainer.getChildren().add(new Label(game.getDeck().size() + " cards remaining"));
    }

    /** Adds one message to the on-screen event log. */
    public void appendLog(String message) {
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
        if (game.isGameOver()) { showEndScreen(); return; }
        Player current = game.getCurrentPlayer();
        if (!current.hasPlayableCard(game.getTable().getCurrentSum())) {
            String playerName = current.getName();
            game.processNoPlayableCardForCurrentPlayer();
            appendLog(playerName + " was eliminated because no card could be played.");
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
        if (!machineTurnScheduled) scheduleMachineTurn();
    }

    /** Loads the end screen after the model identifies a winner. */
    public void showEndScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/com/project/fiftyzo/view/end-view.fxml")));
            Parent root = loader.load();
            loader.<EndController>getController().setWinner(game.getWinner());
            ((Stage) currentSumLabel.getScene().getWindow()).setScene(Main.createScene(root));
        } catch (IOException exception) {
            throw new IllegalStateException("The end screen could not be loaded.", exception);
        }
    }

    private void scheduleMachineTurn() {
        machineTurnScheduled = true;
        PauseTransition pause = new PauseTransition(Duration.millis(550));
        pause.setOnFinished(event -> {
            machineTurnScheduled = false;
            if (game.isGameOver() || !(game.getCurrentPlayer() instanceof MachinePlayer)) return;
            try {
                boolean deckWasEmpty = game.getDeck().isEmpty();
                PlayResult result = game.playMachineTurn();
                appendPlayResult(result);
                if (deckWasEmpty) appendLog("Deck was rebuilt from table cards.");
                renderGameState();
                continueTurnFlow();
            } catch (NoPlayableCardException | InvalidMoveException exception) {
                showError("Machine turn failed", exception.getMessage());
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

    private void appendPlayResult(PlayResult result) {
        if (result.isPlayerEliminated()) appendLog(result.getPlayer().getName() + " was eliminated.");
        else appendLog(result.getPlayer().getName() + " played " + formatCard(result.getPlayedCard()) + " as " + result.getSelectedValue() + ". Current sum: " + result.getNewSum() + ".");
    }

    private StackPane createCardView(Card card, boolean faceUp, boolean valid) {
        StackPane view = new StackPane();
        view.getStyleClass().add("card");
        view.setPrefSize(88, 124);
        view.setMinSize(88, 124);
        if (!faceUp) {
            view.getStyleClass().add("card-back");
            view.getChildren().add(new Label("50ZO"));
        } else {
            VBox content = new VBox(4);
            content.getStyleClass().add("card-content");
            content.getChildren().addAll(new Label(rankSymbol(card.getRank())), new Label(suitSymbol(card.getSuit())));
            view.getChildren().add(content);
            view.getStyleClass().add(valid ? "card-valid" : "card-invalid");
        }
        return view;
    }

    private String formatCard(Card card) { return rankSymbol(card.getRank()) + suitSymbol(card.getSuit()); }
    private String rankSymbol(Rank rank) {
        return switch (rank) {
            case JACK -> "J"; case QUEEN -> "Q"; case KING -> "K"; case ACE -> "A";
            default -> String.valueOf(rank.ordinal() + 2);
        };
    }
    private String suitSymbol(Suit suit) { return switch (suit) { case HEARTS -> "♥"; case DIAMONDS -> "♦"; case CLUBS -> "♣"; case SPADES -> "♠"; }; }
    private void showError(String title, String message) { Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK); alert.setTitle(title); alert.setHeaderText(null); alert.showAndWait(); }
}
