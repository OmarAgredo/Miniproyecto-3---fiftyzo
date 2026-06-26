package com.project.fiftyzo.controller;

import com.project.fiftyzo.FiftyzoApplication;
import com.project.fiftyzo.model.Game;
import java.io.IOException;
import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Controls player-count selection on the start screen. */
public final class StartController {
    @FXML private StackPane startCanvas;
    @FXML private ComboBox<Integer> machineCountComboBox;
    @FXML private Button startButton;
    @FXML private Label instructionLabel;
    private static final double BASE_CANVAS_WIDTH = 1100;
    private static final double BASE_CANVAS_HEIGHT = 760;

    /** Initializes the allowed machine-player counts. */
    @FXML
    public void initialize() {
        startCanvas.sceneProperty().addListener((observable, oldScene, scene) -> {
            if (scene != null) bindStartCanvasScale(scene);
        });
        machineCountComboBox.setItems(FXCollections.observableArrayList(1, 2, 3));
        machineCountComboBox.setValue(1);
        instructionLabel.setText("Choose how many machines are trapped in the room with you.");
    }

    private void bindStartCanvasScale(Scene scene) {
        startCanvas.scaleXProperty().unbind();
        startCanvas.scaleYProperty().unbind();
        DoubleBinding canvasScale = Bindings.createDoubleBinding(
                () -> Math.min(scene.getWidth() / BASE_CANVAS_WIDTH, scene.getHeight() / BASE_CANVAS_HEIGHT),
                scene.widthProperty(),
                scene.heightProperty());
        startCanvas.scaleXProperty().bind(canvasScale);
        startCanvas.scaleYProperty().bind(canvasScale);
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
            stage.setScene(FiftyzoApplication.createScene(root));
        } catch (IOException exception) {
            throw new IllegalStateException("The game screen could not be loaded.", exception);
        }
    }
}
