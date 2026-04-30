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
import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MacroDslCodecTest {

    private final MacroDslCodec codec = new MacroDslCodec();

    @Test
    void exportsFriendlyStructuredDsl() {
        Macro macro = new Macro("Тест");
        macro.actions.add(new DelayAction(0, 500));

        macro.actions.add(new MouseButtonAction(0, MouseButton.LEFT, MouseButtonActionType.CLICK, 1087, 583));

        macro.actions.add(new KeyAction(0, KeyEvent.VK_ENTER, KeyActionType.DOWN));
        macro.actions.add(new KeyAction(0, KeyEvent.VK_ENTER, KeyActionType.UP));

        macro.actions.add(new TextInputAction(0, "Товар №42"));

        macro.actions.add(new KeyAction(0, KeyEvent.VK_ALT, KeyActionType.DOWN));
        macro.actions.add(new KeyAction(0, KeyEvent.VK_TAB, KeyActionType.DOWN));
        macro.actions.add(new KeyAction(0, KeyEvent.VK_TAB, KeyActionType.UP));
        macro.actions.add(new KeyAction(0, KeyEvent.VK_ALT, KeyActionType.UP));

        MouseMovePathAction plainMove = new MouseMovePathAction(0);
        plainMove.points.add(new MouseMovePathAction.PathPoint(10, 20, 0));
        plainMove.points.add(new MouseMovePathAction.PathPoint(30, 40, 20));
        plainMove.points.add(new MouseMovePathAction.PathPoint(50, 60, 25));
        macro.actions.add(plainMove);

        macro.actions.add(new MouseButtonAction(0, MouseButton.RIGHT, MouseButtonActionType.DOWN));

        assertEquals("""
                Подождать мс: 500
                Нажать ЛКМ: 1087 583
                Нажать клавишу: Ввод
                Ввести текст: "Товар №42"
                Нажать сочетание: Alt+Tab
                Провести мышью: 10 20 -> 50 60 45 мс
                Зажать ПКМ:
                """.stripTrailing(), codec.toDsl(macro));
    }
}
