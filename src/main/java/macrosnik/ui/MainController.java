package macrosnik.ui;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import macrosnik.domain.Macro;
import macrosnik.dsl.MacroDslCodec;
import macrosnik.hotkey.HotkeyService;
import macrosnik.play.MacroPlayer;
import macrosnik.play.PlayerState;
import macrosnik.process.AggregationConfig;
import macrosnik.process.EventAggregator;
import macrosnik.record.RecorderService;
import macrosnik.settings.AppSettings;
import macrosnik.settings.SettingsStorage;
import macrosnik.storage.MacroStorage;

import java.nio.file.Path;
import java.util.Set;

public class MainController {

    private final Stage stage;
    private final BorderPane root = new BorderPane();
    private final TableView<MacroTableRow> table = new TableView<>();
    private final ObservableList<MacroTableRow> rows = FXCollections.observableArrayList();
    private final TextArea dslArea = new TextArea();
    private final Label statusLabel = new Label("Готово");
    private final Label fileLabel = new Label("Файл: не выбран");

    private final MacroStorage storage = new MacroStorage();
    private final MacroPlayer player = new MacroPlayer();
    private final SettingsStorage settingsStorage = new SettingsStorage(Path.of("macros/settings.json"));
    private final AppSettings settings = settingsStorage.loadOrDefault();
    private final RecorderService recorder = new RecorderService();
    private final HotkeyService hotkeys = new HotkeyService(player, settings, this::emergencyStop);
    private final MacroDslCodec dslCodec = new MacroDslCodec();

    private Macro currentMacro = new Macro("Новый макрос");
    private Path currentFilePath;
    private boolean dirty;

    public MainController(Stage stage) {
        this.stage = stage;
        recorder.setIgnoredKeyCodes(resolveIgnoredKeys());

        root.setPadding(new Insets(10));
        root.setTop(buildTopArea());
        root.setCenter(buildTabs());
        root.setBottom(buildBottomBar());

        dslArea.setPromptText("");
        refreshAll();
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

    private Parent buildTopArea() {
        VBox box = new VBox(8, buildMenuBar(), buildToolbar(), fileLabel);
        box.setPadding(new Insets(0, 0, 10, 0));
        return box;
    }

    private Parent buildTabs() {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(new Tab("Макрос", buildTable()));
        tabPane.getTabs().add(new Tab("Сценарий", buildDslPanel()));
        tabPane.getTabs().add(new Tab("Справка", buildHelpPanel()));
        tabPane.getTabs().forEach(tab -> tab.setClosable(false));
        return tabPane;
    }

    private MenuBar buildMenuBar() {
        Menu fileMenu = new Menu("Файл");
        MenuItem itemNew = new MenuItem("Новый");
        MenuItem itemOpen = new MenuItem("Открыть...");
        MenuItem itemSave = new MenuItem("Сохранить");
        MenuItem itemSaveAs = new MenuItem("Сохранить как...");
        fileMenu.getItems().addAll(itemNew, itemOpen, itemSave, itemSaveAs);

        itemNew.setOnAction(e -> newMacro());
        itemOpen.setOnAction(e -> openMacro());
        itemSave.setOnAction(e -> saveMacro());
        itemSaveAs.setOnAction(e -> saveMacroAs());

        Menu macroMenu = new Menu("Макрос");
        MenuItem itemRecordStart = new MenuItem("Начать запись");
        MenuItem itemRecordStop = new MenuItem("Остановить запись");
        MenuItem itemPlay = new MenuItem("Запустить");
        MenuItem itemPause = new MenuItem("Пауза / продолжить");
        MenuItem itemStop = new MenuItem("Стоп");
        macroMenu.getItems().addAll(itemRecordStart, itemRecordStop, itemPlay, itemPause, itemStop);

        itemRecordStart.setOnAction(e -> startRecording());
        itemRecordStop.setOnAction(e -> stopRecording());
        itemPlay.setOnAction(e -> playMacro());
        itemPause.setOnAction(e -> togglePause());
        itemStop.setOnAction(e -> stopPlayback());

        return new MenuBar(fileMenu, macroMenu);
    }

    private Parent buildToolbar() {
        Button btnNew = new Button("Новый");
        Button btnOpen = new Button("Открыть");
        Button btnSave = new Button("Сохранить");
        Button btnSaveAs = new Button("Сохранить как");
        Button btnRecord = new Button("Начать запись");
        Button btnStopRecord = new Button("Остановить запись");
        Button btnPlay = new Button("Запустить");
        Button btnPause = new Button("Пауза / продолжить");
        Button btnStop = new Button("Стоп");
        Button btnFromDsl = new Button("Сценарий → макрос");
        Button btnToDsl = new Button("Макрос → сценарий");

        btnNew.setOnAction(e -> newMacro());
        btnOpen.setOnAction(e -> openMacro());
        btnSave.setOnAction(e -> saveMacro());
        btnSaveAs.setOnAction(e -> saveMacroAs());
        btnRecord.setOnAction(e -> startRecording());
        btnStopRecord.setOnAction(e -> stopRecording());
        btnPlay.setOnAction(e -> playMacro());
        btnPause.setOnAction(e -> togglePause());
        btnStop.setOnAction(e -> stopPlayback());
        btnFromDsl.setOnAction(e -> applyDslToMacro());
        btnToDsl.setOnAction(e -> fillDslFromMacro());

        return new HBox(8,
                btnNew, btnOpen, btnSave, btnSaveAs,
                new Separator(),
                btnRecord, btnStopRecord,
                new Separator(),
                btnPlay, btnPause, btnStop,
                new Separator(),
                btnFromDsl, btnToDsl
        );
    }

    private Parent buildTable() {
        TableColumn<MacroTableRow, String> colType = new TableColumn<>("Тип");
        colType.setCellValueFactory(v -> v.getValue().typeProperty());

        TableColumn<MacroTableRow, String> colDelay = new TableColumn<>("Задержка до действия");
        colDelay.setCellValueFactory(v -> v.getValue().delayProperty());

        TableColumn<MacroTableRow, String> colDetails = new TableColumn<>("Описание");
        colDetails.setCellValueFactory(v -> v.getValue().detailsProperty());

        table.getColumns().setAll(colType, colDelay, colDetails);
        table.setItems(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        colType.setPrefWidth(220);
        colDelay.setPrefWidth(180);
        colDetails.setPrefWidth(620);

        return table;
    }

    private Parent buildDslPanel() {
        dslArea.setWrapText(false);

        Button validateButton = new Button("Проверить сценарий");
        Button applyButton = new Button("Применить к макросу");
        Button exportButton = new Button("Заполнить из макроса");
        applyButton.setDefaultButton(true);

        validateButton.setOnAction(e -> validateDsl());
        applyButton.setOnAction(e -> applyDslToMacro());
        exportButton.setOnAction(e -> fillDslFromMacro());

        HBox topActions = new HBox(8, validateButton, exportButton);
        HBox bottomActions = new HBox(applyButton);
        bottomActions.setAlignment(Pos.CENTER_RIGHT);

        StackPane editorPane = new StackPane(dslArea, createScenarioPlaceholder());
        VBox.setVgrow(editorPane, Priority.ALWAYS);

        VBox box = new VBox(8,
                new Label("Редактор сценария"),
                topActions,
                editorPane,
                bottomActions
        );
        return box;
    }

    private Label createScenarioPlaceholder() {
        Label placeholder = new Label("""
                Пример сценария:
                Подождать мс: 500
                Нажать ЛКМ: 400 300
                Провести мышью: 400 300 -> 700 420 350 мс
                Нажать клавишу: Ввод
                Ввести текст: "Привет"
                Нажать сочетание: Alt+Tab
                """.stripTrailing());
        placeholder.setWrapText(true);
        placeholder.setMouseTransparent(true);
        placeholder.setOpacity(0.45);
        placeholder.maxWidthProperty().bind(dslArea.widthProperty().subtract(24));
        placeholder.visibleProperty().bind(dslArea.textProperty().isEmpty());
        StackPane.setAlignment(placeholder, Pos.TOP_LEFT);
        StackPane.setMargin(placeholder, new Insets(8, 10, 8, 10));
        return placeholder;
    }

    private Parent buildHelpPanel() {
        TextArea help = new TextArea();
        help.setEditable(false);
        help.setWrapText(true);
        help.setText("Основные сценарии:\n\n"
                + "1. Запись: нажмите 'Начать запись', выполните действия, затем 'Остановить запись'.\n"
                + "2. Сохранение: используйте 'Сохранить как', чтобы выбрать свой JSON-файл.\n"
                + "3. Сценарий: на вкладке 'Сценарий' можно описывать макрос текстом.\n\n"
                + "Примеры сценариев:\n"
                + "Подождать мс: 500\n"
                + "Нажать ЛКМ: 400 300\n"
                + "Провести мышью: 400 300 -> 700 420 350 мс\n"
                + "Нажать клавишу: Ввод\n"
                + "Ввести текст: \"Привет\"\n"
                + "Нажать сочетание: Alt+Tab\n"
                + "Зажать клавишу: Ctrl\n"
                + "Отпустить клавишу: Ctrl\n\n"
                + "Горячие клавиши:\n"
                + "Пауза/продолжить: F8\n"
                + "Экстренный стоп: F12");
        return help;
    }

    private Parent buildBottomBar() {
        Label hint = new Label("Горячие клавиши: пауза/продолжить — F8, экстренный стоп — F12");
        VBox box = new VBox(4, hint, statusLabel);
        BorderPane.setMargin(box, new Insets(10, 0, 0, 0));
        return box;
    }

    private void startRecording() {
        try {
            recorder.start();
            status("Запись начата");
        } catch (Exception ex) {
            showError("Не удалось начать запись", ex);
        }
    }

    private void stopRecording() {
        try {
            if (!recorder.isRecording()) {
                status("Запись не активна");
                return;
            }
            currentMacro = new EventAggregator(new AggregationConfig()).aggregate(recorder.stop());
            currentMacro.name = deriveMacroName();
            dirty = true;
            refreshAll();
            status("Запись остановлена, макрос обновлен");
        } catch (Exception ex) {
            showError("Не удалось завершить запись", ex);
        }
    }

    private void playMacro() {
        if (currentMacro.actions == null || currentMacro.actions.isEmpty()) {
            status("Нет действий для воспроизведения");
            return;
        }
        player.play(currentMacro);
        status("Воспроизведение запущено");
    }

    private void togglePause() {
        if (player.getState() == PlayerState.PAUSED) {
            player.resume();
            status("Воспроизведение продолжено");
        } else if (player.getState() == PlayerState.PLAYING) {
            player.pause();
            status("Воспроизведение поставлено на паузу");
        } else {
            status("Воспроизведение сейчас не запущено");
        }
    }

    private void stopPlayback() {
        player.stop();
        status("Воспроизведение остановлено");
    }

    private void newMacro() {
        currentMacro = new Macro("Новый макрос");
        currentFilePath = null;
        dirty = false;
        dslArea.clear();
        refreshAll();
        status("Создан новый макрос");
    }

    private void openMacro() {
        try {
            FileChooser chooser = createMacroFileChooser();
            var selected = chooser.showOpenDialog(stage);
            if (selected == null) {
                return;
            }
            currentFilePath = selected.toPath();
            currentMacro = storage.load(currentFilePath);
            dirty = false;
            refreshAll();
            status("Макрос открыт: " + currentFilePath.getFileName());
        } catch (Exception ex) {
            showError("Не удалось открыть файл макроса", ex);
        }
    }

    private void saveMacro() {
        try {
            if (currentFilePath == null) {
                saveMacroAs();
                return;
            }
            storage.save(currentFilePath, currentMacro);
            dirty = false;
            refreshAll();
            status("Макрос сохранен: " + currentFilePath.getFileName());
        } catch (Exception ex) {
            showError("Не удалось сохранить макрос", ex);
        }
    }

    private void saveMacroAs() {
        try {
            FileChooser chooser = createMacroFileChooser();
            var selected = chooser.showSaveDialog(stage);
            if (selected == null) {
                return;
            }
            currentFilePath = ensureJsonExtension(selected.toPath());
            currentMacro.name = deriveMacroName();
            storage.save(currentFilePath, currentMacro);
            dirty = false;
            refreshAll();
            status("Макрос сохранен в новый файл: " + currentFilePath.getFileName());
        } catch (Exception ex) {
            showError("Не удалось сохранить макрос в новый файл", ex);
        }
    }

    private void validateDsl() {
        try {
            dslCodec.fromDsl(dslArea.getText(), deriveMacroName());
            status("Сценарий корректен");
        } catch (Exception ex) {
            showError("В сценарии обнаружены ошибки", ex);
        }
    }

    private void applyDslToMacro() {
        try {
            currentMacro = dslCodec.fromDsl(dslArea.getText(), deriveMacroName());
            dirty = true;
            refreshAll();
            status("Сценарий успешно преобразован в макрос");
        } catch (Exception ex) {
            showError("Не удалось применить сценарий", ex);
        }
    }

    private void fillDslFromMacro() {
        dslArea.setText(dslCodec.toDsl(currentMacro));
        status("Сценарий обновлен из текущего макроса");
    }

    private void refreshAll() {
        refreshTable();
        updateTitle();
        updateFileLabel();
    }

    private void refreshTable() {
        rows.clear();
        if (currentMacro.actions == null) {
            return;
        }
        for (int i = 0; i < currentMacro.actions.size(); i++) {
            rows.add(MacroTableRow.from(i, currentMacro.actions.get(i)));
        }
    }

    private void updateTitle() {
        String name = deriveMacroName();
        String suffix = dirty ? " *" : "";
        stage.setTitle("MacRosNik — " + name + suffix);
    }

    private void updateFileLabel() {
        fileLabel.setText("Файл: " + (currentFilePath == null ? "не выбран" : currentFilePath.toString()));
    }

    private String deriveMacroName() {
        if (currentFilePath != null) {
            String fileName = currentFilePath.getFileName().toString();
            int dot = fileName.lastIndexOf('.');
            return dot > 0 ? fileName.substring(0, dot) : fileName;
        }
        return currentMacro.name == null || currentMacro.name.isBlank() ? "Новый макрос" : currentMacro.name;
    }

    private Path ensureJsonExtension(Path path) {
        String value = path.toString();
        if (value.toLowerCase().endsWith(".json")) {
            return path;
        }
        return Path.of(value + ".json");
    }

    private FileChooser createMacroFileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выберите файл макроса");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON-файлы макросов", "*.json"));
        if (currentFilePath != null && currentFilePath.getParent() != null) {
            chooser.setInitialDirectory(currentFilePath.getParent().toFile());
            chooser.setInitialFileName(currentFilePath.getFileName().toString());
        }
        return chooser;
    }

    private Set<Integer> resolveIgnoredKeys() {
        int pauseKey = settings.pauseResumeKey != 0 ? settings.pauseResumeKey : NativeKeyEvent.VC_F8;
        int stopKey = settings.stopKey != 0 ? settings.stopKey : NativeKeyEvent.VC_F12;
        return Set.of(pauseKey, stopKey);
    }

    private void status(String text) {
        if (Platform.isFxApplicationThread()) {
            statusLabel.setText(text);
        } else {
            Platform.runLater(() -> statusLabel.setText(text));
        }
    }

    private void showError(String title, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(title);
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? ex.toString() : ex.getMessage();
        if (message.contains("InaccessibleObjectException") || message.contains("module macrosnik does not opens macrosnik.domain.enums")) {
            message = "Не удалось прочитать JSON макроса. Требовалось открыть пакет enums для Jackson. В исправленной версии это уже учтено.";
        }
        alert.setContentText(message);
        alert.showAndWait();
        ex.printStackTrace();
        status(title);
    }

    public void emergencyStop() {
        try {
            player.stop();
        } catch (Exception ignored) {
        }

        try {
            if (recorder.isRecording()) {
                recorder.stop();
            }
        } catch (Exception ignored) {
        }
        status("Выполнена экстренная остановка");
    }
}
