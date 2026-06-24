package com.project.fiftyzo.controller;

import com.project.fiftyzo.Main;
import com.project.fiftyzo.model.Game;
import java.io.IOException;
import java.util.Objects;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.stage.Stage;

/** Controls player-count selection on the start screen. */
public final class StartController {
    @FXML private ComboBox<Integer> machineCountComboBox;
    @FXML private Button startButton;
    @FXML private Label instructionLabel;

    /** Initializes the allowed machine-player counts. */
    @FXML
    public void initialize() {
        machineCountComboBox.setItems(FXCollections.observableArrayList(1, 2, 3));
        machineCountComboBox.setValue(1);
        instructionLabel.setText("Choose how many machines are trapped in the room with you.");
    }

    /** Starts a new game and transitions to the game screen. */
    @FXML
    public void handleStartGame() {
        Game game = new Game(machineCountComboBox.getValue());
        game.start();
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/com/project/fiftyzo/view/game-view.fxml")));
            Parent root = loader.load();
            loader.<GameController>getController().setGame(game);
            Stage stage = (Stage) startButton.getScene().getWindow();
            stage.setScene(Main.createScene(root));
        } catch (IOException exception) {
            throw new IllegalStateException("The game screen could not be loaded.", exception);
        }
    }
}
