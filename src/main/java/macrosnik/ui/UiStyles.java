package macrosnik.ui;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

import java.net.URL;

public final class UiStyles {
    private static final String STYLESHEET_PATH = "/styles/macrosnik.css";

    private UiStyles() {
    }

    public static void apply(Scene scene) {
        if (scene == null) {
            return;
        }
        String stylesheet = stylesheet();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }

    public static void apply(DialogPane dialogPane) {
        if (dialogPane == null) {
            return;
        }
        String stylesheet = stylesheet();
        if (!dialogPane.getStylesheets().contains(stylesheet)) {
            dialogPane.getStylesheets().add(stylesheet);
        }
    }

    private static String stylesheet() {
        URL resource = UiStyles.class.getResource(STYLESHEET_PATH);
        if (resource == null) {
            throw new IllegalStateException("Stylesheet not found: " + STYLESHEET_PATH);
        }
        return resource.toExternalForm();
    }
}
