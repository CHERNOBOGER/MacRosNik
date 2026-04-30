package macrosnik.dsl;

import macrosnik.domain.enums.KeyActionType;
import macrosnik.domain.enums.MouseButtonActionType;

enum DslVerb {
    WAIT("Подождать", null, null, "подождать", "ждать", "пауза"),
    CLICK("Нажать", MouseButtonActionType.CLICK, KeyActionType.CLICK, "нажать", "клик", "кликнуть"),
    HOLD("Зажать", MouseButtonActionType.DOWN, KeyActionType.DOWN, "зажать"),
    RELEASE("Отпустить", MouseButtonActionType.UP, KeyActionType.UP, "отпустить"),
    MOVE("Переместить", null, null, "переместить", "двигать", "мышь"),
    PATH("Провести", null, null, "провести", "путь"),
    TEXT("Ввести", null, null, "ввести", "набрать", "печатать");

    private final String displayText;
    private final MouseButtonActionType mouseActionType;
    private final KeyActionType keyActionType;
    private final String[] aliases;

    DslVerb(String displayText,
            MouseButtonActionType mouseActionType,
            KeyActionType keyActionType,
            String... aliases) {
        this.displayText = displayText;
        this.mouseActionType = mouseActionType;
        this.keyActionType = keyActionType;
        this.aliases = aliases;
    }

    String displayText() {
        return displayText;
    }

    MouseButtonActionType mouseActionType() {
        return mouseActionType;
    }

    KeyActionType keyActionType() {
        return keyActionType;
    }

    static DslVerb resolve(String token) {
        for (DslVerb verb : values()) {
            for (String alias : verb.aliases) {
                if (alias.equals(token)) {
                    return verb;
                }
            }
        }
        return null;
    }

    static DslVerb fromMouseAction(MouseButtonActionType actionType) {
        return switch (actionType) {
            case CLICK -> CLICK;
            case DOWN -> HOLD;
            case UP -> RELEASE;
        };
    }

    static DslVerb fromKeyAction(KeyActionType actionType) {
        return switch (actionType) {
            case CLICK -> CLICK;
            case DOWN -> HOLD;
            case UP -> RELEASE;
        };
    }
}
