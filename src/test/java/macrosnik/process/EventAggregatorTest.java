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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventAggregatorTest {
    private final EventAggregator aggregator = new EventAggregator(new AggregationConfig());

    @Test
    void aggregateReturnsEmptyMacroWhenNoEvents() {
        Macro macro = aggregator.aggregate(List.of());
        assertEquals("Recorded macro", macro.name);
        assertTrue(macro.actions.isEmpty());
    }

    @Test
    void aggregateBuildsSingleTargetPointAndFlushesItBeforeKeyboardEvent() {
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
        assertEquals(20, path.delayBeforeMs);
        assertEquals(1, path.points.size());
        assertEquals(18, path.points.getFirst().x);
        assertEquals(14, path.points.getFirst().y);
        assertEquals(0, path.points.getFirst().dtMs);

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

        MouseMovePathAction path = assertInstanceOf(MouseMovePathAction.class, macro.actions.get(0));
        assertEquals(20, path.delayBeforeMs);
        assertEquals(1, path.points.size());
        assertEquals(10, path.points.getFirst().x);
        assertEquals(10, path.points.getFirst().y);

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
    void aggregateCollapsesPlainMouseClickIntoSingleAction() {
        List<RawEvent> rawEvents = List.of(
                new RawMouseButton(1, true, 100, 200, 1_000_000),
                new RawMouseButton(1, false, 100, 200, 31_000_000)
        );

        Macro macro = aggregator.aggregate(rawEvents);
        assertEquals(1, macro.actions.size());

        MouseButtonAction click = assertInstanceOf(MouseButtonAction.class, macro.actions.get(0));
        assertEquals(MouseButton.LEFT, click.button);
        assertEquals(MouseButtonActionType.CLICK, click.action);
        assertEquals(0, click.delayBeforeMs);
    }

    @Test
    void aggregateKeepsMouseDownAndUpSeparateWhenSomethingHappensBetweenThem() {
        List<RawEvent> rawEvents = List.of(
                new RawMouseButton(1, true, 10, 10, 1_000_000),
                new RawMouseMove(20, 20, 11_000_000),
                new RawMouseMove(40, 40, 21_000_000),
                new RawMouseButton(1, false, 40, 40, 31_000_000)
        );

        Macro macro = aggregator.aggregate(rawEvents);
        assertEquals(3, macro.actions.size());

        MouseButtonAction down = assertInstanceOf(MouseButtonAction.class, macro.actions.get(0));
        MouseMovePathAction move = assertInstanceOf(MouseMovePathAction.class, macro.actions.get(1));
        MouseButtonAction up = assertInstanceOf(MouseButtonAction.class, macro.actions.get(2));

        assertEquals(MouseButtonActionType.DOWN, down.action);
        assertEquals(20, move.delayBeforeMs);
        assertEquals(40, move.points.getFirst().x);
        assertEquals(40, move.points.getFirst().y);
        assertEquals(MouseButtonActionType.UP, up.action);
        assertEquals(10, up.delayBeforeMs);
    }
}
