package macrosnik.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
import macrosnik.storage.MacroStorage;

import java.nio.file.Path;
import java.util.Set;

public class MainController {

    private final Stage stage;
    private final BorderPane root = new BorderPane();
    private final TableView<MacroTableRow> table = new TableView<>();
    private final ObservableList<MacroTableRow> rows = FXCollections.observableArrayList();
    private final TextArea dslArea = new TextArea();
    private final Label saveStateLabel = new Label();
    private final Label macroSummaryLabel = new Label();
    private final Label statusLabel = new Label("Готово");
    private final Label fileLabel = new Label("Файл: не выбран");

    private final MacroStorage storage = new MacroStorage();
    private final MacroPlayer player = new MacroPlayer();
    private final RecorderService recorder = new RecorderService();
    private final SettingsWindow settingsWindow;
    private final InspectorWindow inspectorWindow;
    private final HotkeyService hotkeys = new HotkeyService(
            player,
            HotkeyService.DEFAULT_PAUSE_RESUME_KEY,
            HotkeyService.DEFAULT_STOP_KEY,
            this::emergencyStop
    );
    private final MacroDslCodec dslCodec = new MacroDslCodec();

    private Button btnRecordRef;
    private Button btnStopRecordRef;
    private Button btnPlayRef;
    private Button btnPauseRef;
    private Button btnStopRef;

    private Macro currentMacro = new Macro("Новый макрос");
    private Path currentFilePath;
    private boolean dirty;

    public MainController(Stage stage) {
        this.stage = stage;
        this.settingsWindow = new SettingsWindow(stage);
        this.inspectorWindow = new InspectorWindow(stage);
        recorder.setIgnoredKeyCodes(resolveIgnoredKeys());
        player.setStateListener(state -> Platform.runLater(() -> {
            syncHotkeyRegistration();
            updateControls();
        }));

        root.getStyleClass().add("app-root");
        root.setPadding(new Insets(14));
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
            ex.printStackTrace();
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
        VBox box = new VBox(12, buildHeaderBar(), buildMenuBar(), buildActionPanel());
        box.setPadding(new Insets(0, 0, 12, 0));
        return box;
    }

    private Parent buildTabs() {
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("app-tabs");
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

        Menu toolsMenu = new Menu("Инструменты");
        MenuItem itemInspector = new MenuItem("Инспектор координат и цвета");
        toolsMenu.getItems().add(itemInspector);
        itemInspector.setOnAction(e -> openInspector());

        MenuBar menuBar = new MenuBar(fileMenu, macroMenu, toolsMenu);
        menuBar.getStyleClass().add("app-menu-bar");
        return menuBar;
    }

    private Parent buildHeaderBar() {
        Label title = new Label("MacRosNik");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("Запись, проверка и запуск макросов в одном окне.");
        subtitle.getStyleClass().add("app-subtitle");

        VBox titleBox = new VBox(2, title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button settingsButton = createButton("Настройки", "utility-button");
        settingsButton.setOnAction(e -> openSettings());
        Button inspectorButton = createButton("Инспектор", "utility-button");
        inspectorButton.setOnAction(e -> openInspector());

        HBox headerBar = new HBox(16, titleBox, spacer, inspectorButton, settingsButton);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        return headerBar;
    }

    private Parent buildActionPanel() {
        fileLabel.setMaxWidth(Double.MAX_VALUE);
        fileLabel.setWrapText(true);
        fileLabel.getStyleClass().add("meta-value");
        saveStateLabel.getStyleClass().add("meta-caption");

        VBox controlsCard = new VBox(
                12,
                createSectionHeader(
                        "Управление макросом",
                        "Основные действия собраны здесь, сценарий редактируется на соседней вкладке."
                ),
                buildToolbar()
        );
        controlsCard.getStyleClass().add("panel-card");
        controlsCard.setPrefWidth(720);
        controlsCard.setMinWidth(540);

        VBox fileCard = new VBox(
                8,
                createSectionHeader("Текущий файл", "Путь и состояние сохранения."),
                saveStateLabel,
                fileLabel
        );
        fileCard.getStyleClass().add("panel-card");
        fileCard.setPrefWidth(330);
        fileCard.setMinWidth(280);

        FlowPane panel = new FlowPane(12, 12, controlsCard, fileCard);
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setPrefWrapLength(1040);
        return panel;
    }

    private Parent buildToolbar() {
        Button btnRecord = createButton("Начать запись", "primary-button");
        Button btnStopRecord = createButton("Остановить запись", "secondary-button");
        Button btnPlay = createButton("Запустить", "primary-button");
        Button btnPause = createButton("Пауза", "secondary-button");
        Button btnStop = createButton("Стоп", "secondary-button");

        btnRecord.setOnAction(e -> startRecording());
        btnStopRecord.setOnAction(e -> stopRecording());
        btnPlay.setOnAction(e -> playMacro());
        btnPause.setOnAction(e -> togglePause());
        btnStop.setOnAction(e -> stopPlayback());

        btnRecordRef = btnRecord;
        btnStopRecordRef = btnStopRecord;
        btnPlayRef = btnPlay;
        btnPauseRef = btnPause;
        btnStopRef = btnStop;

        FlowPane toolbar = new FlowPane(8, 8, btnRecord, btnStopRecord, btnPlay, btnPause, btnStop);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPrefWrapLength(760);
        return toolbar;
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
        table.setPlaceholder(createPlaceholderLabel("После записи или применения сценария здесь появятся шаги макроса."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        colType.setPrefWidth(220);
        colDelay.setPrefWidth(180);
        colDetails.setPrefWidth(620);

        macroSummaryLabel.getStyleClass().add("section-note");

        VBox box = new VBox(
                8,
                createSectionHeader(
                        "Шаги макроса",
                        "Список обновляется после записи, открытия файла и применения сценария."
                ),
                macroSummaryLabel,
                table
        );
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private Parent buildDslPanel() {
        dslArea.setWrapText(false);

        Button validateButton = createButton("Проверить сценарий", "secondary-button");
        Button applyButton = createButton("Применить к макросу", "primary-button");
        Button exportButton = createButton("Заполнить из макроса", "secondary-button");
        applyButton.setDefaultButton(true);

        validateButton.setOnAction(e -> validateDsl());
        applyButton.setOnAction(e -> applyDslToMacro());
        exportButton.setOnAction(e -> fillDslFromMacro());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(8, validateButton, exportButton, spacer, applyButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        StackPane editorPane = new StackPane(dslArea, createScenarioPlaceholder());
        VBox.setVgrow(editorPane, Priority.ALWAYS);

        VBox box = new VBox(8,
                createSectionHeader(
                        "Редактор сценария",
                        "Проверка не меняет макрос, а применение сразу обновляет шаги."
                ),
                actions,
                editorPane
        );
        VBox.setVgrow(editorPane, Priority.ALWAYS);
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
        placeholder.getStyleClass().add("scenario-placeholder");
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
                + "Пример сценария:\n"
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

        VBox box = new VBox(
                8,
                createSectionHeader(
                        "Как начать",
                        "Короткая памятка по записи, редактированию сценария и горячим клавишам."
                ),
                help
        );
        VBox.setVgrow(help, Priority.ALWAYS);
        return box;
    }

    private Parent buildBottomBar() {
        statusLabel.getStyleClass().add("status-message");

        Label statusTitle = new Label("Состояние");
        statusTitle.getStyleClass().add("status-caption");

        VBox statusCard = new VBox(2, statusTitle, statusLabel);
        statusCard.getStyleClass().add("panel-card");
        HBox.setHgrow(statusCard, Priority.ALWAYS);

        Label hint = new Label("F8 — пауза/продолжить, F12 — экстренный стоп");
        hint.getStyleClass().add("hotkey-chip");

        FlowPane box = new FlowPane(12, 12, statusCard, hint);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWrapLength(1040);
        BorderPane.setMargin(box, new Insets(12, 0, 0, 0));
        return box;
    }

    private VBox createSectionHeader(String titleText, String subtitleText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");

        if (subtitleText == null || subtitleText.isBlank()) {
            return new VBox(2, title);
        }

        Label subtitle = new Label(subtitleText);
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("section-subtitle");
        return new VBox(2, title, subtitle);
    }

    private Button createButton(String text, String... styleClasses) {
        Button button = new Button(text);
        button.setMinHeight(38);
        button.getStyleClass().addAll(styleClasses);
        return button;
    }

    private Label createPlaceholderLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setPadding(new Insets(12));
        label.getStyleClass().add("section-subtitle");
        return label;
    }

    private void startRecording() {
        try {
            recorder.start();
            syncHotkeyRegistration();
            updateControls();
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
            syncHotkeyRegistration();
            updateControls();
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
        if (recorder.isRecording()) {
            status("Нельзя запустить макрос во время записи");
            return;
        }
        player.play(currentMacro);
        syncHotkeyRegistration();
        updateControls();
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
        syncHotkeyRegistration();
        updateControls();
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
        updateSaveStateLabel();
    }

    private void refreshTable() {
        rows.clear();
        if (currentMacro.actions == null) {
            updateMacroSummary();
            return;
        }
        for (int i = 0; i < currentMacro.actions.size(); i++) {
            rows.add(MacroTableRow.from(i, currentMacro.actions.get(i)));
        }
        updateMacroSummary();
    }

    private void updateTitle() {
        String name = deriveMacroName();
        String suffix = dirty ? " *" : "";
        stage.setTitle("MacRosNik — " + name + suffix);
    }

    private void updateFileLabel() {
        fileLabel.setText(currentFilePath == null
                ? "Файл пока не выбран. Можно начать с записи или открыть существующий макрос."
                : currentFilePath.toString());
    }

    private void updateSaveStateLabel() {
        if (currentFilePath == null) {
            saveStateLabel.setText(dirty
                    ? "Есть изменения, файл еще не сохранен"
                    : "Новый макрос, файл еще не сохранен");
            return;
        }
        saveStateLabel.setText(dirty ? "Есть несохраненные изменения" : "Изменения сохранены");
    }

    private void updateMacroSummary() {
        if (rows.isEmpty()) {
            macroSummaryLabel.setText("Пока нет действий. Запишите макрос или примените сценарий.");
            return;
        }
        macroSummaryLabel.setText("Шагов в макросе: " + rows.size());
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
        return Set.of(HotkeyService.DEFAULT_PAUSE_RESUME_KEY, HotkeyService.DEFAULT_STOP_KEY);
    }

    private void syncHotkeyRegistration() {
        boolean shouldRegister = recorder.isRecording()
                || player.getState() == PlayerState.PLAYING
                || player.getState() == PlayerState.PAUSED;

        try {
            if (shouldRegister) {
                hotkeys.start();
            } else {
                hotkeys.close();
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            status("Горячие клавиши не запущены: " + readableMessage(ex));
        }
    }

    private void updateControls() {
        if (btnRecordRef == null || btnStopRecordRef == null || btnPlayRef == null || btnPauseRef == null || btnStopRef == null) {
            return;
        }
        PlayerState state = player.getState();
        boolean playing = state == PlayerState.PLAYING;
        boolean paused = state == PlayerState.PAUSED;
        boolean recording = recorder.isRecording();

        btnPlayRef.setDisable(playing || paused || recording);
        btnPauseRef.setDisable(!playing && !paused);
        btnStopRef.setDisable(!playing && !paused);
        btnRecordRef.setDisable(recording || playing || paused);
        btnStopRecordRef.setDisable(!recording);
        btnPauseRef.setText(paused ? "Продолжить" : "Пауза");
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
        UiStyles.apply(alert.getDialogPane());
        alert.showAndWait();
        ex.printStackTrace();
        status(title);
    }

    public void emergencyStop() {
        try {
            player.stop();
            updateControls();
        } catch (Exception ignored) {
        }

        try {
            if (recorder.isRecording()) {
                recorder.stop();
                updateControls();
            }
        } catch (Exception ignored) {
        }
        status("Выполнена экстренная остановка");
    }
}
