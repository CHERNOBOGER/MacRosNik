package macrosnik.process;

import macrosnik.domain.KeyAction;
import macrosnik.domain.Macro;
import macrosnik.domain.MouseButtonAction;
import macrosnik.domain.enums.KeyActionType;
import macrosnik.domain.enums.MouseButton;
import macrosnik.domain.enums.MouseButtonActionType;
import macrosnik.record.RawEvent;
import macrosnik.record.RawKey;
import macrosnik.record.RawMouseButton;
import macrosnik.record.RawMouseMove;
import macrosnik.record.RawMouseWheel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventAggregatorTest {
    private final EventAggregator aggregator = new EventAggregator();

    @Test
    void aggregateReturnsEmptyMacroWhenNoEvents() {
        Macro macro = aggregator.aggregate(List.of());
        assertEquals("Recorded macro", macro.name);
        assertTrue(macro.actions.isEmpty());
    }

    @Test
    void aggregateDropsStandaloneMouseMovementAndCarriesItsDelayForward() {
        List<RawEvent> rawEvents = List.of(
                new RawMouseMove(10, 10, 1_000_000),
                new RawMouseMove(18, 14, 21_000_000),
                new RawKey(30, true, 41_000_000)
        );

        Macro macro = aggregator.aggregate(rawEvents);
        assertEquals(1, macro.actions.size());

        KeyAction keyAction = assertInstanceOf(KeyAction.class, macro.actions.getFirst());
        assertEquals(40, keyAction.delayBeforeMs);
        assertEquals(KeyActionType.DOWN, keyAction.action);
    }

    @Test
    void aggregateMapsMouseButtonsAndIgnoresWheelWithoutCreatingMoveActions() {
        List<RawEvent> rawEvents = List.of(
                new RawMouseMove(1, 1, 0),
                new RawMouseMove(10, 10, 20_000_000),
                new RawMouseWheel(-1, 25_000_000),
                new RawMouseButton(2, true, 10, 10, 35_000_000),
                new RawMouseButton(3, false, 10, 10, 45_000_000)
        );

        Macro macro = aggregator.aggregate(rawEvents);
        assertEquals(2, macro.actions.size());

        MouseButtonAction middleDown = assertInstanceOf(MouseButtonAction.class, macro.actions.get(0));
        MouseButtonAction rightUp = assertInstanceOf(MouseButtonAction.class, macro.actions.get(1));
        assertEquals(MouseButton.MIDDLE, middleDown.button);
        assertEquals(MouseButtonActionType.DOWN, middleDown.action);
        assertTrue(middleDown.hasCoordinates());
        assertEquals(35, middleDown.delayBeforeMs);
        assertEquals(MouseButton.RIGHT, rightUp.button);
        assertEquals(MouseButtonActionType.UP, rightUp.action);
        assertTrue(rightUp.hasCoordinates());
        assertEquals(10, rightUp.delayBeforeMs);
    }

    @Test
    void aggregateIgnoresRepeatedKeyPressedEventsWhileKeyIsHeld() {
        List<RawEvent> rawEvents = List.of(
                new RawKey(30, true, 1_000_000),
                new RawKey(30, true, 5_000_000),
                new RawKey(30, true, 10_000_000),
                new RawKey(30, false, 20_000_000)
        );

        Macro macro = aggregator.aggregate(rawEvents);
        assertEquals(1, macro.actions.size());

        KeyAction keyClick = assertInstanceOf(KeyAction.class, macro.actions.getFirst());
        assertEquals(KeyActionType.CLICK, keyClick.action);
    }

    @Test
    void aggregateCollapsesPlainKeyPressIntoSingleClickAction() {
        List<RawEvent> rawEvents = List.of(
                new RawKey(21, true, 1_000_000),
                new RawKey(21, false, 86_000_000)
        );

        Macro macro = aggregator.aggregate(rawEvents);
        assertEquals(1, macro.actions.size());

        KeyAction keyClick = assertInstanceOf(KeyAction.class, macro.actions.get(0));
        assertEquals(KeyActionType.CLICK, keyClick.action);
        assertEquals(0, keyClick.delayBeforeMs);
    }

    @Test
    void aggregateCollapsesPlainMouseClickIntoSingleCoordinateAction() {
        List<RawEvent> rawEvents = List.of(
                new RawMouseButton(1, true, 100, 200, 1_000_000),
                new RawMouseButton(1, false, 100, 200, 31_000_000)
        );

        Macro macro = aggregator.aggregate(rawEvents);
        assertEquals(1, macro.actions.size());

        MouseButtonAction click = assertInstanceOf(MouseButtonAction.class, macro.actions.getFirst());
        assertEquals(MouseButton.LEFT, click.button);
        assertEquals(MouseButtonActionType.CLICK, click.action);
        assertEquals(0, click.delayBeforeMs);
        assertEquals(100, click.x);
        assertEquals(200, click.y);
    }

    @Test
    void aggregateConvertsDragIntoDownAndUpWithCoordinates() {
        List<RawEvent> rawEvents = List.of(
                new RawMouseButton(1, true, 10, 10, 1_000_000),
                new RawMouseMove(20, 20, 11_000_000),
                new RawMouseMove(40, 40, 21_000_000),
                new RawMouseButton(1, false, 40, 40, 31_000_000)
        );

        Macro macro = aggregator.aggregate(rawEvents);
        assertEquals(2, macro.actions.size());

        MouseButtonAction down = assertInstanceOf(MouseButtonAction.class, macro.actions.get(0));
        MouseButtonAction up = assertInstanceOf(MouseButtonAction.class, macro.actions.get(1));

        assertEquals(MouseButtonActionType.DOWN, down.action);
        assertEquals(10, down.x);
        assertEquals(10, down.y);
        assertEquals(MouseButtonActionType.UP, up.action);
        assertEquals(40, up.x);
        assertEquals(40, up.y);
        assertEquals(30, up.delayBeforeMs);
    }
}
