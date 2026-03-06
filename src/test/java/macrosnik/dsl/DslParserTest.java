package macrosnik.dsl;

import macrosnik.domain.DelayAction;
import macrosnik.domain.KeyAction;
import macrosnik.domain.Macro;
import macrosnik.domain.MouseButtonAction;
import macrosnik.domain.MouseMovePathAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DslParserTest {

    private final DslParser parser = new DslParser();

    @Test
    void parsesBasicCommands() {
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
    void reportsInvalidLine() {
        DslFormatException ex = assertThrows(DslFormatException.class,
                () -> parser.parse("непонятно что-то", "Тест"));

        assertTrue(ex.getMessage().contains("Строка 1"));
    }
}
