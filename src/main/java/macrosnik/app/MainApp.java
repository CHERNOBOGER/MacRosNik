package macrosnik.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import macrosnik.hotkey.PackagedJNativeHookLibraryLocator;
import macrosnik.ui.MainController;
import macrosnik.ui.UiStyles;

import java.io.InputStream;

public class MainApp extends Application {
    private static final String APP_ICON_RESOURCE = "/icons/app.png";

    @Override
    public void start(Stage stage) {
        MainController controller = new MainController(stage);

        Scene scene = new Scene(controller.root(), 1140, 666);
        UiStyles.apply(scene);
        stage.setTitle("MacRosNik");
        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        applyWindowIcon(stage);
        stage.setOnCloseRequest(e -> controller.shutdown());
        stage.show();

    }

    public static void main(String[] args) {
        if (System.getProperty("jnativehook.lib.locator") == null) {
            System.setProperty("jnativehook.lib.locator", PackagedJNativeHookLibraryLocator.class.getName());
        }
        launch(args);
    }

    private void applyWindowIcon(Stage stage) {
        try (InputStream inputStream = MainApp.class.getResourceAsStream(APP_ICON_RESOURCE)) {
            if (inputStream != null) {
                stage.getIcons().add(new Image(inputStream));
            }
        } catch (Exception ignored) {
        }
    }
}
