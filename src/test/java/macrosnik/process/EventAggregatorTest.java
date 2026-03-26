package macrosnik.process;

import macrosnik.domain.KeyAction;
import macrosnik.domain.Macro;
import macrosnik.domain.MouseButtonAction;
import macrosnik.domain.MouseMovePathAction;
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

import static org.junit.jupiter.api.Assertions.*;

class EventAggregatorTest {
    private final EventAggregator aggregator = new EventAggregator(new AggregationConfig());

    @Test
    void aggregateReturnsEmptyMacroWhenNoEvents() {
        Macro macro = aggregator.aggregate(List.of());
        assertEquals("Recorded macro", macro.name);
        assertTrue(macro.actions.isEmpty());
    }

    @Test
    void aggregateBuildsMousePathAndFlushesItBeforeKeyboardEvent() {
        List<RawEvent> rawEvents = List.of(
                new RawMouseMove(10, 10, 1_000_000),
                new RawMouseMove(18, 14, 21_000_000),
                new RawKey(30, true, 41_000_000)
        );

        Macro macro = aggregator.aggregate(rawEvents);
        assertEquals(2, macro.actions.size());
        assertInstanceOf(MouseMovePathAction.class, macro.actions.get(0));
        assertInstanceOf(KeyAction.class, macro.actions.get(1));

        MouseMovePathAction path = (MouseMovePathAction) macro.actions.get(0);
        assertEquals(2, path.points.size());
        assertEquals(20, path.points.get(1).dtMs);

        KeyAction keyAction = (KeyAction) macro.actions.get(1);
        assertEquals(20, keyAction.delayBeforeMs);
        assertEquals(KeyActionType.DOWN, keyAction.action);
    }

    @Test
    void aggregateMapsMouseButtonsAndIgnoresWheel() {
        List<RawEvent> rawEvents = List.of(
                new RawMouseMove(1, 1, 0),
                new RawMouseMove(10, 10, 20_000_000),
                new RawMouseWheel(-1, 25_000_000),
                new RawMouseButton(2, true, 10, 10, 35_000_000),
                new RawMouseButton(3, false, 10, 10, 45_000_000)
        );

        Macro macro = aggregator.aggregate(rawEvents);
        assertEquals(3, macro.actions.size());
        MouseButtonAction middleDown = (MouseButtonAction) macro.actions.get(1);
        MouseButtonAction rightUp = (MouseButtonAction) macro.actions.get(2);
        assertEquals(MouseButton.MIDDLE, middleDown.button);
        assertEquals(MouseButtonActionType.DOWN, middleDown.action);
        assertEquals(MouseButton.RIGHT, rightUp.button);
        assertEquals(MouseButtonActionType.UP, rightUp.action);
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
        assertEquals(2, macro.actions.size());

        KeyAction keyDown = assertInstanceOf(KeyAction.class, macro.actions.get(0));
        KeyAction keyUp = assertInstanceOf(KeyAction.class, macro.actions.get(1));
        assertEquals(KeyActionType.DOWN, keyDown.action);
        assertEquals(KeyActionType.UP, keyUp.action);
    }
}
