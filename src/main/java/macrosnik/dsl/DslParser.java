package macrosnik.dsl;

import macrosnik.domain.DelayAction;
import macrosnik.domain.KeyAction;
import macrosnik.domain.Macro;
import macrosnik.domain.MouseButtonAction;
import macrosnik.domain.MouseMovePathAction;
import macrosnik.domain.TextInputAction;
import macrosnik.domain.enums.KeyActionType;
import macrosnik.domain.enums.MouseButton;
import macrosnik.domain.enums.MouseButtonActionType;
import macrosnik.dsl.StructuredDslCommand.DelayUnit;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DslParser {

    private static final Pattern DURATION_PATTERN = Pattern.compile("^(-?\\d+)\\s*([\\p{L}_]+)?$");
    private static final Pattern MOUSE_PATH_PATTERN = Pattern.compile(
            "^(-?\\d+)\\s+(-?\\d+)\\s*->\\s*(-?\\d+)\\s+(-?\\d+)(?:\\s+(.+))?$"
    );
    private final StructuredCommandResolver structuredCommandResolver = new StructuredCommandResolver();

    public Macro parse(String text, String macroName) {
        Macro macro = new Macro(macroName == null || macroName.isBlank() ? "Макрос из сценария" : macroName.trim());
        String[] lines = text == null ? new String[0] : text.split("\\R");

        List<String> errors = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String rawLine = stripUtf8Bom(stripInlineComment(lines[i])).trim();
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
        if (line.contains(":")) {
            parseStructuredLine(line, macro);
            return;
        }
        parseLegacyLine(line, macro);
    }

    private void parseStructuredLine(String line, Macro macro) {
        int colonIndex = line.indexOf(':');
        StructuredDslCommand command = structuredCommandResolver.resolve(
                line.substring(0, colonIndex),
                line.substring(colonIndex + 1).trim()
        );

        switch (command) {
            case StructuredDslCommand.WaitCommand waitCommand -> parseWaitCommand(waitCommand, macro);
            case StructuredDslCommand.TextCommand textCommand -> parseTextCommand(textCommand, macro);
            case StructuredDslCommand.MouseMoveCommand moveCommand -> parseMouseMoveCommand(moveCommand, macro);
            case StructuredDslCommand.MousePathCommand pathCommand -> parseMousePathCommand(pathCommand, macro);
            case StructuredDslCommand.MouseButtonCommand mouseButtonCommand -> parseMouseButtonCommand(mouseButtonCommand, macro);
            case StructuredDslCommand.KeyCommand keyCommand -> parseKeyCommand(keyCommand, macro);
            case StructuredDslCommand.KeyComboCommand keyComboCommand -> parseKeyComboCommand(keyComboCommand, macro);
        }
    }

    private void parseLegacyLine(String line, Macro macro) {
        String[] tokens = line.split("\\s+");
        String first = DslLexicon.normalize(tokens[0]);

        if (first.equals("ждать") || first.equals("пауза")) {
            requireLength(tokens, 2, line);
            macro.actions.add(new DelayAction(0, parseDurationMs(tokens[1], DelayUnit.MILLISECONDS)));
            return;
        }

        if (first.equals("клик")) {
            requireLength(tokens, 2, line);
            macro.actions.add(new MouseButtonAction(0, parseMouseButton(tokens[1]), MouseButtonActionType.CLICK));
            return;
        }

        if (first.equals("мышь")) {
            parseLegacyMouse(tokens, macro, line);
            return;
        }

        if (first.equals("клавиша")) {
            parseLegacyKey(tokens, macro, line);
            return;
        }

        throw new DslFormatException("неизвестная команда: " + tokens[0]);
    }

    private void parseTextCommand(StructuredDslCommand.TextCommand command, Macro macro) {
        requireData(command.data(), command.normalizedHead());
        macro.actions.add(new TextInputAction(0, parseTextValue(command.data())));
    }

    private void parseKeyComboCommand(StructuredDslCommand.KeyComboCommand command, Macro macro) {
        List<Integer> keyCodes = parseKeySequence(command.comboText());
        if (keyCodes.size() < 2) {
            throw new DslFormatException("в сочетании должно быть хотя бы две клавиши");
        }

        for (int keyCode : keyCodes) {
            macro.actions.add(new KeyAction(0, keyCode, KeyActionType.DOWN));
        }
        for (int i = keyCodes.size() - 1; i >= 0; i--) {
            macro.actions.add(new KeyAction(0, keyCodes.get(i), KeyActionType.UP));
        }
    }

    private void parseWaitCommand(StructuredDslCommand.WaitCommand command, Macro macro) {
        requireData(command.data(), command.normalizedHead());
        macro.actions.add(new DelayAction(0, parseDurationMs(command.data(), command.defaultUnit())));
    }

    private void parseMouseMoveCommand(StructuredDslCommand.MouseMoveCommand command, Macro macro) {
        requireData(command.data(), command.normalizedHead());
        int[] coordinates = parseCoordinates(command.data());
        macro.actions.add(mouseMoveAction(coordinates[0], coordinates[1]));
    }

    private void parseMousePathCommand(StructuredDslCommand.MousePathCommand command, Macro macro) {
        requireData(command.data(), command.normalizedHead());
        Matcher matcher = MOUSE_PATH_PATTERN.matcher(command.data().trim());
        if (!matcher.matches()) {
            throw new DslFormatException("ожидался путь вида: x1 y1 -> x2 y2 300 мс");
        }

        int startX = parseInt(matcher.group(1), "x1");
        int startY = parseInt(matcher.group(2), "y1");
        int endX = parseInt(matcher.group(3), "x2");
        int endY = parseInt(matcher.group(4), "y2");
        long durationMs = matcher.group(5) == null || matcher.group(5).isBlank()
                ? 300
                : parseDurationMs(matcher.group(5), DelayUnit.MILLISECONDS);

        macro.actions.add(createLinearMousePath(startX, startY, endX, endY, durationMs));
    }

    private void parseMouseButtonCommand(StructuredDslCommand.MouseButtonCommand command, Macro macro) {
        if (!command.data().isBlank()) {
            int[] coordinates = parseCoordinates(command.data());
            macro.actions.add(mouseMoveAction(coordinates[0], coordinates[1]));
        }
        macro.actions.add(new MouseButtonAction(0, command.button(), command.actionType()));
    }

    private void parseKeyCommand(StructuredDslCommand.KeyCommand command, Macro macro) {
        int keyCode = parseKeyCode(command.keyToken());
        macro.actions.add(new KeyAction(0, keyCode, command.actionType()));
    }

    private void parseLegacyMouse(String[] tokens, Macro macro, String line) {
        requireLength(tokens, 3, line);
        String second = DslLexicon.normalize(tokens[1]);

        if (second.equals("двигать") || second.equals("переместить")) {
            requireLength(tokens, 4, line);
            int x = parseInt(tokens[2], "x");
            int y = parseInt(tokens[3], "y");
            macro.actions.add(mouseMoveAction(x, y));
            return;
        }

        MouseButton button = parseMouseButton(tokens[1]);
        MouseButtonActionType actionType = parseMouseAction(tokens[2]);
        macro.actions.add(new MouseButtonAction(0, button, actionType));
    }

    private void parseLegacyKey(String[] tokens, Macro macro, String line) {
        requireLength(tokens, 3, line);
        int keyCode = parseKeyCode(tokens[1]);
        String actionToken = DslLexicon.normalize(tokens[2]);

        if (actionToken.equals("нажать") || actionToken.equals("клик")) {
            macro.actions.add(new KeyAction(0, keyCode, KeyActionType.CLICK));
            return;
        }

        KeyActionType type = switch (actionToken) {
            case "вниз", "down", "нажата" -> KeyActionType.DOWN;
            case "вверх", "up", "отпустить" -> KeyActionType.UP;
            default -> throw new DslFormatException("неизвестное действие клавиши: " + tokens[2]);
        };
        macro.actions.add(new KeyAction(0, keyCode, type));
    }

    private MouseMovePathAction mouseMoveAction(int x, int y) {
        MouseMovePathAction action = new MouseMovePathAction(0);
        action.points.add(new MouseMovePathAction.PathPoint(x, y, 0));
        return action;
    }

    private MouseMovePathAction createLinearMousePath(int startX, int startY, int endX, int endY, long durationMs) {
        MouseMovePathAction action = new MouseMovePathAction(0);
        action.points.add(new MouseMovePathAction.PathPoint(startX, startY, 0));

        if (startX == endX && startY == endY) {
            action.points.getFirst().dtMs = safeInt(durationMs);
            return action;
        }

        double distance = Math.hypot(endX - startX, endY - startY);
        int segmentsByTime = durationMs <= 0 ? 1 : (int) Math.ceil(durationMs / 20.0);
        int segmentsByDistance = distance <= 0 ? 1 : (int) Math.ceil(distance / 12.0);
        int segments = Math.max(1, Math.min(60, Math.max(segmentsByTime, segmentsByDistance)));

        long baseDt = segments == 0 ? 0 : durationMs / segments;
        long remainder = segments == 0 ? 0 : durationMs % segments;

        for (int i = 1; i <= segments; i++) {
            int x = (int) Math.round(startX + (endX - startX) * (i / (double) segments));
            int y = (int) Math.round(startY + (endY - startY) * (i / (double) segments));
            int dt = safeInt(baseDt + (i <= remainder ? 1 : 0));
            appendOrMergePoint(action, x, y, dt);
        }

        ensureLastPoint(action, endX, endY);
        return action;
    }

    private void appendOrMergePoint(MouseMovePathAction action, int x, int y, int dtMs) {
        MouseMovePathAction.PathPoint last = action.points.getLast();
        if (last.x == x && last.y == y) {
            last.dtMs += dtMs;
            return;
        }
        action.points.add(new MouseMovePathAction.PathPoint(x, y, dtMs));
    }

    private void ensureLastPoint(MouseMovePathAction action, int endX, int endY) {
        MouseMovePathAction.PathPoint last = action.points.getLast();
        if (last.x != endX || last.y != endY) {
            action.points.add(new MouseMovePathAction.PathPoint(endX, endY, 0));
        }
    }

    private int safeInt(long value) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
    }

    private MouseButton parseMouseButton(String token) {
        MouseButton button = DslLexicon.tryParseMouseButtonPhrase(token);
        if (button == null) {
            throw new DslFormatException("неизвестная кнопка мыши: " + token);
        }
        return button;
    }

    private MouseButtonActionType parseMouseAction(String token) {
        return switch (DslLexicon.normalize(token)) {
            case "вниз", "down", "нажать" -> MouseButtonActionType.DOWN;
            case "вверх", "up", "отпустить" -> MouseButtonActionType.UP;
            case "клик", "click" -> MouseButtonActionType.CLICK;
            default -> throw new DslFormatException("неизвестное действие мыши: " + token);
        };
    }

    private int parseKeyCode(String token) {
        Integer russianLayoutKeyCode = DslLexicon.tryParseRussianLayoutKeyCode(token);
        if (russianLayoutKeyCode != null) {
            return russianLayoutKeyCode;
        }

        String normalized = DslLexicon.normalize(token).toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        String collapsed = normalized.replace("_", "");
        if (collapsed.length() == 1) {
            return KeyEvent.getExtendedKeyCodeForChar(collapsed.charAt(0));
        }

        if (collapsed.matches("F\\d{1,2}")) {
            return readKeyEventField(collapsed);
        }

        return switch (collapsed) {
            case "CTRL", "CONTROL", "КОНТРОЛ", "КОНТРОЛЬ" -> KeyEvent.VK_CONTROL;
            case "SHIFT", "ШИФТ" -> KeyEvent.VK_SHIFT;
            case "ALT", "АЛЬТ" -> KeyEvent.VK_ALT;
            case "ENTER", "ВВОД" -> KeyEvent.VK_ENTER;
            case "ESC", "ESCAPE", "ЭСК", "ЭСКЕЙП" -> KeyEvent.VK_ESCAPE;
            case "SPACE", "ПРОБЕЛ" -> KeyEvent.VK_SPACE;
            case "TAB", "ТАБ", "ТАБУЛЯЦИЯ" -> KeyEvent.VK_TAB;
            case "BACKSPACE", "БЭКСПЕЙС" -> KeyEvent.VK_BACK_SPACE;
            case "DELETE", "DEL", "УДАЛИТЬ", "УДАЛЕНИЕ" -> KeyEvent.VK_DELETE;
            case "UP", "ВВЕРХ" -> KeyEvent.VK_UP;
            case "DOWN", "ВНИЗ" -> KeyEvent.VK_DOWN;
            case "LEFT", "ВЛЕВО" -> KeyEvent.VK_LEFT;
            case "RIGHT", "ВПРАВО" -> KeyEvent.VK_RIGHT;
            default -> readKeyEventField("VK_" + normalized);
        };
    }

    private List<Integer> parseKeySequence(String data) {
        String[] tokens = data.split("\\s*\\+\\s*");
        List<Integer> keyCodes = new ArrayList<>();
        for (String token : tokens) {
            if (token.isBlank()) {
                throw new DslFormatException("пустая клавиша в сочетании: " + data);
            }
            keyCodes.add(parseKeyCode(token));
        }
        return keyCodes;
    }

    private String parseTextValue(String data) {
        String trimmed = data.trim();
        if (trimmed.startsWith("$\"") && trimmed.endsWith("\"") && trimmed.length() >= 3) {
            return unescapeQuotedText(trimmed.substring(2, trimmed.length() - 1));
        }
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return unescapeQuotedText(trimmed.substring(1, trimmed.length() - 1));
        }
        return trimmed;
    }

    private String unescapeQuotedText(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        boolean escaping = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (!escaping && ch == '\\') {
                escaping = true;
                continue;
            }
            if (escaping) {
                builder.append(switch (ch) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case '"' -> '"';
                    case '\\' -> '\\';
                    default -> ch;
                });
                escaping = false;
                continue;
            }
            builder.append(ch);
        }
        if (escaping) {
            builder.append('\\');
        }
        return builder.toString();
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

    private long parseDurationMs(String token, DelayUnit defaultUnit) {
        String value = DslLexicon.normalizePhrase(token);
        Matcher matcher = DURATION_PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new DslFormatException("некорректное время ожидания: " + token);
        }

        long amount;
        try {
            amount = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            throw new DslFormatException("некорректное время ожидания: " + token);
        }

        DelayUnit unit = matcher.group(2) == null || matcher.group(2).isBlank()
                ? defaultUnit
                : DslLexicon.parseDelayUnit(matcher.group(2));
        return amount * unit.multiplierMs;
    }

    private int[] parseCoordinates(String data) {
        String[] tokens = data.trim().split("\\s+");
        if (tokens.length < 2) {
            throw new DslFormatException("ожидались координаты x y");
        }
        return new int[] {
                parseInt(tokens[0], "x"),
                parseInt(tokens[1], "y")
        };
    }

    private String stripInlineComment(String line) {
        boolean inQuotes = false;
        for (int i = 0; i < line.length() - 1; i++) {
            char current = line.charAt(i);
            if (current == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            if (!inQuotes && current == '/' && line.charAt(i + 1) == '/') {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private String stripUtf8Bom(String line) {
        return line != null && !line.isEmpty() && line.charAt(0) == '\uFEFF'
                ? line.substring(1)
                : line;
    }

    private int parseInt(String token, String fieldName) {
        try {
            return Integer.parseInt(token.replace("мс", ""));
        } catch (NumberFormatException e) {
            throw new DslFormatException("некорректное число для " + fieldName + ": " + token);
        }
    }

    private void requireData(String data, String command) {
        if (data == null || data.isBlank()) {
            throw new DslFormatException("не хватает данных для команды: " + command);
        }
    }

    private void requireLength(String[] tokens, int minLength, String line) {
        if (tokens.length < minLength) {
            throw new DslFormatException("недостаточно аргументов: " + line);
        }
    }
}
