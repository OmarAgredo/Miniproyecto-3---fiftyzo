package com.project.fiftyzo.controller;

import com.project.fiftyzo.FiftyzoApplication;
import com.project.fiftyzo.model.HumanPlayer;
import com.project.fiftyzo.model.Player;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Displays the final result and handles restart or application exit. */
public final class EndController {
    @FXML private StackPane endCanvas;
    @FXML private Label resultTitleLabel;
    @FXML private Label winnerLabel;
    @FXML private VBox finalLogBox;
    @FXML private Button playAgainButton;
    private static final double BASE_CANVAS_WIDTH = 1100;
    private static final double BASE_CANVAS_HEIGHT = 760;

    @FXML
    public void initialize() {
        endCanvas.sceneProperty().addListener((observable, oldScene, scene) -> {
            if (scene != null) bindEndCanvasScale(scene);
        });
    }

    private void bindEndCanvasScale(Scene scene) {
        endCanvas.scaleXProperty().unbind();
        endCanvas.scaleYProperty().unbind();
        DoubleBinding canvasScale = Bindings.createDoubleBinding(
                () -> Math.min(scene.getWidth() / BASE_CANVAS_WIDTH, scene.getHeight() / BASE_CANVAS_HEIGHT),
                scene.widthProperty(),
                scene.heightProperty());
        endCanvas.scaleXProperty().bind(canvasScale);
        endCanvas.scaleYProperty().bind(canvasScale);
    }

    /** Supplies the winner after the game finishes. */
    public void setWinner(Player winner) {
        setResult(winner, List.of());
    }

    /**
     * Supplies the winner and final event log after the game finishes.
     *
     * @param winner the only remaining active player.
     * @param finalLog chronological event messages from the game screen.
     */
    public void setResult(Player winner, List<String> finalLog) {
        boolean humanWon = winner instanceof HumanPlayer;
        resultTitleLabel.setText(humanWon ? "YOU ESCAPED THE ROOM" : "THE ROOM KEPT YOU");
        resultTitleLabel.getStyleClass().removeAll("end-win-title", "end-loss-title");
        resultTitleLabel.getStyleClass().add(humanWon ? "end-win-title" : "end-loss-title");
        winnerLabel.setText("Winner: " + winner.getName());
        renderFinalLog(finalLog);
    }

    private void renderFinalLog(List<String> finalLog) {
        finalLogBox.getChildren().clear();
        if (finalLog == null || finalLog.isEmpty()) {
            finalLogBox.getChildren().add(createLogLabel("No final log messages were recorded."));
            return;
        }
        for (String message : finalLog) {
            finalLogBox.getChildren().add(createLogLabel(message));
        }
    }

    private Label createLogLabel(String message) {
        Label label = new Label(message);
        label.setWrapText(true);
        label.getStyleClass().addAll("log-entry", "end-log-entry");
        return label;
    }

    /** Returns to the player-count selection screen. */
    @FXML
    public void handlePlayAgain() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/project/fiftyzo/view/start-view.fxml")));
            ((Stage) playAgainButton.getScene().getWindow()).setScene(FiftyzoApplication.createScene(root));
        } catch (IOException exception) {
            throw new IllegalStateException("The start screen could not be loaded.", exception);
        }
    }

    /** Exits the JavaFX application. */
    @FXML
    public void handleExit() { Platform.exit(); }
}
