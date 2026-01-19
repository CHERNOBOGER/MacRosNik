package macrosnik.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import macrosnik.domain.Macro;
import macrosnik.play.MacroPlayer;
import macrosnik.storage.MacroStorage;
import macrosnik.hotkey.HotkeyService;
import macrosnik.settings.AppSettings;
import macrosnik.settings.SettingsStorage;
import macrosnik.record.RecorderService;

import java.nio.file.Path;

public class MainController {

    private final BorderPane root = new BorderPane();
    private final TableView<MacroTableRow> table = new TableView<>();
    private final ObservableList<MacroTableRow> rows = FXCollections.observableArrayList();

    private final MacroStorage storage = new MacroStorage();
    private final MacroPlayer player = new MacroPlayer();
    private final SettingsStorage settingsStorage = new SettingsStorage(Path.of("macros/settings.json"));
    private final AppSettings settings = settingsStorage.loadOrDefault();
    private final HotkeyService hotkeys = new HotkeyService(player, settings, this::emergencyStop);
    private final RecorderService recorder = new RecorderService();

    private Macro currentMacro = new Macro("Demo");

    public MainController() {
        root.setPadding(new Insets(10));

        root.setTop(buildToolbar());
        root.setCenter(buildTable());
        root.setBottom(buildBottomBar());

        refreshTable();
    }

    public Parent root() {
        return root;
    }

    public void startHotkeys() {
        hotkeys.start();
    }

    public void shutdown() {
        hotkeys.close();
    }


    private Parent buildToolbar() {
        Button btnLoad = new Button("Загрузить");
        Button btnSave = new Button("Сохранить");
        Button btnPlay = new Button("Запустить");
        Button btnPause = new Button("Пауза");
        Button btnStop = new Button("Остановка");
        Button btnRecord = new Button("Запись");
        Button btnStopRec = new Button("Остановить запись");

        btnRecord.setOnAction(e -> {
            recorder.start();
            btnPlay.setDisable(true);
            btnPause.setDisable(true);
        });

        btnStopRec.setOnAction(e -> {
            var raw = recorder.stop();

            var aggregator = new macrosnik.process.EventAggregator(
                    new macrosnik.process.AggregationConfig()
            );
            currentMacro = aggregator.aggregate(raw);
            refreshTable();

            btnPlay.setDisable(false);
            btnPause.setDisable(false);
        });

        btnLoad.setOnAction(e -> {
            try {
                // временно фиксированный путь
                currentMacro = storage.load(Path.of("macros/demo.json"));
                refreshTable();
            } catch (Exception ex) {
                showError(ex);
            }
        });

        btnSave.setOnAction(e -> {
            try {
                storage.save(Path.of("macros/demo.json"), currentMacro);
            } catch (Exception ex) {
                showError(ex);
            }
        });

        btnPlay.setOnAction(e -> player.play(currentMacro));
        btnPause.setOnAction(e -> {
            if (player.getState() == macrosnik.play.PlayerState.PAUSED) player.resume();
            else player.pause();
        });
        btnStop.setOnAction(e -> player.stop());

        HBox bar = new HBox(
                8,
                btnLoad, btnSave,
                new Separator(),
                btnRecord, btnStopRec,
                new Separator(),
                btnPlay, btnPause, btnStop
        );
        bar.setPadding(new Insets(0, 0, 10, 0));
        return bar;


    }

    private Parent buildTable() {
        TableColumn<MacroTableRow, String> colType = new TableColumn<>("Тип");
        colType.setCellValueFactory(v -> v.getValue().typeProperty());

        TableColumn<MacroTableRow, String> colDelay = new TableColumn<>("Задержка");
        colDelay.setCellValueFactory(v -> v.getValue().delayProperty());

        TableColumn<MacroTableRow, String> colDetails = new TableColumn<>("Детали");
        colDetails.setCellValueFactory(v -> v.getValue().detailsProperty());

        table.getColumns().addAll(colType, colDelay, colDetails);
        table.setItems(rows);

        colType.setPrefWidth(180);
        colDelay.setPrefWidth(140);
        colDetails.setPrefWidth(540);

        return table;
    }

    private Parent buildBottomBar() {
        Label hint = new Label("Пауза (F8), Остановка (F12)");
        BorderPane.setMargin(hint, new Insets(10, 0, 0, 0));
        return hint;
    }

    private void refreshTable() {
        rows.clear();
        for (int i = 0; i < currentMacro.actions.size(); i++) {
            rows.add(MacroTableRow.from(i + 1, currentMacro.actions.get(i)));
        }
    }

    private void showError(Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(ex.getClass().getSimpleName());
        alert.setContentText(ex.getMessage());
        alert.showAndWait();
        ex.printStackTrace();
    }

    public void emergencyStop() {
        try {
            player.stop();
        } catch (Exception ignored) {}

        try {
            if (recorder.isRecording()) {
                recorder.stop();
            }
        } catch (Exception ignored) {}
    }
}
