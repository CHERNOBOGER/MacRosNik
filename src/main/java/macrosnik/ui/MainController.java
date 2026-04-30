package macrosnik.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import macrosnik.domain.Macro;
import macrosnik.dsl.MacroDslCodec;
import macrosnik.hotkey.HotkeyService;
import macrosnik.play.MacroPlayer;
import macrosnik.play.PlayerState;
import macrosnik.process.EventAggregator;
import macrosnik.record.RecorderService;
import macrosnik.storage.MacroStorage;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.IntStream;

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
    private final RecorderService recorder = new RecorderService();
    private final SettingsWindow settingsWindow;
    private final InspectorWindow inspectorWindow;
    private final ToolbarButtons toolbarButtons = new ToolbarButtons();
    private final HotkeyService hotkeys = new HotkeyService(
            player,
            HotkeyService.DEFAULT_PAUSE_RESUME_KEY,
            HotkeyService.DEFAULT_STOP_KEY,
            this::emergencyStop
    );
    private final MacroDslCodec dslCodec = new MacroDslCodec();

    private final MacroDocument document = new MacroDocument();

    public MainController(Stage stage) {
        this.stage = stage;
        this.settingsWindow = new SettingsWindow(stage);
        this.inspectorWindow = new InspectorWindow(stage);
        recorder.setIgnoredKeyCodes(resolveIgnoredKeys());
        player.setStateListener(state -> Platform.runLater(this::refreshUiState));
        player.setErrorListener(error -> Platform.runLater(
                () -> status("Macro error: " + readableMessage(error))
        ));

        root.setPadding(new Insets(10));
        root.setTop(buildTopArea());
        root.setCenter(buildTabs());
        root.setBottom(buildBottomBar());

        dslArea.setPromptText("");
        refreshAll();
        updateControls();
    }

    public Parent root() {
        return root;
    }

    public void startHotkeys() {
        try {
            hotkeys.start();
        } catch (Throwable ex) {
            status("Горячие клавиши не запущены: " + readableMessage(ex));
        }
    }

    public void shutdown() {
        if (recorder.isRecording()) {
            recorder.stop();
        }
        player.stop();
        settingsWindow.close();
        inspectorWindow.close();
        hotkeys.close();
    }

    private Parent buildTopArea() {
        VBox box = new VBox(8, buildHeaderBar(), buildActionBar());
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

        bindAction(itemNew, this::newMacro);
        bindAction(itemOpen, this::openMacro);
        bindAction(itemSave, this::saveMacro);
        bindAction(itemSaveAs, this::saveMacroAs);

        Menu macroMenu = new Menu("Макрос");
        MenuItem itemRecordStart = new MenuItem("Начать запись");
        MenuItem itemRecordStop = new MenuItem("Остановить запись");
        MenuItem itemPlay = new MenuItem("Запустить");
        MenuItem itemPause = new MenuItem("Пауза / продолжить");
        MenuItem itemStop = new MenuItem("Стоп");
        macroMenu.getItems().addAll(itemRecordStart, itemRecordStop, itemPlay, itemPause, itemStop);

        bindAction(itemRecordStart, this::startRecording);
        bindAction(itemRecordStop, this::stopRecording);
        bindAction(itemPlay, this::playMacro);
        bindAction(itemPause, this::togglePause);
        bindAction(itemStop, this::stopPlayback);

        Menu toolsMenu = new Menu("Инструменты");
        MenuItem itemInspector = new MenuItem("Инспектор координат и цвета");
        toolsMenu.getItems().add(itemInspector);
        bindAction(itemInspector, this::openInspector);
        return new MenuBar(fileMenu, macroMenu, toolsMenu);
    }

    private Parent buildHeaderBar() {
        MenuBar menuBar = buildMenuBar();
        Button settingsButton = new Button("Настройки");
        bindAction(settingsButton, this::openSettings);
        Button inspectorButton = new Button("Инспектор");
        bindAction(inspectorButton, this::openInspector);
        HBox rightActions = new HBox(8, inspectorButton, settingsButton);
        rightActions.setAlignment(Pos.CENTER_RIGHT);

        BorderPane headerBar = new BorderPane();
        headerBar.setLeft(menuBar);
        headerBar.setRight(rightActions);
        BorderPane.setAlignment(rightActions, Pos.CENTER_RIGHT);
        return headerBar;
    }

    private Parent buildActionBar() {
        fileLabel.setMaxWidth(Double.MAX_VALUE);
        fileLabel.setAlignment(Pos.CENTER_RIGHT);
        fileLabel.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);

        BorderPane actionBar = new BorderPane();
        actionBar.setLeft(buildToolbar());
        actionBar.setCenter(fileLabel);
        BorderPane.setAlignment(fileLabel, Pos.CENTER_RIGHT);
        BorderPane.setMargin(fileLabel, new Insets(0, 0, 0, 12));
        return actionBar;
    }

    private HBox buildToolbar() {
        Button btnRecord = new Button("Начать запись");
        Button btnStopRecord = new Button("Остановить запись");
        Button btnPlay = new Button("Запустить");
        Button btnPause = new Button("Пауза / продолжить");
        Button btnStop = new Button("Стоп");
        Button btnFromDsl = new Button("Сценарий → макрос");
        Button btnToDsl = new Button("Макрос → сценарий");

        bindAction(btnRecord, this::startRecording);
        bindAction(btnStopRecord, this::stopRecording);
        bindAction(btnPlay, this::playMacro);
        bindAction(btnPause, this::togglePause);
        bindAction(btnStop, this::stopPlayback);
        bindAction(btnFromDsl, this::applyDslToMacro);
        bindAction(btnToDsl, this::fillDslFromMacro);

        toolbarButtons.initialize(btnRecord, btnStopRecord, btnPlay, btnPause, btnStop);

        HBox toolbar = new HBox(8, btnRecord, btnStopRecord, btnPlay, btnPause, btnStop, btnFromDsl, btnToDsl);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        return toolbar;
    }

    @SuppressWarnings("unchecked")
    private Parent buildTable() {
        TableColumn<MacroTableRow, String> colType = new TableColumn<>("Тип");
        colType.setCellValueFactory(v -> v.getValue().typeProperty());

        TableColumn<MacroTableRow, String> colDelay = new TableColumn<>("Задержка до действия");
        colDelay.setCellValueFactory(v -> v.getValue().delayProperty());

        TableColumn<MacroTableRow, String> colDetails = new TableColumn<>("Описание");
        colDetails.setCellValueFactory(v -> v.getValue().detailsProperty());

        table.getColumns().setAll(colType, colDelay, colDetails);
        table.setItems(rows);
        table.setColumnResizePolicy(constrainedResizePolicy());

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

        bindAction(validateButton, this::validateDsl);
        bindAction(applyButton, this::applyDslToMacro);
        bindAction(exportButton, this::fillDslFromMacro);

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
            refreshUiState();
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
            document.replaceMacro(new EventAggregator().aggregate(recorder.stop()));
            document.syncMacroNameToFile();
            document.markDirty();
            refreshAll();
            refreshUiState();
            status("Запись остановлена, макрос обновлен");
        } catch (Exception ex) {
            showError("Не удалось завершить запись", ex);
        }
    }

    private void playMacro() {
        if (!hasMacroActions()) {
            status("Нет действий для воспроизведения");
            return;
        }
        if (recorder.isRecording()) {
            status("Нельзя запустить макрос во время записи");
            return;
        }
        player.play(document.macro());
        refreshUiState();
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
        refreshUiState();
        status("Воспроизведение остановлено");
    }

    private void newMacro() {
        document.reset();
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
            document.setFilePath(selected.toPath());
            document.replaceMacro(storage.load(document.filePath()));
            document.markClean();
            refreshAll();
            Path currentFilePath = document.filePath();
            status("Макрос открыт: " + currentFilePath.getFileName());
        } catch (Exception ex) {
            showError("Не удалось открыть файл макроса", ex);
        }
    }

    private void saveMacro() {
        try {
            if (document.filePath() == null) {
                saveMacroAs();
                return;
            }
            storage.save(document.filePath(), document.macro());
            document.markClean();
            refreshAll();
            Path currentFilePath = document.filePath();
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
            document.setFilePath(document.ensureJsonExtension(selected.toPath()));
            document.syncMacroNameToFile();
            storage.save(document.filePath(), document.macro());
            document.markClean();
            refreshAll();
            Path currentFilePath = document.filePath();
            status("Макрос сохранен в новый файл: " + currentFilePath.getFileName());
        } catch (Exception ex) {
            showError("Не удалось сохранить макрос в новый файл", ex);
        }
    }

    private void openSettings() {
        settingsWindow.show();
        status("Открыто окно настроек");
    }

    private void openInspector() {
        inspectorWindow.show();
        status("Открыто окно инспектора");
    }

    private void validateDsl() {
        try {
            dslCodec.fromDsl(dslArea.getText(), document.displayName());
            status("Сценарий корректен");
        } catch (Exception ex) {
            showError("В сценарии обнаружены ошибки", ex);
        }
    }

    private void applyDslToMacro() {
        try {
            document.replaceMacro(dslCodec.fromDsl(dslArea.getText(), document.displayName()));
            document.markDirty();
            refreshAll();
            status("Сценарий успешно преобразован в макрос");
        } catch (Exception ex) {
            showError("Не удалось применить сценарий", ex);
        }
    }

    private void fillDslFromMacro() {
        dslArea.setText(dslCodec.toDsl(document.macro()));
        status("Сценарий обновлен из текущего макроса");
    }

    private void refreshAll() {
        refreshTable();
        updateTitle();
        updateFileLabel();
    }

    private void refreshTable() {
        rows.clear();
        if (document.macro().actions == null) {
            return;
        }
        rows.setAll(IntStream.range(0, document.macro().actions.size())
                .mapToObj(i -> MacroTableRow.from(i, document.macro().actions.get(i)))
                .toList());
    }

    private void updateTitle() {
        stage.setTitle(document.windowTitle());
    }

    private void updateFileLabel() {
        fileLabel.setText(document.fileLabel());
    }

    private FileChooser createMacroFileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выберите файл макроса");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON-файлы макросов", "*.json"));
        if (document.filePath() != null && document.filePath().getParent() != null) {
            chooser.setInitialDirectory(document.filePath().getParent().toFile());
            chooser.setInitialFileName(document.filePath().getFileName().toString());
        }
        return chooser;
    }

    private Set<Integer> resolveIgnoredKeys() {
        return Set.of(HotkeyService.DEFAULT_PAUSE_RESUME_KEY, HotkeyService.DEFAULT_STOP_KEY);
    }

    private void syncHotkeyRegistration() {
        boolean shouldRegister = recorder.isRecording() || isPlayerActive(player.getState());

        try {
            if (shouldRegister) {
                hotkeys.start();
            } else {
                hotkeys.close();
            }
        } catch (Throwable ex) {
            status("Горячие клавиши не запущены: " + readableMessage(ex));
        }
    }

    private void updateControls() {
        if (!toolbarButtons.isInitialized()) {
            return;
        }
        PlayerState state = player.getState();
        boolean playing = state == PlayerState.PLAYING;
        boolean paused = state == PlayerState.PAUSED;
        boolean recording = recorder.isRecording();

        toolbarButtons.update(recording, playing, paused);
    }

    private void refreshUiState() {
        syncHotkeyRegistration();
        updateControls();
    }

    private boolean hasMacroActions() {
        return document.hasActions();
    }

    private boolean isPlayerActive(PlayerState state) {
        return state == PlayerState.PLAYING || state == PlayerState.PAUSED;
    }

    private void bindAction(MenuItem item, Runnable action) {
        item.setOnAction(event -> action.run());
    }

    private void bindAction(ButtonBase button, Runnable action) {
        button.setOnAction(event -> action.run());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Callback<TableView.ResizeFeatures, Boolean> constrainedResizePolicy() {
        return (Callback) TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS;
    }

    private void status(String text) {
        if (Platform.isFxApplicationThread()) {
            statusLabel.setText(text);
        } else {
            Platform.runLater(() -> statusLabel.setText(text));
        }
    }

    private String readableMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return (message == null || message.isBlank()) ? current.getClass().getSimpleName() : message;
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
        refreshUiState();
        status("Выполнена экстренная остановка");
    }

    private static final class ToolbarButtons {
        private Button record;
        private Button stopRecord;
        private Button play;
        private Button pause;
        private Button stop;

        private void initialize(Button record, Button stopRecord, Button play, Button pause, Button stop) {
            this.record = record;
            this.stopRecord = stopRecord;
            this.play = play;
            this.pause = pause;
            this.stop = stop;
        }

        private boolean isInitialized() {
            return record != null
                    && stopRecord != null
                    && play != null
                    && pause != null
                    && stop != null;
        }

        private void update(boolean recording, boolean playing, boolean paused) {
            play.setDisable(playing || paused || recording);
            pause.setDisable(!playing && !paused);
            stop.setDisable(!playing && !paused);
            record.setDisable(recording || playing || paused);
            stopRecord.setDisable(!recording);
        }
    }
}
