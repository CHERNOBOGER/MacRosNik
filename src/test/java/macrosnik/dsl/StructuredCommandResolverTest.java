package macrosnik.dsl;

import macrosnik.domain.enums.KeyActionType;
import macrosnik.domain.enums.MouseButton;
import macrosnik.domain.enums.MouseButtonActionType;
import macrosnik.dsl.StructuredDslCommand.DelayUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class StructuredCommandResolverTest {

    private final StructuredCommandResolver resolver = new StructuredCommandResolver();

    @Test
    void resolvesKeyCommandFromVerbAndAttributeMarker() {
        StructuredDslCommand command = resolver.resolve("Нажать клавишу", "Enter");

        StructuredDslCommand.KeyCommand keyCommand = assertInstanceOf(StructuredDslCommand.KeyCommand.class, command);
        assertEquals("Enter", keyCommand.data());
        assertEquals("Enter", keyCommand.keyToken());
        assertEquals(KeyActionType.CLICK, keyCommand.actionType());
    }

    @Test
    void resolvesKeyComboFromAttributeWord() {
        StructuredDslCommand command = resolver.resolve("Нажать клавиши Alt+Tab", "");

        StructuredDslCommand.KeyComboCommand comboCommand = assertInstanceOf(StructuredDslCommand.KeyComboCommand.class, command);
        assertEquals("alt+tab", comboCommand.comboText());
    }

    @Test
    void resolvesMouseButtonAndWaitCommandsIndependently() {
        StructuredDslCommand mouseCommand = resolver.resolve("Нажать ЛКМ", "10 20");
        StructuredDslCommand waitCommand = resolver.resolve("Подождать минут", "5");

        StructuredDslCommand.MouseButtonCommand clickCommand = assertInstanceOf(StructuredDslCommand.MouseButtonCommand.class, mouseCommand);
        StructuredDslCommand.WaitCommand delayCommand = assertInstanceOf(StructuredDslCommand.WaitCommand.class, waitCommand);

        assertEquals(MouseButton.LEFT, clickCommand.button());
        assertEquals(MouseButtonActionType.CLICK, clickCommand.actionType());
        assertEquals("10 20", clickCommand.data());
        assertEquals(DelayUnit.MINUTES, delayCommand.defaultUnit());
    }
}
