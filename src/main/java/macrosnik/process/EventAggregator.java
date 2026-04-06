package macrosnik.process;

import macrosnik.domain.*;
import macrosnik.domain.enums.KeyActionType;
import macrosnik.domain.enums.MouseButton;
import macrosnik.domain.enums.MouseButtonActionType;
import macrosnik.record.*;
import macrosnik.util.NativeKeyCodeMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Set<Integer> pressedKeys = new HashSet<>();
        PendingMouseButton pendingMouseButton = null;
        PendingKey pendingKey = null;

        for (RawEvent ev : rawEvents) {
            long delayMs = nanosToMs(ev.timeNanos() - lastTime);
            lastTime = ev.timeNanos();

            if (pendingMouseButton != null && !isClickRelease(pendingMouseButton, ev)) {
                macro.actions.add(pendingMouseButton.toAction());
                pendingMouseButton = null;
            }
            if (pendingKey != null && !isKeyClickRelease(pendingKey, ev)) {
                macro.actions.add(pendingKey.toAction());
                pendingKey = null;
            }

            if (!(ev instanceof RawMouseMove)) {
                flushPathIfNeeded(pathBuilder, macro);
            }

            if (ev instanceof RawMouseMove mm) {
                pathBuilder.accept(mm, delayMs);
                continue;
            }

            if (ev instanceof RawMouseButton mb) {
                MouseButton button = toButton(mb.button());
                if (mb.pressed()) {
                    pendingMouseButton = new PendingMouseButton(delayMs, button);
                } else if (pendingMouseButton != null && pendingMouseButton.button() == button) {
                    macro.actions.add(new MouseButtonAction(
                            pendingMouseButton.delayBeforeMs(),
                            button,
                            MouseButtonActionType.CLICK
                    ));
                    pendingMouseButton = null;
                } else {
                    macro.actions.add(new MouseButtonAction(
                            delayMs,
                            button,
                            MouseButtonActionType.UP
                    ));
                }
                continue;
            }

            if (ev instanceof RawKey rk) {
                if (rk.pressed()) {
                    if (!pressedKeys.add(rk.keyCode())) {
                        continue;
                    }
                    pendingKey = new PendingKey(
                            delayMs,
                            rk.keyCode(),
                            NativeKeyCodeMapper.toAwt(rk.keyCode())
                    );
                    continue;
                } else {
                    pressedKeys.remove(rk.keyCode());
                    if (pendingKey != null && pendingKey.nativeKeyCode() == rk.keyCode()) {
                        macro.actions.add(new KeyAction(
                                pendingKey.delayBeforeMs(),
                                pendingKey.awtKeyCode(),
                                KeyActionType.CLICK
                        ));
                        pendingKey = null;
                        continue;
                    }
                }

                macro.actions.add(new KeyAction(
                        delayMs,
                        NativeKeyCodeMapper.toAwt(rk.keyCode()),
                        rk.pressed()
                                ? KeyActionType.DOWN
                                : KeyActionType.UP
                ));
                continue;
            }

            //if (ev instanceof RawMouseWheel mw) {}
        }

        flushPathIfNeeded(pathBuilder, macro);
        if (pendingMouseButton != null) {
            macro.actions.add(pendingMouseButton.toAction());
        }
        if (pendingKey != null) {
            macro.actions.add(pendingKey.toAction());
        }
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

    private static boolean isClickRelease(PendingMouseButton pendingMouseButton, RawEvent event) {
        return event instanceof RawMouseButton mouseButton
                && !mouseButton.pressed()
                && pendingMouseButton.button() == toButton(mouseButton.button());
    }

    private static boolean isKeyClickRelease(PendingKey pendingKey, RawEvent event) {
        return event instanceof RawKey key
                && !key.pressed()
                && pendingKey.nativeKeyCode() == key.keyCode();
    }

    private record PendingMouseButton(long delayBeforeMs, MouseButton button) {
        private MouseButtonAction toAction() {
            return new MouseButtonAction(delayBeforeMs, button, MouseButtonActionType.DOWN);
        }
    }

    private record PendingKey(long delayBeforeMs, int nativeKeyCode, int awtKeyCode) {
        private KeyAction toAction() {
            return new KeyAction(delayBeforeMs, awtKeyCode, KeyActionType.DOWN);
        }
    }
}
