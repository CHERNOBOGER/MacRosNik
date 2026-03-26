package macrosnik.dsl;

import macrosnik.domain.Action;
import macrosnik.domain.DelayAction;
import macrosnik.domain.KeyAction;
import macrosnik.domain.Macro;
import macrosnik.domain.MouseButtonAction;
import macrosnik.domain.MouseMovePathAction;
import macrosnik.domain.TextInputAction;
import macrosnik.domain.enums.KeyActionType;
import macrosnik.domain.enums.MouseButton;
import macrosnik.domain.enums.MouseButtonActionType;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MacroDslCodec {
    private final DslParser parser = new DslParser();

    public Macro fromDsl(String text, String macroName) {
        return parser.parse(text, macroName);
    }

    public String toDsl(Macro macro) {
        if (macro == null || macro.actions == null) {
            return "";
        }

        List<String> lines = new ArrayList<>();
        List<Action> actions = macro.actions;
        for (int i = 0; i < actions.size(); i++) {
            Action action = actions.get(i);
            if (action.delayBeforeMs > 0) {
                lines.add(formatWait(action.delayBeforeMs));
            }

            if (action instanceof DelayAction delayAction) {
                lines.add(formatWait(delayAction.durationMs));
                continue;
            }

            if (action instanceof MouseMovePathAction mouseMovePathAction) {
                if (canInlineMouseCommand(actions, i)) {
                    MouseButtonAction mouseButtonAction = (MouseButtonAction) actions.get(i + 1);
                    MouseMovePathAction.PathPoint point = mouseMovePathAction.points.getFirst();
                    lines.add(formatMouseCommand(mouseButtonAction.action, mouseButtonAction.button, point.x, point.y));
                    i++;
                } else if (canSummarizeMousePath(mouseMovePathAction)) {
                    lines.add(formatMousePath(mouseMovePathAction));
                } else {
                    appendMousePath(lines, mouseMovePathAction);
                }
                continue;
            }

            if (action instanceof MouseButtonAction mouseButtonAction) {
                lines.add(formatMouseCommand(mouseButtonAction.action, mouseButtonAction.button, null, null));
                continue;
            }

            if (action instanceof TextInputAction textInputAction) {
                lines.add("Ввести текст: " + quoteText(textInputAction.text));
                continue;
            }

            if (action instanceof KeyAction keyAction) {
                KeyComboMatch comboMatch = readKeyCombo(actions, i);
                if (comboMatch != null) {
                    lines.add("Нажать сочетание: " + String.join("+", comboMatch.keyNames()));
                    i += comboMatch.length() - 1;
                    continue;
                }

                if (canInlineKeyClick(actions, i)) {
                    lines.add("Нажать клавишу: " + formatKeyName(keyAction.keyCode));
                    i++;
                } else {
                    lines.add(formatKeyCommand(keyAction));
                }
            }
        }
        return String.join("\n", lines);
    }

    private boolean canInlineMouseCommand(List<Action> actions, int index) {
        if (!(actions.get(index) instanceof MouseMovePathAction mouseMovePathAction)) {
            return false;
        }
        if (index + 1 >= actions.size()) {
            return false;
        }
        if (!(actions.get(index + 1) instanceof MouseButtonAction mouseButtonAction)) {
            return false;
        }
        if (mouseButtonAction.delayBeforeMs > 0) {
            return false;
        }
        if (mouseMovePathAction.points.size() != 1) {
            return false;
        }
        return mouseMovePathAction.points.getFirst().dtMs == 0;
    }

    private boolean canInlineKeyClick(List<Action> actions, int index) {
        if (!(actions.get(index) instanceof KeyAction current)) {
            return false;
        }
        if (current.action != KeyActionType.DOWN || index + 1 >= actions.size()) {
            return false;
        }
        if (!(actions.get(index + 1) instanceof KeyAction next)) {
            return false;
        }
        return next.action == KeyActionType.UP
                && next.keyCode == current.keyCode
                && next.delayBeforeMs == 0;
    }

    private KeyComboMatch readKeyCombo(List<Action> actions, int startIndex) {
        if (!(actions.get(startIndex) instanceof KeyAction first) || first.action != KeyActionType.DOWN) {
            return null;
        }

        List<KeyAction> downs = new ArrayList<>();
        int cursor = startIndex;
        while (cursor < actions.size() && actions.get(cursor) instanceof KeyAction keyAction && keyAction.action == KeyActionType.DOWN) {
            if (cursor > startIndex && keyAction.delayBeforeMs > 0) {
                return null;
            }
            downs.add(keyAction);
            cursor++;
        }

        if (downs.size() < 2) {
            return null;
        }

        for (int i = downs.size() - 1; i >= 0; i--) {
            if (cursor >= actions.size() || !(actions.get(cursor) instanceof KeyAction keyAction)) {
                return null;
            }
            if (keyAction.delayBeforeMs > 0 || keyAction.action != KeyActionType.UP || keyAction.keyCode != downs.get(i).keyCode) {
                return null;
            }
            cursor++;
        }

        List<String> keyNames = new ArrayList<>();
        for (KeyAction down : downs) {
            keyNames.add(formatKeyName(down.keyCode));
        }
        return new KeyComboMatch(keyNames, downs.size() * 2);
    }

    private void appendMousePath(List<String> lines, MouseMovePathAction mouseMovePathAction) {
        for (MouseMovePathAction.PathPoint point : mouseMovePathAction.points) {
            lines.add("Переместить мышь: " + point.x + " " + point.y);
            if (point.dtMs > 0) {
                lines.add(formatWait(point.dtMs));
            }
        }
    }

    private boolean canSummarizeMousePath(MouseMovePathAction action) {
        if (action.points.size() < 2) {
            return false;
        }

        MouseMovePathAction.PathPoint first = action.points.getFirst();
        MouseMovePathAction.PathPoint last = action.points.getLast();
        double directDistance = Math.hypot(last.x - first.x, last.y - first.y);
        if (directDistance < 1.0) {
            return false;
        }

        double travelledDistance = 0;
        double maxDeviation = 0;
        for (int i = 1; i < action.points.size(); i++) {
            MouseMovePathAction.PathPoint previous = action.points.get(i - 1);
            MouseMovePathAction.PathPoint current = action.points.get(i);
            travelledDistance += Math.hypot(current.x - previous.x, current.y - previous.y);
            maxDeviation = Math.max(maxDeviation, distanceToSegment(first, last, current));
        }

        return travelledDistance / directDistance <= 1.25 && maxDeviation <= 12;
    }

    private String formatMousePath(MouseMovePathAction action) {
        MouseMovePathAction.PathPoint first = action.points.getFirst();
        MouseMovePathAction.PathPoint last = action.points.getLast();
        return "Провести мышью: "
                + first.x + " " + first.y
                + " -> "
                + last.x + " " + last.y
                + " "
                + totalPathDuration(action)
                + " мс";
    }

    private long totalPathDuration(MouseMovePathAction action) {
        long total = 0;
        for (MouseMovePathAction.PathPoint point : action.points) {
            total += point.dtMs;
        }
        return total;
    }

    private double distanceToSegment(MouseMovePathAction.PathPoint start,
                                     MouseMovePathAction.PathPoint end,
                                     MouseMovePathAction.PathPoint point) {
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared == 0) {
            return Math.hypot(point.x - start.x, point.y - start.y);
        }

        double t = ((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared;
        t = Math.max(0, Math.min(1, t));
        double projectionX = start.x + t * dx;
        double projectionY = start.y + t * dy;
        return Math.hypot(point.x - projectionX, point.y - projectionY);
    }

    private String formatWait(long durationMs) {
        return "Подождать мс: " + durationMs;
    }

    private String formatMouseCommand(MouseButtonActionType actionType, MouseButton button, Integer x, Integer y) {
        String prefix = switch (actionType) {
            case CLICK -> "Нажать";
            case DOWN -> "Зажать";
            case UP -> "Отпустить";
        };

        String suffix = x == null || y == null ? "" : " " + x + " " + y;
        return prefix + " " + mouseButtonName(button) + ":" + suffix;
    }

    private String formatKeyCommand(KeyAction keyAction) {
        String prefix = keyAction.action == KeyActionType.DOWN ? "Зажать клавишу: " : "Отпустить клавишу: ";
        return prefix + formatKeyName(keyAction.keyCode);
    }

    private String formatKeyName(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_ENTER -> "Ввод";
            case KeyEvent.VK_SPACE -> "Пробел";
            case KeyEvent.VK_UP -> "Вверх";
            case KeyEvent.VK_DOWN -> "Вниз";
            case KeyEvent.VK_LEFT -> "Влево";
            case KeyEvent.VK_RIGHT -> "Вправо";
            case KeyEvent.VK_CONTROL -> "Ctrl";
            case KeyEvent.VK_SHIFT -> "Shift";
            case KeyEvent.VK_ALT -> "Alt";
            case KeyEvent.VK_TAB -> "Tab";
            case KeyEvent.VK_ESCAPE -> "Esc";
            case KeyEvent.VK_DELETE -> "Delete";
            case KeyEvent.VK_BACK_SPACE -> "Backspace";
            default -> formatDefaultKeyName(keyCode);
        };
    }

    private String formatDefaultKeyName(int keyCode) {
        String text = KeyEvent.getKeyText(keyCode);
        if (text == null || text.isBlank()) {
            return "Клавиша " + keyCode;
        }
        return text.length() == 1 ? text.toUpperCase(Locale.ROOT) : text;
    }

    private String quoteText(String text) {
        String value = text == null ? "" : text;
        return '"' + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                + '"';
    }

    private String mouseButtonName(MouseButton button) {
        return switch (button) {
            case LEFT -> "ЛКМ";
            case RIGHT -> "ПКМ";
            case MIDDLE -> "СКМ";
        };
    }

    private record KeyComboMatch(List<String> keyNames, int length) {
    }
}
