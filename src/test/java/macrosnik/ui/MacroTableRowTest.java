package macrosnik.ui;

import macrosnik.domain.CoordinateMode;
import macrosnik.domain.MouseButtonAction;
import macrosnik.domain.MouseMovePathAction;
import macrosnik.domain.TextInputAction;
import macrosnik.domain.enums.MouseButton;
import macrosnik.domain.enums.MouseButtonActionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MacroTableRowTest {

    @Test
    void formatsPathActionWithOneBasedIndexAndReadableSummary() {
        MouseMovePathAction action = new MouseMovePathAction(15);
        action.coordinateMode = CoordinateMode.WINDOW_RELATIVE;
        action.points.add(new MouseMovePathAction.PathPoint(10, 20, 0));
        action.points.add(new MouseMovePathAction.PathPoint(30, 40, 16));

        MacroTableRow row = MacroTableRow.from(0, action);

        assertEquals("1. Перемещение мыши", row.typeProperty().get());
        assertEquals("15 мс", row.delayProperty().get());
        assertEquals(
                "Точек: 2, от (10, 20) до (30, 40), время: 16 мс, режим: относительно окна",
                row.detailsProperty().get()
        );
    }

    @Test
    void normalizesLineBreaksAndTruncatesLongTextPreview() {
        TextInputAction action = new TextInputAction(0, "Первая строка\r\nВторая строка 12345678901234567890");

        MacroTableRow row = MacroTableRow.from(2, action);

        assertEquals("3. Ввод текста", row.typeProperty().get());
        assertEquals("Текст: \"Первая строка\\nВторая строка 12345678...\"", row.detailsProperty().get());
    }

    @Test
    void includesCoordinatesForMouseButtonActions() {
        MouseButtonAction action = new MouseButtonAction(12, MouseButton.LEFT, MouseButtonActionType.CLICK, 400, 300);

        MacroTableRow row = MacroTableRow.from(1, action);

        assertEquals("2. Кнопка мыши", row.typeProperty().get());
        assertEquals("12 мс", row.delayProperty().get());
        assertEquals("Левая кнопка, действие: клик, точка: (400, 300)", row.detailsProperty().get());
    }
}
