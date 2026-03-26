package macrosnik.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import macrosnik.ui.MainController;
import macrosnik.hotkey.PackagedJNativeHookLibraryLocator;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        MainController controller = new MainController(stage);

        Scene scene = new Scene(controller.root(), 1234, 666);
        stage.setTitle("MacRosNik");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> controller.shutdown());
        stage.show();

        controller.startHotkeys();
    }

    public static void main(String[] args) {
        if (System.getProperty("jnativehook.lib.locator") == null) {
            System.setProperty("jnativehook.lib.locator", PackagedJNativeHookLibraryLocator.class.getName());
        }
        launch(args);
    }
}
