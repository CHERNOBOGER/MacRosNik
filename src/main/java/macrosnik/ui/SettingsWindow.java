package macrosnik.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SettingsWindow {

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
        title.getStyleClass().add("section-title");

        Label message = new Label("Окно настроек готово для дальнейшей доработки.");
        message.setWrapText(true);
        message.getStyleClass().add("section-subtitle");

        Button closeButton = new Button("Закрыть");
        closeButton.getStyleClass().add("secondary-button");
        closeButton.setOnAction(event -> close());

        Region spacer = new Region();
        spacer.setMinHeight(4);

        VBox root = new VBox(12, title, message, spacer, closeButton);
        root.setPadding(new Insets(18));
        root.setAlignment(Pos.TOP_LEFT);
        root.getStyleClass().addAll("app-root", "panel-card");

        Stage settingsStage = new Stage();
        settingsStage.initOwner(owner);
        settingsStage.setTitle("MacRosNik - Настройки");
        Scene scene = new Scene(root, 420, 180);
        UiStyles.apply(scene);
        settingsStage.setScene(scene);
        settingsStage.setMinWidth(360);
        settingsStage.setMinHeight(160);
        return settingsStage;
    }
}
