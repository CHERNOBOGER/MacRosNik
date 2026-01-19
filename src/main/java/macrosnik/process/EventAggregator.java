package macrosnik.process;

import macrosnik.domain.*;
import macrosnik.domain.enums.KeyActionType;
import macrosnik.domain.enums.MouseButton;
import macrosnik.domain.enums.MouseButtonActionType;
import macrosnik.record.*;

import java.util.ArrayList;
import java.util.List;

public class EventAggregator {

    private final AggregationConfig config;

    public EventAggregator(AggregationConfig config) {
        this.config = config;
    }

    public Macro aggregate(List<RawEvent> rawEvents) {
        Macro macro = new Macro("Recorded macro");

        if (rawEvents.isEmpty()) {
            return macro;
        }

        long lastTime = rawEvents.get(0).timeNanos();
        MouseMovePathBuilder pathBuilder = new MouseMovePathBuilder(config);

        for (RawEvent ev : rawEvents) {
            long delayMs = nanosToMs(ev.timeNanos() - lastTime);
            lastTime = ev.timeNanos();

            if (!(ev instanceof RawMouseMove)) {
                flushPathIfNeeded(pathBuilder, macro);
            }

            if (ev instanceof RawMouseMove mm) {
                pathBuilder.accept(mm, delayMs);
                continue;
            }

            if (ev instanceof RawMouseButton mb) {
                macro.actions.add(new MouseButtonAction(
                        delayMs,
                        toButton(mb.button()),
                        mb.pressed()
                                ? MouseButtonActionType.DOWN
                                : MouseButtonActionType.UP
                ));
                continue;
            }

            if (ev instanceof RawKey rk) {
                macro.actions.add(new KeyAction(
                        delayMs,
                        rk.keyCode(),
                        rk.pressed()
                                ? KeyActionType.DOWN
                                : KeyActionType.UP
                ));
                continue;
            }

            //if (ev instanceof RawMouseWheel mw) {}
        }

        flushPathIfNeeded(pathBuilder, macro);
        return macro;
    }

    private void flushPathIfNeeded(MouseMovePathBuilder builder, Macro macro) {
        MouseMovePathAction action = builder.buildAndReset();
        if (action != null) {
            macro.actions.add(action);
        }
    }

    private static long nanosToMs(long nanos) {
        return nanos <= 0 ? 0 : nanos / 1_000_000;
    }

    private static MouseButton toButton(int nativeButton) {
        return switch (nativeButton) {
            case 1 -> MouseButton.LEFT;
            case 2 -> MouseButton.MIDDLE;
            case 3 -> MouseButton.RIGHT;
            default -> MouseButton.LEFT;
        };
    }
}
