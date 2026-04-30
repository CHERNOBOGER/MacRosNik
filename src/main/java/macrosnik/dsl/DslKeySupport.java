package macrosnik.dsl;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.Locale;

final class DslKeySupport {

    private DslKeySupport() {
    }

    static int parseKeyCode(String token) {
        Integer russianLayoutKeyCode = DslLexicon.tryParseRussianLayoutKeyCode(token);
        if (russianLayoutKeyCode != null) {
            return russianLayoutKeyCode;
        }

        String normalized = DslLexicon.normalize(token)
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
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

    static String formatKeyName(int keyCode) {
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

    private static int readKeyEventField(String fieldName) {
        try {
            String actualName = fieldName.startsWith("VK_") ? fieldName : "VK_" + fieldName;
            Field field = KeyEvent.class.getField(actualName);
            return field.getInt(null);
        } catch (Exception e) {
            throw new DslFormatException("неизвестная клавиша: " + fieldName.replace("VK_", ""));
        }
    }

    private static String formatDefaultKeyName(int keyCode) {
        String text = KeyEvent.getKeyText(keyCode);
        if (text == null || text.isBlank()) {
            return "Клавиша " + keyCode;
        }
        return text.length() == 1 ? text.toUpperCase(Locale.ROOT) : text;
    }
}
