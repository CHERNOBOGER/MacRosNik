package macrosnik.dsl;

import macrosnik.domain.DelayAction;
import macrosnik.domain.KeyAction;
import macrosnik.domain.Macro;
import macrosnik.domain.MouseButtonAction;
import macrosnik.domain.MouseMovePathAction;
import macrosnik.domain.enums.KeyActionType;
import macrosnik.domain.enums.MouseButton;
import macrosnik.domain.enums.MouseButtonActionType;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DslParser {

    public Macro parse(String text, String macroName) {
        Macro macro = new Macro(macroName == null || macroName.isBlank() ? "DSL-макрос" : macroName.trim());
        String[] lines = text == null ? new String[0] : text.split("\\R");

        List<String> errors = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String rawLine = lines[i].trim();
            if (rawLine.isEmpty() || rawLine.startsWith("#") || rawLine.startsWith("//")) {
                continue;
            }
            try {
                parseLine(rawLine, macro);
            } catch (Exception e) {
                errors.add("Строка " + (i + 1) + ": " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new DslFormatException(String.join("\n", errors));
        }
        return macro;
    }

    private void parseLine(String line, Macro macro) {
        String[] tokens = line.split("\\s+");
        String first = normalize(tokens[0]);

        if (first.equals("ждать") || first.equals("пауза")) {
            requireLength(tokens, 2, line);
            macro.actions.add(new DelayAction(0, parseLong(tokens[1], "ожидание")));
            return;
        }

        if (first.equals("клик")) {
            requireLength(tokens, 2, line);
            macro.actions.add(new MouseButtonAction(0, parseMouseButton(tokens[1]), MouseButtonActionType.CLICK));
            return;
        }

        if (first.equals("мышь")) {
            parseMouse(tokens, macro, line);
            return;
        }

        if (first.equals("клавиша")) {
            parseKey(tokens, macro, line);
            return;
        }

        throw new DslFormatException("неизвестная команда: " + tokens[0]);
    }

    private void parseMouse(String[] tokens, Macro macro, String line) {
        requireLength(tokens, 3, line);
        String second = normalize(tokens[1]);

        if (second.equals("двигать") || second.equals("переместить")) {
            requireLength(tokens, 4, line);
            int x = parseInt(tokens[2], "x");
            int y = parseInt(tokens[3], "y");
            MouseMovePathAction action = new MouseMovePathAction(0);
            action.points.add(new MouseMovePathAction.PathPoint(x, y, 0));
            macro.actions.add(action);
            return;
        }

        MouseButton button = parseMouseButton(tokens[1]);
        MouseButtonActionType actionType = parseMouseAction(tokens[2]);
        macro.actions.add(new MouseButtonAction(0, button, actionType));
    }

    private void parseKey(String[] tokens, Macro macro, String line) {
        requireLength(tokens, 3, line);
        int keyCode = parseKeyCode(tokens[1]);
        String actionToken = normalize(tokens[2]);

        if (actionToken.equals("нажать") || actionToken.equals("клик")) {
            macro.actions.add(new KeyAction(0, keyCode, KeyActionType.DOWN));
            macro.actions.add(new KeyAction(0, keyCode, KeyActionType.UP));
            return;
        }

        KeyActionType type = switch (actionToken) {
            case "вниз", "down", "нажата" -> KeyActionType.DOWN;
            case "вверх", "up", "отпустить" -> KeyActionType.UP;
            default -> throw new DslFormatException("неизвестное действие клавиши: " + tokens[2]);
        };
        macro.actions.add(new KeyAction(0, keyCode, type));
    }

    private MouseButton parseMouseButton(String token) {
        return switch (normalize(token)) {
            case "левая", "лкм", "left" -> MouseButton.LEFT;
            case "правая", "пкм", "right" -> MouseButton.RIGHT;
            case "средняя", "скм", "middle" -> MouseButton.MIDDLE;
            default -> throw new DslFormatException("неизвестная кнопка мыши: " + token);
        };
    }

    private MouseButtonActionType parseMouseAction(String token) {
        return switch (normalize(token)) {
            case "вниз", "down", "нажать" -> MouseButtonActionType.DOWN;
            case "вверх", "up", "отпустить" -> MouseButtonActionType.UP;
            case "клик", "click" -> MouseButtonActionType.CLICK;
            default -> throw new DslFormatException("неизвестное действие мыши: " + token);
        };
    }

    private int parseKeyCode(String token) {
        String normalized = normalize(token).toUpperCase(Locale.ROOT);
        if (normalized.length() == 1) {
            return KeyEvent.getExtendedKeyCodeForChar(normalized.charAt(0));
        }

        if (normalized.matches("F\\d{1,2}")) {
            return readKeyEventField(normalized);
        }

        return switch (normalized) {
            case "CTRL", "CONTROL" -> KeyEvent.VK_CONTROL;
            case "SHIFT" -> KeyEvent.VK_SHIFT;
            case "ALT" -> KeyEvent.VK_ALT;
            case "ENTER" -> KeyEvent.VK_ENTER;
            case "ESC", "ESCAPE" -> KeyEvent.VK_ESCAPE;
            case "SPACE", "ПРОБЕЛ" -> KeyEvent.VK_SPACE;
            case "TAB" -> KeyEvent.VK_TAB;
            case "BACKSPACE" -> KeyEvent.VK_BACK_SPACE;
            case "DELETE" -> KeyEvent.VK_DELETE;
            case "UP" -> KeyEvent.VK_UP;
            case "DOWN" -> KeyEvent.VK_DOWN;
            case "LEFT" -> KeyEvent.VK_LEFT;
            case "RIGHT" -> KeyEvent.VK_RIGHT;
            default -> readKeyEventField("VK_" + normalized);
        };
    }

    private int readKeyEventField(String fieldName) {
        try {
            String actualName = fieldName.startsWith("VK_") ? fieldName : "VK_" + fieldName;
            Field field = KeyEvent.class.getField(actualName);
            return field.getInt(null);
        } catch (Exception e) {
            throw new DslFormatException("неизвестная клавиша: " + fieldName.replace("VK_", ""));
        }
    }

    private String normalize(String token) {
        return token.trim().toLowerCase(Locale.ROOT);
    }

    private int parseInt(String token, String fieldName) {
        try {
            return Integer.parseInt(token.replace("мс", ""));
        } catch (NumberFormatException e) {
            throw new DslFormatException("некорректное число для " + fieldName + ": " + token);
        }
    }

    private long parseLong(String token, String fieldName) {
        try {
            return Long.parseLong(token.replace("мс", ""));
        } catch (NumberFormatException e) {
            throw new DslFormatException("некорректное число для " + fieldName + ": " + token);
        }
    }

    private void requireLength(String[] tokens, int minLength, String line) {
        if (tokens.length < minLength) {
            throw new DslFormatException("недостаточно аргументов: " + line);
        }
    }
}
