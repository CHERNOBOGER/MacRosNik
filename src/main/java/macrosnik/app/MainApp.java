package macrosnik.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import macrosnik.ui.MainController;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        MainController controller = new MainController(stage);

        Scene scene = new Scene(controller.root(), 1050, 680);
        stage.setTitle("MacRosNik");
        stage.setScene(scene);

        controller.startHotkeys();

        stage.setOnCloseRequest(e -> controller.shutdown());
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
