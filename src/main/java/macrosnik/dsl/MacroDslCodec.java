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

import java.util.ArrayList;
import java.util.List;

public class MacroDslCodec {
    private static final String WAIT_UNIT_LABEL = "мс";
    private static final String KEY_TARGET_LABEL = "клавишу";
    private static final String KEY_COMBO_LABEL = "сочетание";
    private static final String TEXT_TARGET_LABEL = "текст";
    private static final String MOUSE_TARGET_LABEL = "мышь";
    private static final String MOUSE_PATH_TARGET_LABEL = "мышью";

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
        for (int i = 0; i < actions.size(); ) {
            Action action = actions.get(i);
            appendDelayBefore(lines, action);
            RenderResult result = renderAction(actions, i);
            lines.addAll(result.lines());
            i += result.consumedActions();
        }
        return String.join("\n", lines);
    }

    private void appendDelayBefore(List<String> lines, Action action) {
        if (action.delayBeforeMs > 0) {
            lines.add(formatWait(action.delayBeforeMs));
        }
    }

    private RenderResult renderAction(List<Action> actions, int index) {
        Action action = actions.get(index);
        if (action instanceof DelayAction delayAction) {
            return new RenderResult(List.of(formatWait(delayAction.durationMs)), 1);
        }
        if (action instanceof MouseMovePathAction mouseMovePathAction) {
            return renderMouseMoveAction(actions, index, mouseMovePathAction);
        }
        if (action instanceof MouseButtonAction mouseButtonAction) {
            return new RenderResult(List.of(formatMouseCommand(mouseButtonAction)), 1);
        }
        if (action instanceof TextInputAction textInputAction) {
            return new RenderResult(List.of(formatTextCommand(textInputAction.text)), 1);
        }
        if (action instanceof KeyAction keyAction) {
            return renderKeyAction(actions, index, keyAction);
        }
        throw new IllegalArgumentException("Unsupported action type: " + action.getClass().getName());
    }

    private RenderResult renderMouseMoveAction(List<Action> actions, int index, MouseMovePathAction action) {
        if (canInlineMouseCommand(actions, index)) {
            MouseButtonAction mouseButtonAction = (MouseButtonAction) actions.get(index + 1);
            MouseMovePathAction.PathPoint point = action.points.getFirst();
            return new RenderResult(
                    List.of(formatMouseCommand(mouseButtonAction.action, mouseButtonAction.button, point.x, point.y)),
                    2
            );
        }
        if (canSummarizeMousePath(action)) {
            return new RenderResult(List.of(formatMousePath(action)), 1);
        }
        return new RenderResult(expandMousePath(action), 1);
    }

    private RenderResult renderKeyAction(List<Action> actions, int index, KeyAction keyAction) {
        KeyComboMatch comboMatch = readKeyCombo(actions, index);
        if (comboMatch != null) {
            return new RenderResult(List.of(formatKeyCombo(comboMatch.keyNames())), comboMatch.length());
        }
        if (canInlineKeyClick(actions, index)) {
            return new RenderResult(List.of(formatInlineKeyClick(keyAction.keyCode)), 2);
        }
        return new RenderResult(List.of(formatKeyCommand(keyAction)), 1);
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
        if (mouseButtonAction.hasCoordinates()) {
            return false;
        }
        if (mouseButtonAction.delayBeforeMs > 0 || mouseMovePathAction.points.size() != 1) {
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
            keyNames.add(DslKeySupport.formatKeyName(down.keyCode));
        }
        return new KeyComboMatch(keyNames, downs.size() * 2);
    }

    private List<String> expandMousePath(MouseMovePathAction action) {
        List<String> lines = new ArrayList<>();
        for (MouseMovePathAction.PathPoint point : action.points) {
            lines.add(formatMouseMove(point.x, point.y));
            if (point.dtMs > 0) {
                lines.add(formatWait(point.dtMs));
            }
        }
        return lines;
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
        return DslVerb.PATH.displayText() + " " + MOUSE_PATH_TARGET_LABEL + ": "
                + first.x + " " + first.y
                + " -> "
                + last.x + " " + last.y
                + " "
                + totalPathDuration(action)
                + " " + WAIT_UNIT_LABEL;
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
        return DslVerb.WAIT.displayText() + " " + WAIT_UNIT_LABEL + ": " + durationMs;
    }

    private String formatMouseMove(int x, int y) {
        return DslVerb.MOVE.displayText() + " " + MOUSE_TARGET_LABEL + ": " + x + " " + y;
    }

    private String formatMouseCommand(MouseButtonAction action) {
        Integer x = action.hasCoordinates() ? action.x : null;
        Integer y = action.hasCoordinates() ? action.y : null;
        return formatMouseCommand(action.action, action.button, x, y);
    }

    private String formatMouseCommand(MouseButtonActionType actionType, MouseButton button, Integer x, Integer y) {
        String prefix = DslVerb.fromMouseAction(actionType).displayText();
        String suffix = x == null || y == null ? "" : " " + x + " " + y;
        return prefix + " " + DslLexicon.mouseButtonLabel(button) + ":" + suffix;
    }

    private String formatTextCommand(String text) {
        return DslVerb.TEXT.displayText() + " " + TEXT_TARGET_LABEL + ": " + quoteText(text);
    }

    private String formatKeyCombo(List<String> keyNames) {
        return DslVerb.CLICK.displayText() + " " + KEY_COMBO_LABEL + ": " + String.join("+", keyNames);
    }

    private String formatInlineKeyClick(int keyCode) {
        return DslVerb.CLICK.displayText() + " " + KEY_TARGET_LABEL + ": " + DslKeySupport.formatKeyName(keyCode);
    }

    private String formatKeyCommand(KeyAction keyAction) {
        return DslVerb.fromKeyAction(keyAction.action).displayText()
                + " "
                + KEY_TARGET_LABEL
                + ": "
                + DslKeySupport.formatKeyName(keyAction.keyCode);
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

    private record KeyComboMatch(List<String> keyNames, int length) {
    }

    private record RenderResult(List<String> lines, int consumedActions) {
    }
}
