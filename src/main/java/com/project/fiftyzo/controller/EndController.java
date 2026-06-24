package com.project.fiftyzo.controller;

import com.project.fiftyzo.Main;
import com.project.fiftyzo.model.HumanPlayer;
import com.project.fiftyzo.model.Player;
import java.io.IOException;
import java.util.Objects;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/** Displays the final result and handles restart or application exit. */
public final class EndController {
    @FXML private Label resultTitleLabel;
    @FXML private Label winnerLabel;
    @FXML private Button playAgainButton;

    /** Supplies the winner after the game finishes. */
    public void setWinner(Player winner) {
        boolean humanWon = winner instanceof HumanPlayer;
        resultTitleLabel.setText(humanWon ? "YOU ESCAPED THE ROOM" : "THE ROOM KEPT YOU");
        winnerLabel.setText("Winner: " + winner.getName());
    }

    /** Returns to the player-count selection screen. */
    @FXML
    public void handlePlayAgain() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/project/fiftyzo/view/start-view.fxml")));
            ((Stage) playAgainButton.getScene().getWindow()).setScene(Main.createScene(root));
        } catch (IOException exception) {
            throw new IllegalStateException("The start screen could not be loaded.", exception);
        }
    }

    /** Exits the JavaFX application. */
    @FXML
    public void handleExit() { Platform.exit(); }
}
