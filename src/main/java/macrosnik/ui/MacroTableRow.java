package macrosnik.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import macrosnik.domain.Action;
import macrosnik.domain.CoordinateMode;
import macrosnik.domain.DelayAction;
import macrosnik.domain.KeyAction;
import macrosnik.domain.MouseButtonAction;
import macrosnik.domain.MouseMovePathAction;
import macrosnik.domain.MouseMovePathAction.PathPoint;
import macrosnik.domain.TextInputAction;
import macrosnik.domain.enums.KeyActionType;
import macrosnik.domain.enums.MouseButton;
import macrosnik.domain.enums.MouseButtonActionType;

import java.awt.event.KeyEvent;
import java.util.List;

public class MacroTableRow {
    private static final int TEXT_PREVIEW_LIMIT = 40;

    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty delay = new SimpleStringProperty();
    private final StringProperty details = new SimpleStringProperty();

    private MacroTableRow(String type, String delay, String details) {
        this.type.set(type);
        this.delay.set(delay);
        this.details.set(details);
    }

    public static MacroTableRow from(int index, Action action) {
        ActionSummary summary = summarize(action);
        return new MacroTableRow((index + 1) + ". " + summary.type(), action.delayBeforeMs + " мс", summary.details());
    }

    private static ActionSummary summarize(Action action) {
        if (action instanceof DelayAction delayAction) {
            return new ActionSummary("Пауза", "Ожидание " + delayAction.durationMs + " мс");
        }
        if (action instanceof MouseMovePathAction pathAction) {
            return new ActionSummary("Перемещение мыши", describePath(pathAction));
        }
        if (action instanceof MouseButtonAction mouseButtonAction) {
            return new ActionSummary(
                    "Кнопка мыши",
                    describeMouseButton(mouseButtonAction)
            );
        }
        if (action instanceof KeyAction keyAction) {
            return new ActionSummary(
                    "Клавиатура",
                    "Клавиша: " + keyName(keyAction.keyCode) + ", действие: " + keyActionName(keyAction.action)
            );
        }
        if (action instanceof TextInputAction textInputAction) {
            return new ActionSummary("Ввод текста", "Текст: " + previewText(textInputAction.text));
        }
        return new ActionSummary(action.actionType().name(), action.getClass().getSimpleName());
    }

    private static String describePath(MouseMovePathAction action) {
        List<PathPoint> points = action.points;
        String mode = modeName(action.coordinateMode);
        if (points.size() == 1) {
            return "Точка: " + formatPoint(points.getFirst()) + ", режим: " + mode;
        }

        String firstPoint = points.isEmpty() ? "-" : formatPoint(points.getFirst());
        String lastPoint = points.isEmpty() ? "-" : formatPoint(points.getLast());
        return "Точек: " + points.size()
                + ", от " + firstPoint
                + " до " + lastPoint
                + ", время: " + totalPathDuration(points) + " мс"
                + ", режим: " + mode;
    }

    private static String keyActionName(KeyActionType actionType) {
        return switch (actionType) {
            case DOWN -> "нажатие";
            case UP -> "отпускание";
            case CLICK -> "клик";
        };
    }

    private static String mouseButtonName(MouseButton button) {
        return switch (button) {
            case LEFT -> "Левая кнопка";
            case RIGHT -> "Правая кнопка";
            case MIDDLE -> "Средняя кнопка";
        };
    }

    private static String mouseActionName(MouseButtonActionType actionType) {
        return switch (actionType) {
            case DOWN -> "нажатие";
            case UP -> "отпускание";
            case CLICK -> "клик";
        };
    }

    private static String describeMouseButton(MouseButtonAction action) {
        String details = mouseButtonName(action.button) + ", действие: " + mouseActionName(action.action);
        if (action.hasCoordinates()) {
            details += ", точка: (" + action.x + ", " + action.y + ")";
        }
        return details;
    }

    private static String modeName(CoordinateMode mode) {
        return switch (mode) {
            case SCREEN_ABSOLUTE -> "экранные координаты";
            case WINDOW_RELATIVE -> "относительно окна";
            case WINDOW_ABSOLUTE -> "абсолютно в окне";
        };
    }

    private static String formatPoint(PathPoint point) {
        return "(" + point.x + ", " + point.y + ")";
    }

    private static String keyName(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_ENTER -> "Ввод";
            case KeyEvent.VK_SPACE -> "Пробел";
            case KeyEvent.VK_UP -> "Вверх";
            case KeyEvent.VK_DOWN -> "Вниз";
            case KeyEvent.VK_LEFT -> "Влево";
            case KeyEvent.VK_RIGHT -> "Вправо";
            default -> KeyEvent.getKeyText(keyCode);
        };
    }

    private static String previewText(String text) {
        if (text == null || text.isEmpty()) {
            return "\"\"";
        }
        String normalized = text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\n", "\\n");
        if (normalized.length() > TEXT_PREVIEW_LIMIT) {
            normalized = normalized.substring(0, TEXT_PREVIEW_LIMIT - 3) + "...";
        }
        return '"' + normalized + '"';
    }

    private static long totalPathDuration(List<PathPoint> points) {
        long total = 0;
        for (PathPoint point : points) {
            total += point.dtMs;
        }
        return total;
    }

    public StringProperty typeProperty() {
        return type;
    }

    public StringProperty delayProperty() {
        return delay;
    }

    public StringProperty detailsProperty() {
        return details;
    }

    private record ActionSummary(String type, String details) {
    }
}
