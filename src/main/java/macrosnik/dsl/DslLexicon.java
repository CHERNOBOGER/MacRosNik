package macrosnik.dsl;

import macrosnik.domain.enums.MouseButton;
import macrosnik.dsl.StructuredDslCommand.DelayUnit;

import java.awt.event.KeyEvent;
import java.util.Locale;

final class DslLexicon {

    private DslLexicon() {
    }

    static String normalize(String token) {
        return token.trim().toLowerCase(Locale.ROOT);
    }

    static String normalizePhrase(String token) {
        return normalize(token).replaceAll("\\s+", " ");
    }

    static boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    static MouseButton tryParseMouseButtonPhrase(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String normalized = normalizePhrase(token);
        if (containsAny(normalized, "лкм", "лев", "left")) {
            return MouseButton.LEFT;
        }
        if (containsAny(normalized, "пкм", "прав", "right")) {
            return MouseButton.RIGHT;
        }
        if (containsAny(normalized, "скм", "сред", "middle")) {
            return MouseButton.MIDDLE;
        }
        return null;
    }

    static String mouseButtonLabel(MouseButton button) {
        return switch (button) {
            case LEFT -> "ЛКМ";
            case RIGHT -> "ПКМ";
            case MIDDLE -> "СКМ";
        };
    }

    static DelayUnit parseDelayUnit(String token) {
        return switch (normalize(token)) {
            case "мс", "ms" -> DelayUnit.MILLISECONDS;
            case "с", "сек", "секунда", "секунды", "секунд", "sec", "second", "seconds" -> DelayUnit.SECONDS;
            case "м", "мин", "минута", "минуты", "минут", "min", "minute", "minutes" -> DelayUnit.MINUTES;
            default -> throw new DslFormatException("неизвестная единица времени: " + token);
        };
    }

    static Integer tryParseRussianLayoutKeyCode(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String normalized = normalize(token);
        if (normalized.length() != 1) {
            return null;
        }

        return switch (normalized.charAt(0)) {
            case 'ё' -> KeyEvent.VK_BACK_QUOTE;
            case 'й' -> KeyEvent.VK_Q;
            case 'ц' -> KeyEvent.VK_W;
            case 'у' -> KeyEvent.VK_E;
            case 'к' -> KeyEvent.VK_R;
            case 'е' -> KeyEvent.VK_T;
            case 'н' -> KeyEvent.VK_Y;
            case 'г' -> KeyEvent.VK_U;
            case 'ш' -> KeyEvent.VK_I;
            case 'щ' -> KeyEvent.VK_O;
            case 'з' -> KeyEvent.VK_P;
            case 'х' -> KeyEvent.VK_OPEN_BRACKET;
            case 'ъ' -> KeyEvent.VK_CLOSE_BRACKET;
            case 'ф' -> KeyEvent.VK_A;
            case 'ы' -> KeyEvent.VK_S;
            case 'в' -> KeyEvent.VK_D;
            case 'а' -> KeyEvent.VK_F;
            case 'п' -> KeyEvent.VK_G;
            case 'р' -> KeyEvent.VK_H;
            case 'о' -> KeyEvent.VK_J;
            case 'л' -> KeyEvent.VK_K;
            case 'д' -> KeyEvent.VK_L;
            case 'ж' -> KeyEvent.VK_SEMICOLON;
            case 'э' -> KeyEvent.VK_QUOTE;
            case 'я' -> KeyEvent.VK_Z;
            case 'ч' -> KeyEvent.VK_X;
            case 'с' -> KeyEvent.VK_C;
            case 'м' -> KeyEvent.VK_V;
            case 'и' -> KeyEvent.VK_B;
            case 'т' -> KeyEvent.VK_N;
            case 'ь' -> KeyEvent.VK_M;
            case 'б' -> KeyEvent.VK_COMMA;
            case 'ю' -> KeyEvent.VK_PERIOD;
            default -> null;
        };
    }
}
