package macrosnik.dsl;

import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DslKeySupportTest {

    @Test
    void parsesLocalizedAndLatinKeyAliases() {
        assertEquals(KeyEvent.VK_CONTROL, DslKeySupport.parseKeyCode("Ctrl"));
        assertEquals(KeyEvent.VK_DELETE, DslKeySupport.parseKeyCode("Удалить"));
        assertEquals(KeyEvent.VK_Q, DslKeySupport.parseKeyCode("й"));
    }

    @Test
    void formatsSpecialAndSingleCharacterKeys() {
        assertEquals("Delete", DslKeySupport.formatKeyName(KeyEvent.VK_DELETE));
        assertEquals("Ввод", DslKeySupport.formatKeyName(KeyEvent.VK_ENTER));
        assertEquals("A", DslKeySupport.formatKeyName(KeyEvent.VK_A));
    }
}
