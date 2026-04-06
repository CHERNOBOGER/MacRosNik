package macrosnik.dsl;

import macrosnik.domain.enums.KeyActionType;
import macrosnik.domain.enums.MouseButton;
import macrosnik.domain.enums.MouseButtonActionType;
import macrosnik.dsl.StructuredDslCommand.DelayUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class StructuredCommandResolver {

    private static final Set<String> KEY_MARKERS = Set.of("клавиша", "клавишу");
    private static final Set<String> KEY_COMBO_MARKERS = Set.of("сочетание", "комбинация", "комбинацию", "клавиши", "комбо");
    private static final Set<String> TEXT_MARKERS = Set.of("текст");
    private static final Set<String> MOVE_VERB_ATTRIBUTES = Set.of("двигать", "переместить");

    StructuredDslCommand resolve(String headText, String data) {
        ParsedHead head = parseHead(headText, data);
        return switch (head.verb()) {
            case WAIT -> resolveWait(head);
            case MOVE -> resolveMove(head);
            case PATH -> resolvePath(head);
            case TEXT -> resolveText(head);
            case CLICK, HOLD, RELEASE -> resolveAction(head);
        };
    }

    private ParsedHead parseHead(String headText, String data) {
        String originalHead = headText == null ? "" : headText.trim();
        String normalizedHead = DslLexicon.normalizePhrase(originalHead);
        if (normalizedHead.isEmpty()) {
            throw new DslFormatException("пустая команда");
        }

        String[] words = normalizedHead.split(" ");
        StructuredVerb verb = StructuredVerb.resolve(words[0]);
        if (verb == null) {
            throw new DslFormatException("неизвестная команда: " + originalHead);
        }

        List<String> attributes = new ArrayList<>();
        for (int i = 1; i < words.length; i++) {
            attributes.add(words[i]);
        }

        return new ParsedHead(originalHead, normalizedHead, List.copyOf(attributes), data, verb);
    }

    private StructuredDslCommand resolveWait(ParsedHead head) {
        DelayUnit defaultUnit = head.hasNoAttributes()
                ? DelayUnit.MILLISECONDS
                : DslLexicon.parseDelayUnit(head.attributePhrase());
        requireData(head.data(), head.normalizedHead());
        return new StructuredDslCommand.WaitCommand(head.originalHead(), head.normalizedHead(), head.data(), defaultUnit);
    }

    private StructuredDslCommand resolveMove(ParsedHead head) {
        String attributes = head.attributePhrase();
        requireSupportedAttributes(head,
                attributes.isBlank()
                        || DslLexicon.containsAny(attributes, "мыш", "курсор", "cursor")
                        || MOVE_VERB_ATTRIBUTES.contains(attributes));
        requireData(head.data(), head.normalizedHead());
        return new StructuredDslCommand.MouseMoveCommand(head.originalHead(), head.normalizedHead(), head.data());
    }

    private StructuredDslCommand resolvePath(ParsedHead head) {
        String attributes = head.attributePhrase();
        requireSupportedAttributes(head, attributes.isBlank() || DslLexicon.containsAny(attributes, "мыш", "курсор", "cursor"));
        requireData(head.data(), head.normalizedHead());
        return new StructuredDslCommand.MousePathCommand(head.originalHead(), head.normalizedHead(), head.data());
    }

    private StructuredDslCommand resolveText(ParsedHead head) {
        requireSupportedAttributes(head, head.hasNoAttributes() || head.hasSingleAttribute(TEXT_MARKERS));
        requireData(head.data(), head.normalizedHead());
        return new StructuredDslCommand.TextCommand(head.originalHead(), head.normalizedHead(), head.data());
    }

    private StructuredDslCommand resolveAction(ParsedHead head) {
        if (head.firstAttributeIs(KEY_COMBO_MARKERS)) {
            if (head.verb().keyActionType != KeyActionType.CLICK) {
                throw new DslFormatException("сочетание поддерживается только для команды нажать");
            }
            String comboText = resolveValue(head, head.trailingAttributePhrase());
            return new StructuredDslCommand.KeyComboCommand(head.originalHead(), head.normalizedHead(), head.data(), comboText);
        }

        MouseButton button = DslLexicon.tryParseMouseButtonPhrase(head.attributePhrase());
        if (button != null) {
            return new StructuredDslCommand.MouseButtonCommand(
                    head.originalHead(),
                    head.normalizedHead(),
                    head.data(),
                    button,
                    head.verb().mouseActionType
            );
        }

        String keyToken = resolveKeyToken(head);
        if (keyToken.isBlank()) {
            throw new DslFormatException("неизвестная цель команды: " + head.originalHead());
        }
        return new StructuredDslCommand.KeyCommand(
                head.originalHead(),
                head.normalizedHead(),
                head.data(),
                keyToken,
                head.verb().keyActionType
        );
    }

    private String resolveKeyToken(ParsedHead head) {
        if (head.hasNoAttributes()) {
            return head.data();
        }

        if (head.firstAttributeIs(KEY_MARKERS)) {
            return resolveValue(head, head.trailingAttributePhrase());
        }

        if (!head.data().isBlank()) {
            throw new DslFormatException("лишние аргументы в команде: " + head.originalHead() + ": " + head.data());
        }
        return head.attributePhrase();
    }

    private String resolveValue(ParsedHead head, String inlineValue) {
        if (!inlineValue.isBlank() && !head.data().isBlank()) {
            throw new DslFormatException("лишние аргументы в команде: " + head.originalHead() + ": " + head.data());
        }

        String value = head.data().isBlank() ? inlineValue : head.data();
        requireData(value, head.normalizedHead());
        return value;
    }

    private void requireSupportedAttributes(ParsedHead head, boolean condition) {
        if (!condition) {
            throw new DslFormatException("неподдерживаемые атрибуты команды: " + head.originalHead());
        }
    }

    private void requireData(String data, String command) {
        if (data == null || data.isBlank()) {
            throw new DslFormatException("не хватает данных для команды: " + command);
        }
    }

    private record ParsedHead(String originalHead,
                              String normalizedHead,
                              List<String> attributes,
                              String data,
                              StructuredVerb verb) {

        String attributePhrase() {
            return String.join(" ", attributes);
        }

        boolean hasNoAttributes() {
            return attributes.isEmpty();
        }

        boolean hasSingleAttribute(Set<String> values) {
            return attributes.size() == 1 && values.contains(attributes.getFirst());
        }

        boolean firstAttributeIs(Set<String> values) {
            return !attributes.isEmpty() && values.contains(attributes.getFirst());
        }

        String trailingAttributePhrase() {
            return attributes.size() <= 1 ? "" : String.join(" ", attributes.subList(1, attributes.size()));
        }
    }

    private enum StructuredVerb {
        WAIT(null, null, "подождать", "ждать", "пауза"),
        CLICK(MouseButtonActionType.CLICK, KeyActionType.CLICK, "нажать", "клик", "кликнуть"),
        HOLD(MouseButtonActionType.DOWN, KeyActionType.DOWN, "зажать"),
        RELEASE(MouseButtonActionType.UP, KeyActionType.UP, "отпустить"),
        MOVE(null, null, "переместить", "двигать", "мышь"),
        PATH(null, null, "провести", "путь"),
        TEXT(null, null, "ввести", "набрать", "печатать");

        private final MouseButtonActionType mouseActionType;
        private final KeyActionType keyActionType;
        private final String[] aliases;

        StructuredVerb(MouseButtonActionType mouseActionType,
                       KeyActionType keyActionType,
                       String... aliases) {
            this.mouseActionType = mouseActionType;
            this.keyActionType = keyActionType;
            this.aliases = aliases;
        }

        private static StructuredVerb resolve(String token) {
            for (StructuredVerb verb : values()) {
                for (String alias : verb.aliases) {
                    if (alias.equals(token)) {
                        return verb;
                    }
                }
            }
            return null;
        }
    }
}
