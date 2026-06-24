package com.project.fiftyzo;

import java.io.IOException;
import java.util.Objects;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** JavaFX application entry point for the Fiftyzo game. */
public final class Main extends Application {
    private static final String STYLE_SHEET = "/com/project/fiftyzo/css/style.css";

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(Main.class.getResource("/com/project/fiftyzo/view/start-view.fxml")));
        stage.setTitle("50ZO");
        stage.setMinWidth(960);
        stage.setMinHeight(680);
        stage.setScene(createScene(root));
        stage.show();
    }

    /** Creates a consistently styled application scene. */
    public static Scene createScene(Parent root) {
        Scene scene = new Scene(root, 1100, 760);
        scene.getStylesheets().add(Objects.requireNonNull(Main.class.getResource(STYLE_SHEET)).toExternalForm());
        return scene;
    }

    /** Launches the JavaFX runtime. */
    public static void main(String[] args) { launch(args); }
}
