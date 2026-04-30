package macrosnik.ui;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.function.Function;
import java.util.function.Supplier;

import static javafx.util.Duration.millis;

public class InspectorWindow {
    private static final String APP_ICON_RESOURCE = "/icons/app.png";
    private static final String WINDOW_TITLE = "MacRosNik - Инспектор";
    private static final String DEFAULT_STATUS = "F6 - захват, Esc - закрыть, клик по полю - копирование.";
    private static final String HOTKEYS_UNAVAILABLE_STATUS = "Глобальные клавиши недоступны. Используйте окно активным.";
    private static final String SNAPSHOT_FAILED_STATUS = "Не удалось получить снимок точки.";
    private static final String SNAPSHOT_CAPTURED_STATUS = "Точка зафиксирована.";
    private static final String EMPTY_VALUE = "-";
    private static final double COORDINATE_BUTTON_WIDTH = 92;
    private static final double HEX_BUTTON_WIDTH = 102;
    private static final int WINDOW_WIDTH = 320;
    private static final int WINDOW_HEIGHT = 130;
    private static final int REFRESH_INTERVAL_MS = 80;
    private static final int CAPTURE_KEY_CODE = NativeKeyEvent.VC_F6;
    private static final int CLOSE_KEY_CODE = NativeKeyEvent.VC_ESCAPE;

    private final Stage owner;
    private final Robot robot;
    private final Timeline refreshTimeline;
    private final CaptureHotkeyListener captureHotkeyListener = new CaptureHotkeyListener();
    private ScreenProbeSnapshot currentSnapshot;
    private ScreenProbeSnapshot frozenSnapshot;
    private final SnapshotSection liveSection = new SnapshotSection(() -> currentSnapshot);
    private final SnapshotSection frozenSection = new SnapshotSection(() -> frozenSnapshot);
    private final Label statusLabel = new Label(DEFAULT_STATUS);

    private Stage stage;
    private boolean captureHotkeysActive;

    public InspectorWindow(Stage owner) {
        this.owner = owner;
        this.robot = new Robot();
        this.refreshTimeline = new Timeline(new KeyFrame(millis(REFRESH_INTERVAL_MS), event -> refreshLiveSnapshot()));
        this.refreshTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    public void show() {
        if (stage == null) {
            stage = createStage();
        }
        if (!stage.isShowing()) {
            stage.show();
        }
        activateWindow();
        stage.toFront();
        stage.requestFocus();
    }

    public void close() {
        deactivateWindow();
        if (stage != null) {
            stage.close();
        }
    }

    private Stage createStage() {
        liveSection.update(null);
        frozenSection.update(null);

        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        liveSection.addTo(grid, 0, "Текущие");
        frozenSection.addTo(grid, 1, "Фикс");

        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: #555;");

        VBox root = new VBox(8, grid, statusLabel);
        root.setPadding(new Insets(10));

        Stage inspectorStage = new Stage();
        inspectorStage.initOwner(owner);
        inspectorStage.setTitle(WINDOW_TITLE);
        inspectorStage.setScene(new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT));
        inspectorStage.setMinWidth(WINDOW_WIDTH);
        inspectorStage.setMinHeight(WINDOW_HEIGHT);
        inspectorStage.setMaxWidth(WINDOW_WIDTH);
        inspectorStage.setMaxHeight(WINDOW_HEIGHT);
        inspectorStage.setResizable(false);
        inspectorStage.setAlwaysOnTop(true);
        inspectorStage.setOnHidden(event -> deactivateWindow());
        applyWindowIcon(inspectorStage);
        return inspectorStage;
    }

    private void activateWindow() {
        startRefresh();
        startCaptureHotkeys();
    }

    private void deactivateWindow() {
        stopRefresh();
        stopCaptureHotkeys();
    }

    private boolean isWindowShowing() {
        return stage != null && stage.isShowing();
    }

    private void startRefresh() {
        if (isWindowShowing()) {
            refreshTimeline.play();
        }
    }

    private void stopRefresh() {
        refreshTimeline.stop();
    }

    private void refreshLiveSnapshot() {
        if (!isWindowShowing()) {
            return;
        }
        currentSnapshot = readSnapshot();
        liveSection.update(currentSnapshot);
    }

    private void captureSnapshot() {
        applyCapturedSnapshot(currentSnapshot != null ? currentSnapshot : readSnapshot());
    }

    private void applyCapturedSnapshot(ScreenProbeSnapshot snapshot) {
        if (!isWindowShowing()) {
            return;
        }
        if (snapshot == null) {
            setStatus(SNAPSHOT_FAILED_STATUS);
            return;
        }

        frozenSnapshot = snapshot;
        frozenSection.update(frozenSnapshot);
        setStatus(SNAPSHOT_CAPTURED_STATUS);
    }

    private ScreenProbeSnapshot readSnapshot() {
        Point2D mouse = robot.getMousePosition();
        if (mouse == null) {
            return null;
        }

        int x = (int) Math.floor(mouse.getX());
        int y = (int) Math.floor(mouse.getY());
        WritableImage capture = robot.getScreenCapture(null, x, y, 1, 1);
        PixelReader reader = capture.getPixelReader();
        if (reader == null) {
            return null;
        }
        return ScreenProbeSnapshot.fromFx(x, y, reader.getColor(0, 0));
    }

    private void startCaptureHotkeys() {
        if (captureHotkeysActive || !isWindowShowing()) {
            return;
        }

        try {
            if (!GlobalScreen.isNativeHookRegistered()) {
                GlobalScreen.registerNativeHook();
            }
            GlobalScreen.removeNativeKeyListener(captureHotkeyListener);
            GlobalScreen.addNativeKeyListener(captureHotkeyListener);
            captureHotkeysActive = true;
            setStatus(DEFAULT_STATUS);
        } catch (NativeHookException ex) {
            captureHotkeysActive = false;
            setStatus(HOTKEYS_UNAVAILABLE_STATUS);
        }
    }

    private void stopCaptureHotkeys() {
        captureHotkeyListener.resetState();
        if (!captureHotkeysActive) {
            return;
        }
        try {
            GlobalScreen.removeNativeKeyListener(captureHotkeyListener);
        } catch (Throwable ignored) {
        } finally {
            captureHotkeysActive = false;
        }
    }

    private void copySnapshotValue(ScreenProbeSnapshot snapshot, Function<ScreenProbeSnapshot, String> valueExtractor) {
        if (snapshot == null) {
            return;
        }
        copyText(valueExtractor.apply(snapshot));
    }

    private void copyText(String value) {
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard.getSystemClipboard().setContent(content);
        setStatus("Скопировано в буфер: " + value);
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private Label rowLabel(String text) {
        Label label = new Label(text);
        label.setMinWidth(52);
        return label;
    }

    private Button createValueButton(double width) {
        Button button = new Button(EMPTY_VALUE);
        button.setFocusTraversable(false);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPrefWidth(width);
        button.setMinWidth(width);
        button.setMaxWidth(width);
        return button;
    }

    private Rectangle createColorPreview() {
        Rectangle preview = new Rectangle(18, 18);
        preview.setArcWidth(4);
        preview.setArcHeight(4);
        preview.setStroke(Color.web("#444"));
        preview.setFill(Color.TRANSPARENT);
        return preview;
    }

    private StackPane wrapPreview(Rectangle preview) {
        StackPane wrapper = new StackPane(preview);
        wrapper.setPadding(new Insets(2, 0, 2, 0));
        return wrapper;
    }

    private void applyWindowIcon(Stage stage) {
        try (InputStream inputStream = InspectorWindow.class.getResourceAsStream(APP_ICON_RESOURCE)) {
            if (inputStream != null) {
                stage.getIcons().add(new Image(inputStream));
            }
        } catch (Exception ignored) {
        }
    }

    private final class SnapshotSection {
        private final Supplier<ScreenProbeSnapshot> snapshotSupplier;
        private final Button coordinatesButton = createValueButton(COORDINATE_BUTTON_WIDTH);
        private final Button hexButton = createValueButton(HEX_BUTTON_WIDTH);
        private final Rectangle colorPreview = createColorPreview();

        private SnapshotSection(Supplier<ScreenProbeSnapshot> snapshotSupplier) {
            this.snapshotSupplier = snapshotSupplier;
            coordinatesButton.setOnAction(event -> copySnapshotValue(this.snapshotSupplier.get(), ScreenProbeSnapshot::coordinatesText));
            hexButton.setOnAction(event -> copySnapshotValue(this.snapshotSupplier.get(), ScreenProbeSnapshot::hexColor));
        }

        private void addTo(GridPane grid, int rowIndex, String labelText) {
            grid.add(rowLabel(labelText), 0, rowIndex);
            grid.add(coordinatesButton, 1, rowIndex);
            grid.add(hexButton, 2, rowIndex);
            grid.add(wrapPreview(colorPreview), 3, rowIndex);
        }

        private void update(ScreenProbeSnapshot snapshot) {
            boolean hasSnapshot = snapshot != null;
            coordinatesButton.setText(hasSnapshot ? snapshot.positionText() : EMPTY_VALUE);
            hexButton.setText(hasSnapshot ? snapshot.hexColor() : EMPTY_VALUE);
            colorPreview.setFill(hasSnapshot ? snapshot.toFxColor() : Color.TRANSPARENT);
            coordinatesButton.setDisable(!hasSnapshot);
            hexButton.setDisable(!hasSnapshot);
        }
    }

    private final class CaptureHotkeyListener implements NativeKeyListener {
        private boolean captureKeyDown;
        private boolean closeKeyDown;

        @Override
        public void nativeKeyPressed(NativeKeyEvent event) {
            if (!isWindowShowing()) {
                return;
            }

            if (event.getKeyCode() == CAPTURE_KEY_CODE && !captureKeyDown) {
                captureKeyDown = true;
                Platform.runLater(InspectorWindow.this::captureSnapshot);
                return;
            }

            if (event.getKeyCode() == CLOSE_KEY_CODE && !closeKeyDown) {
                closeKeyDown = true;
                Platform.runLater(() -> {
                    if (stage != null) {
                        stage.hide();
                    }
                });
            }
        }

        @Override
        public void nativeKeyReleased(NativeKeyEvent event) {
            if (event.getKeyCode() == CAPTURE_KEY_CODE) {
                captureKeyDown = false;
            }
            if (event.getKeyCode() == CLOSE_KEY_CODE) {
                closeKeyDown = false;
            }
        }

        @Override
        public void nativeKeyTyped(NativeKeyEvent event) {
        }

        private void resetState() {
            captureKeyDown = false;
            closeKeyDown = false;
        }
    }
}
