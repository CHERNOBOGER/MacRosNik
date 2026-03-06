package macrosnik.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import macrosnik.domain.*;
import macrosnik.domain.MouseMovePathAction.PathPoint;
import macrosnik.domain.enums.KeyActionType;
import macrosnik.domain.enums.MouseButton;
import macrosnik.domain.enums.MouseButtonActionType;

import java.awt.event.KeyEvent;

public class MacroTableRow {
    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty delay = new SimpleStringProperty();
    private final StringProperty details = new SimpleStringProperty();

    public static MacroTableRow from(int index, Action action) {
        MacroTableRow row = new MacroTableRow();
        row.type.set(index + ". " + typeName(action));
        row.delay.set(action.delayBeforeMs + " мс");

        if (action instanceof DelayAction delayAction) {
            row.details.set("Ожидание " + delayAction.durationMs + " мс");
        } else if (action instanceof MouseMovePathAction mouseMovePathAction) {
            int n = mouseMovePathAction.points.size();
            String first = n > 0 ? fmt(mouseMovePathAction.points.get(0)) : "-";
            String last = n > 0 ? fmt(mouseMovePathAction.points.get(n - 1)) : "-";
            row.details.set("Точек: " + n + ", от " + first + " до " + last + ", режим: " + modeName(mouseMovePathAction.coordinateMode));
        } else if (action instanceof MouseButtonAction mouseButtonAction) {
            row.details.set(mouseButtonName(mouseButtonAction.button) + ", действие: " + mouseActionName(mouseButtonAction.action));
        } else if (action instanceof KeyAction keyAction) {
            row.details.set("Клавиша: " + KeyEvent.getKeyText(keyAction.keyCode) + ", действие: " + keyActionName(keyAction.action));
        } else {
            row.details.set(action.getClass().getSimpleName());
        }
        return row;
    }

    private static String typeName(Action action) {
        if (action instanceof DelayAction) return "Пауза";
        if (action instanceof MouseMovePathAction) return "Перемещение мыши";
        if (action instanceof MouseButtonAction) return "Кнопка мыши";
        if (action instanceof KeyAction) return "Клавиатура";
        return action.actionType().name();
    }

    private static String keyActionName(KeyActionType actionType) {
        return actionType == KeyActionType.DOWN ? "нажатие" : "отпускание";
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

    private static String modeName(CoordinateMode mode) {
        return switch (mode) {
            case SCREEN_ABSOLUTE -> "экранные координаты";
            case WINDOW_RELATIVE -> "относительно окна";
            case WINDOW_ABSOLUTE -> "абсолютно в окне";
        };
    }

    private static String fmt(PathPoint point) {
        return "(" + point.x + ", " + point.y + ")";
    }

    public StringProperty typeProperty() { return type; }
    public StringProperty delayProperty() { return delay; }
    public StringProperty detailsProperty() { return details; }
}
