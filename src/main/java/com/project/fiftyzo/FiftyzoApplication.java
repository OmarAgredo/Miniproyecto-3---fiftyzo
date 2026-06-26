package com.project.fiftyzo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * JavaFX application class for the 50ZO game.
 * It loads shared fonts, opens the start screen, and creates scenes with the common stylesheet.
 */
public final class FiftyzoApplication extends Application {
    private static final String START_VIEW = "/com/project/fiftyzo/view/start-view.fxml";
    private static final String STYLE_SHEET = "/com/project/fiftyzo/css/style.css";

    /**
     * Initializes the primary stage and displays the start screen.
     *
     * @param stage primary JavaFX stage supplied by the runtime
     * @throws IOException if the start view cannot be loaded
     */
    @Override
    public void start(Stage stage) throws IOException {
        loadCustomFonts();
        Parent root = FXMLLoader.load(Objects.requireNonNull(FiftyzoApplication.class.getResource(START_VIEW)));
        stage.setTitle("50ZO");
        stage.setMinWidth(960);
        stage.setMinHeight(680);
        stage.setScene(createScene(root));
        stage.show();
    }

    /**
     * Creates a consistently styled application scene.
     *
     * @param root root node loaded from an FXML view.
     * @return a styled JavaFX scene.
     */
    public static Scene createScene(Parent root) {
        Scene scene = new Scene(root, 1100, 760);
        scene.getStylesheets().add(Objects.requireNonNull(FiftyzoApplication.class.getResource(STYLE_SHEET)).toExternalForm());
        return scene;
    }

    /**
     * Loads the custom fonts used by the themed JavaFX CSS.
     */
    private static void loadCustomFonts() {
        loadFont("/com/project/fiftyzo/fonts/arcade-raiders.ttf");
        loadFont("/com/project/fiftyzo/fonts/comic-w.ttf");
    }

    /**
     * Attempts to load one font resource into the JavaFX font registry.
     *
     * @param resourcePath classpath path to the font resource
     */
    private static void loadFont(String resourcePath) {
        try (InputStream stream = FiftyzoApplication.class.getResourceAsStream(resourcePath)) {
            if (stream != null) Font.loadFont(stream, 12);
        } catch (IOException ignored) {
            // Font loading is cosmetic; CSS fallbacks keep the UI readable.
        }
    }
}
