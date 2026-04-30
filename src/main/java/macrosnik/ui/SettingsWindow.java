package macrosnik.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SettingsWindow {
    private static final String WINDOW_TITLE = "MacRosNik - Настройки";
    private static final String PLACEHOLDER_TEXT = "Настройки пока не реализованы.";

    private final Stage owner;
    private Stage stage;

    public SettingsWindow(Stage owner) {
        this.owner = owner;
    }

    public void show() {
        if (stage == null) {
            stage = createStage();
        }
        if (!stage.isShowing()) {
            stage.show();
        }
        stage.toFront();
        stage.requestFocus();
    }

    public void close() {
        if (stage != null) {
            stage.close();
        }
    }

    private Stage createStage() {
        Label title = new Label("Настройки");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label message = new Label(PLACEHOLDER_TEXT);
        message.setWrapText(true);

        Button closeButton = new Button("Закрыть");
        closeButton.setOnAction(event -> close());

        VBox root = new VBox(12, title, message, closeButton);
        root.setPadding(new Insets(18));
        root.setAlignment(Pos.TOP_LEFT);

        Stage settingsStage = new Stage();
        settingsStage.initOwner(owner);
        settingsStage.setTitle(WINDOW_TITLE);
        settingsStage.setScene(new Scene(root, 420, 160));
        settingsStage.setMinWidth(360);
        settingsStage.setMinHeight(150);
        return settingsStage;
    }
}
