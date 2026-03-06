package macrosnik.dsl;

import macrosnik.domain.Action;
import macrosnik.domain.DelayAction;
import macrosnik.domain.KeyAction;
import macrosnik.domain.Macro;
import macrosnik.domain.MouseButtonAction;
import macrosnik.domain.MouseMovePathAction;
import macrosnik.domain.enums.KeyActionType;
import macrosnik.domain.enums.MouseButton;
import macrosnik.domain.enums.MouseButtonActionType;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class MacroDslCodec {
    private final DslParser parser = new DslParser();

    public Macro fromDsl(String text, String macroName) {
        return parser.parse(text, macroName);
    }

    public String toDsl(Macro macro) {
        List<String> lines = new ArrayList<>();
        if (macro == null || macro.actions == null) {
            return "";
        }

        for (Action action : macro.actions) {
            if (action.delayBeforeMs > 0) {
                lines.add("ждать " + action.delayBeforeMs);
            }

            if (action instanceof DelayAction delayAction) {
                lines.add("ждать " + delayAction.durationMs);
            } else if (action instanceof MouseMovePathAction mouseMovePathAction) {
                for (var point : mouseMovePathAction.points) {
                    lines.add("мышь двигать " + point.x + " " + point.y);
                    if (point.dtMs > 0) {
                        lines.add("ждать " + point.dtMs);
                    }
                }
            } else if (action instanceof MouseButtonAction mouseButtonAction) {
                if (mouseButtonAction.action == MouseButtonActionType.CLICK) {
                    lines.add("клик " + mouseButtonName(mouseButtonAction.button));
                } else {
                    lines.add("мышь " + mouseButtonName(mouseButtonAction.button) + " " + mouseActionName(mouseButtonAction.action));
                }
            } else if (action instanceof KeyAction keyAction) {
                lines.add("клавиша " + KeyEvent.getKeyText(keyAction.keyCode).toUpperCase() + " " + keyActionName(keyAction.action));
            }
        }
        return String.join("\n", lines);
    }

    private String mouseButtonName(MouseButton button) {
        return switch (button) {
            case LEFT -> "лкм";
            case RIGHT -> "пкм";
            case MIDDLE -> "скм";
        };
    }

    private String mouseActionName(MouseButtonActionType actionType) {
        return switch (actionType) {
            case DOWN -> "вниз";
            case UP -> "вверх";
            case CLICK -> "клик";
        };
    }

    private String keyActionName(KeyActionType actionType) {
        return actionType == KeyActionType.DOWN ? "вниз" : "вверх";
    }
}
