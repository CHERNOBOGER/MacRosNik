package macrosnik.dsl;

import macrosnik.domain.DelayAction;
import macrosnik.domain.KeyAction;
import macrosnik.domain.Macro;
import macrosnik.domain.MouseButtonAction;
import macrosnik.domain.MouseMovePathAction;
import macrosnik.domain.TextInputAction;
import macrosnik.domain.enums.KeyActionType;
import macrosnik.domain.enums.MouseButtonActionType;
import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.*;

class DslParserTest {

    private final DslParser parser = new DslParser();

    @Test
    void parsesLegacyCommands() {
        Macro macro = parser.parse("""
                ждать 500
                клавиша CTRL вниз
                клик лкм
                мышь двигать 100 200
                """, "Тест");

        assertEquals(4, macro.actions.size());
        assertInstanceOf(DelayAction.class, macro.actions.get(0));
        assertInstanceOf(KeyAction.class, macro.actions.get(1));
        assertInstanceOf(MouseButtonAction.class, macro.actions.get(2));
        assertInstanceOf(MouseMovePathAction.class, macro.actions.get(3));
    }

    @Test
    void parsesStructuredCommands() {
        Macro macro = parser.parse("""
                Подождать минут: 5
                Нажать ЛКМ: 1087 583
                Нажать клавишу: Ввод
                """, "Тест");

        assertEquals(4, macro.actions.size());

        DelayAction delayAction = assertInstanceOf(DelayAction.class, macro.actions.get(0));
        assertEquals(300_000, delayAction.durationMs);

        MouseMovePathAction moveAction = assertInstanceOf(MouseMovePathAction.class, macro.actions.get(1));
        assertEquals(1087, moveAction.points.getFirst().x);
        assertEquals(583, moveAction.points.getFirst().y);

        MouseButtonAction mouseButtonAction = assertInstanceOf(MouseButtonAction.class, macro.actions.get(2));
        assertEquals(MouseButtonActionType.CLICK, mouseButtonAction.action);

        KeyAction keyClick = assertInstanceOf(KeyAction.class, macro.actions.get(3));
        assertEquals(KeyActionType.CLICK, keyClick.action);
    }

    @Test
    void parsesTextInputAndKeyCombo() {
        Macro macro = parser.parse("""
                Ввести текст: $"Товар №{x}"
                Нажать сочетание: Alt+Tab
                """, "Тест");

        assertEquals(5, macro.actions.size());

        TextInputAction textInputAction = assertInstanceOf(TextInputAction.class, macro.actions.get(0));
        assertEquals("Товар №{x}", textInputAction.text);

        KeyAction altDown = assertInstanceOf(KeyAction.class, macro.actions.get(1));
        KeyAction tabDown = assertInstanceOf(KeyAction.class, macro.actions.get(2));
        KeyAction tabUp = assertInstanceOf(KeyAction.class, macro.actions.get(3));
        KeyAction altUp = assertInstanceOf(KeyAction.class, macro.actions.get(4));

        assertEquals(KeyEvent.VK_ALT, altDown.keyCode);
        assertEquals(KeyEvent.VK_TAB, tabDown.keyCode);
        assertEquals(KeyActionType.DOWN, altDown.action);
        assertEquals(KeyActionType.DOWN, tabDown.action);
        assertEquals(KeyActionType.UP, tabUp.action);
        assertEquals(KeyActionType.UP, altUp.action);
    }

    @Test
    void parsesStructuredAttributesSeparatelyFromVerb() {
        Macro macro = parser.parse("""
                Нажать клавишу Enter:
                Зажать клавишу: Ctrl
                Отпустить клавишу Ctrl:
                """, "Тест");

        assertEquals(3, macro.actions.size());

        KeyAction enterClick = assertInstanceOf(KeyAction.class, macro.actions.get(0));
        assertEquals(KeyEvent.VK_ENTER, enterClick.keyCode);
        assertEquals(KeyActionType.CLICK, enterClick.action);

        KeyAction ctrlDown = assertInstanceOf(KeyAction.class, macro.actions.get(1));
        assertEquals(KeyEvent.VK_CONTROL, ctrlDown.keyCode);
        assertEquals(KeyActionType.DOWN, ctrlDown.action);

        KeyAction ctrlUp = assertInstanceOf(KeyAction.class, macro.actions.get(2));
        assertEquals(KeyEvent.VK_CONTROL, ctrlUp.keyCode);
        assertEquals(KeyActionType.UP, ctrlUp.action);
    }

    @Test
    void parsesInlineKeyComboDeclaredByAttributeWord() {
        Macro macro = parser.parse("Нажать клавиши Alt+Tab:", "Тест");

        assertEquals(4, macro.actions.size());
        assertEquals(KeyEvent.VK_ALT, ((KeyAction) macro.actions.get(0)).keyCode);
        assertEquals(KeyEvent.VK_TAB, ((KeyAction) macro.actions.get(1)).keyCode);
        assertEquals(KeyActionType.DOWN, ((KeyAction) macro.actions.get(0)).action);
        assertEquals(KeyActionType.DOWN, ((KeyAction) macro.actions.get(1)).action);
        assertEquals(KeyActionType.UP, ((KeyAction) macro.actions.get(2)).action);
        assertEquals(KeyActionType.UP, ((KeyAction) macro.actions.get(3)).action);
    }

    @Test
    void treatsCyrillicLettersAsSamePhysicalKeysAsLatin() {
        Macro macro = parser.parse("""
                Нажать клавишу: а
                Нажать клавишу: f
                Нажать клавишу: ф
                Нажать клавишу: a
                Нажать сочетание: Ctrl+Й
                """, "Тест");

        assertEquals(8, macro.actions.size());

        KeyAction russianA = assertInstanceOf(KeyAction.class, macro.actions.get(0));
        KeyAction latinF = assertInstanceOf(KeyAction.class, macro.actions.get(1));
        KeyAction russianEf = assertInstanceOf(KeyAction.class, macro.actions.get(2));
        KeyAction latinA = assertInstanceOf(KeyAction.class, macro.actions.get(3));

        assertEquals(KeyEvent.VK_F, russianA.keyCode);
        assertEquals(KeyEvent.VK_F, latinF.keyCode);
        assertEquals(KeyEvent.VK_A, russianEf.keyCode);
        assertEquals(KeyEvent.VK_A, latinA.keyCode);

        KeyAction ctrlDown = assertInstanceOf(KeyAction.class, macro.actions.get(4));
        KeyAction qDown = assertInstanceOf(KeyAction.class, macro.actions.get(5));
        assertEquals(KeyEvent.VK_CONTROL, ctrlDown.keyCode);
        assertEquals(KeyEvent.VK_Q, qDown.keyCode);
    }

    @Test
    void parsesMousePathCommand() {
        Macro macro = parser.parse("Провести мышью: 10 20 -> 110 220 120 мс", "Тест");

        assertEquals(1, macro.actions.size());
        MouseMovePathAction path = assertInstanceOf(MouseMovePathAction.class, macro.actions.get(0));
        assertTrue(path.points.size() >= 2);
        assertEquals(10, path.points.getFirst().x);
        assertEquals(20, path.points.getFirst().y);
        assertEquals(110, path.points.getLast().x);
        assertEquals(220, path.points.getLast().y);

        long totalDuration = path.points.stream().mapToLong(point -> point.dtMs).sum();
        assertEquals(120, totalDuration);
    }

    @Test
    void supportsKeyShortcutWithEmptyDataSection() {
        Macro macro = parser.parse("Нажать ввод:", "Тест");

        assertEquals(1, macro.actions.size());
        assertInstanceOf(KeyAction.class, macro.actions.get(0));
        assertEquals(KeyActionType.CLICK, ((KeyAction) macro.actions.get(0)).action);
    }

    @Test
    void ignoresInlineComments() {
        Macro macro = parser.parse("Нажать ЛКМ: 10 20 // сюда кликаем", "Тест");

        assertEquals(2, macro.actions.size());
        assertInstanceOf(MouseMovePathAction.class, macro.actions.get(0));
        assertInstanceOf(MouseButtonAction.class, macro.actions.get(1));
    }

    @Test
    void reportsInvalidLine() {
        DslFormatException ex = assertThrows(DslFormatException.class,
                () -> parser.parse("непонятно что-то", "Тест"));

        assertTrue(ex.getMessage().contains("Строка 1"));
    }

    @Test
    void parsesDslWithUtf8Bom() {
        Macro macro = parser.parse("\uFEFFждать 250", "Проверка");

        assertEquals(1, macro.actions.size());
        assertInstanceOf(DelayAction.class, macro.actions.get(0));
    }
}
