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

import static javafx.util.Duration.millis;

public class InspectorWindow {
    private static final String APP_ICON_RESOURCE = "/icons/app.png";
    private static final int REFRESH_INTERVAL_MS = 80;
    private static final int CAPTURE_KEY_CODE = NativeKeyEvent.VC_F6;
    private static final int CLOSE_KEY_CODE = NativeKeyEvent.VC_ESCAPE;

    private final Stage owner;
    private final Robot robot;
    private final Timeline refreshTimeline;
    private final CaptureHotkeyListener captureHotkeyListener = new CaptureHotkeyListener();

    private Stage stage;
    private boolean captureHotkeysActive;

    private final Button liveCoordinatesButton = createValueButton(92);
    private final Button liveHexButton = createValueButton(102);
    private final Rectangle liveColorPreview = createColorPreview();

    private final Button frozenCoordinatesButton = createValueButton(92);
    private final Button frozenHexButton = createValueButton(102);
    private final Rectangle frozenColorPreview = createColorPreview();

    private final Label statusLabel = new Label("F6 - захват, Esc - закрыть, клик по полю - копирование.");

    private ScreenProbeSnapshot currentSnapshot;
    private ScreenProbeSnapshot frozenSnapshot;

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
        startRefresh();
        startCaptureHotkeys();
        stage.toFront();
        stage.requestFocus();
    }

    public void close() {
        stopRefresh();
        stopCaptureHotkeys();
        if (stage != null) {
            stage.close();
        }
    }

    private Stage createStage() {
        configureCopyButtons();
        updateLiveSection(null);
        updateFrozenSection();

        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);

        grid.add(rowLabel("Текущие"), 0, 0);
        grid.add(liveCoordinatesButton, 1, 0);
        grid.add(liveHexButton, 2, 0);
        grid.add(wrapPreview(liveColorPreview), 3, 0);

        grid.add(rowLabel("Фикс"), 0, 1);
        grid.add(frozenCoordinatesButton, 1, 1);
        grid.add(frozenHexButton, 2, 1);
        grid.add(wrapPreview(frozenColorPreview), 3, 1);

        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: #555;");

        VBox root = new VBox(8, grid, statusLabel);
        root.setPadding(new Insets(10));

        Stage inspectorStage = new Stage();
        inspectorStage.initOwner(owner);
        inspectorStage.setTitle("MacRosNik - Инспектор");
        inspectorStage.setScene(new Scene(root, 320, 130));
        inspectorStage.setMinWidth(320);
        inspectorStage.setMinHeight(130);
        inspectorStage.setMaxWidth(320);
        inspectorStage.setMaxHeight(130);
        inspectorStage.setResizable(false);
        inspectorStage.setAlwaysOnTop(true);
        inspectorStage.setOnShown(event -> {
            startRefresh();
            startCaptureHotkeys();
        });
        inspectorStage.setOnHidden(event -> {
            stopRefresh();
            stopCaptureHotkeys();
        });
        applyWindowIcon(inspectorStage);
        return inspectorStage;
    }

    private void configureCopyButtons() {
        liveCoordinatesButton.setOnAction(event -> copySnapshotCoordinates(currentSnapshot));
        liveHexButton.setOnAction(event -> copySnapshotColor(currentSnapshot));
        frozenCoordinatesButton.setOnAction(event -> copySnapshotCoordinates(frozenSnapshot));
        frozenHexButton.setOnAction(event -> copySnapshotColor(frozenSnapshot));
    }

    private void startRefresh() {
        if (stage != null && stage.isShowing()) {
            refreshTimeline.play();
        }
    }

    private void stopRefresh() {
        refreshTimeline.stop();
    }

    private void refreshLiveSnapshot() {
        if (stage == null || !stage.isShowing()) {
            return;
        }
        updateLiveSection(readSnapshot());
    }

    private void captureSnapshot() {
        applyCapturedSnapshot(currentSnapshot != null ? currentSnapshot : readSnapshot());
    }

    private void applyCapturedSnapshot(ScreenProbeSnapshot snapshot) {
        if (stage == null || !stage.isShowing()) {
            return;
        }
        if (snapshot == null) {
            statusLabel.setText("Не удалось получить снимок точки.");
            return;
        }

        frozenSnapshot = snapshot;
        updateFrozenSection();
        statusLabel.setText("Точка зафиксирована.");
    }

    private void updateLiveSection(ScreenProbeSnapshot snapshot) {
        currentSnapshot = snapshot;
        if (snapshot == null) {
            liveCoordinatesButton.setText("—");
            liveHexButton.setText("—");
            liveColorPreview.setFill(Color.TRANSPARENT);
            liveCoordinatesButton.setDisable(true);
            liveHexButton.setDisable(true);
            return;
        }

        liveCoordinatesButton.setText(snapshot.positionText());
        liveHexButton.setText(snapshot.hexColor());
        liveColorPreview.setFill(toFxColor(snapshot));
        liveCoordinatesButton.setDisable(false);
        liveHexButton.setDisable(false);
    }

    private void updateFrozenSection() {
        boolean hasSnapshot = frozenSnapshot != null;

        frozenCoordinatesButton.setText(hasSnapshot ? frozenSnapshot.positionText() : "—");
        frozenHexButton.setText(hasSnapshot ? frozenSnapshot.hexColor() : "—");
        frozenColorPreview.setFill(hasSnapshot ? toFxColor(frozenSnapshot) : Color.TRANSPARENT);

        frozenCoordinatesButton.setDisable(!hasSnapshot);
        frozenHexButton.setDisable(!hasSnapshot);
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
        if (captureHotkeysActive || stage == null || !stage.isShowing()) {
            return;
        }

        try {
            if (!GlobalScreen.isNativeHookRegistered()) {
                GlobalScreen.registerNativeHook();
            }
            GlobalScreen.removeNativeKeyListener(captureHotkeyListener);
            GlobalScreen.addNativeKeyListener(captureHotkeyListener);
            captureHotkeysActive = true;
            statusLabel.setText("F6 - захват, клик по полю - копирование.");
        } catch (NativeHookException ex) {
            captureHotkeysActive = false;
            statusLabel.setText("Глобальные клавиши недоступны. Используйте окно активным.");
        }
    }

    private void stopCaptureHotkeys() {
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

    private void copySnapshotCoordinates(ScreenProbeSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        copyText(snapshot.coordinatesText());
    }

    private void copySnapshotColor(ScreenProbeSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        copyText(snapshot.hexColor());
    }

    private void copyText(String value) {
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard.getSystemClipboard().setContent(content);
        statusLabel.setText("Сохранено в буфер: " + value);
    }

    private Label rowLabel(String text) {
        Label label = new Label(text);
        label.setMinWidth(52);
        return label;
    }

    private Button createValueButton(double width) {
        Button button = new Button("—");
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

    private Color toFxColor(ScreenProbeSnapshot snapshot) {
        return Color.rgb(snapshot.red(), snapshot.green(), snapshot.blue());
    }

    private void applyWindowIcon(Stage stage) {
        try (InputStream inputStream = InspectorWindow.class.getResourceAsStream(APP_ICON_RESOURCE)) {
            if (inputStream != null) {
                stage.getIcons().add(new Image(inputStream));
            }
        } catch (Exception ignored) {
        }
    }

    private final class CaptureHotkeyListener implements NativeKeyListener {
        private boolean captureKeyDown;
        private boolean closeKeyDown;

        @Override
        public void nativeKeyPressed(NativeKeyEvent event) {
            if (stage == null || !stage.isShowing()) {
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
    }
}
