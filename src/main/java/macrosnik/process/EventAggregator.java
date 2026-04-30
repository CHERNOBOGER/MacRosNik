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
import macrosnik.util.NativeKeyCodeMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EventAggregator {

    public Macro aggregate(List<RawEvent> rawEvents) {
        Macro macro = new Macro("Recorded macro");

        if (rawEvents.isEmpty()) {
            return macro;
        }

        long lastTime = rawEvents.getFirst().timeNanos();
        long carriedDelayMs = 0;
        Set<Integer> pressedKeys = new HashSet<>();
        PendingMouseButton pendingMouseButton = null;
        PendingKey pendingKey = null;

        for (RawEvent event : rawEvents) {
            long eventDelayMs = nanosToMs(event.timeNanos() - lastTime);
            lastTime = event.timeNanos();

            long effectiveDelayMs = carriedDelayMs + eventDelayMs;
            carriedDelayMs = 0;

            if (pendingMouseButton != null && shouldFlushMouseButtonBefore(pendingMouseButton, event)) {
                macro.actions.add(pendingMouseButton.toDownAction());
                effectiveDelayMs += pendingMouseButton.heldDurationMs();
                pendingMouseButton = null;
            }

            if (pendingKey != null && shouldFlushKeyBefore(pendingKey, event)) {
                macro.actions.add(pendingKey.toDownAction());
                effectiveDelayMs += pendingKey.heldDurationMs();
                pendingKey = null;
            }

            if (event instanceof RawMouseMove mouseMove) {
                if (pendingMouseButton != null) {
                    pendingMouseButton = pendingMouseButton.recordMovement(effectiveDelayMs, mouseMove.x(), mouseMove.y());
                } else {
                    carriedDelayMs += effectiveDelayMs;
                }
                continue;
            }

            if (event instanceof RawMouseButton mouseButton) {
                MouseButton button = toButton(mouseButton.button());
                if (mouseButton.pressed()) {
                    pendingMouseButton = new PendingMouseButton(
                            effectiveDelayMs,
                            button,
                            mouseButton.x(),
                            mouseButton.y(),
                            0,
                            false
                    );
                    continue;
                }

                if (pendingMouseButton != null && pendingMouseButton.button() == button) {
                    if (pendingMouseButton.isClickRelease(mouseButton.x(), mouseButton.y())) {
                        macro.actions.add(pendingMouseButton.toClickAction());
                    } else {
                        macro.actions.add(pendingMouseButton.toDownAction());
                        macro.actions.add(pendingMouseButton.toUpAction(
                                pendingMouseButton.heldDurationMs() + effectiveDelayMs,
                                mouseButton.x(),
                                mouseButton.y()
                        ));
                    }
                    pendingMouseButton = null;
                    continue;
                }

                macro.actions.add(new MouseButtonAction(
                        effectiveDelayMs,
                        button,
                        MouseButtonActionType.UP,
                        mouseButton.x(),
                        mouseButton.y()
                ));
                continue;
            }

            if (event instanceof RawKey key) {
                if (key.pressed()) {
                    if (pendingKey != null && pendingKey.nativeKeyCode() == key.keyCode()) {
                        pendingKey = pendingKey.withHeldDuration(effectiveDelayMs);
                        continue;
                    }
                    if (!pressedKeys.add(key.keyCode())) {
                        carriedDelayMs += effectiveDelayMs;
                        continue;
                    }
                    pendingKey = new PendingKey(
                            effectiveDelayMs,
                            key.keyCode(),
                            NativeKeyCodeMapper.toAwt(key.keyCode()),
                            0
                    );
                    continue;
                }

                pressedKeys.remove(key.keyCode());
                if (pendingKey != null && pendingKey.nativeKeyCode() == key.keyCode()) {
                    macro.actions.add(pendingKey.toClickAction());
                    pendingKey = null;
                    continue;
                }

                macro.actions.add(new KeyAction(
                        effectiveDelayMs,
                        NativeKeyCodeMapper.toAwt(key.keyCode()),
                        KeyActionType.UP
                ));
                continue;
            }

            if (event instanceof RawMouseWheel) {
                carriedDelayMs += effectiveDelayMs;
            }
        }

        if (pendingMouseButton != null) {
            macro.actions.add(pendingMouseButton.toDownAction());
        }
        if (pendingKey != null) {
            macro.actions.add(pendingKey.toDownAction());
        }
        return macro;
    }

    private static boolean shouldFlushMouseButtonBefore(PendingMouseButton pendingMouseButton, RawEvent event) {
        return !(event instanceof RawMouseMove)
                && !isMatchingMouseRelease(pendingMouseButton, event);
    }

    private static boolean shouldFlushKeyBefore(PendingKey pendingKey, RawEvent event) {
        return !(event instanceof RawKey key) || key.keyCode() != pendingKey.nativeKeyCode();
    }

    private static boolean isMatchingMouseRelease(PendingMouseButton pendingMouseButton, RawEvent event) {
        return event instanceof RawMouseButton mouseButton
                && !mouseButton.pressed()
                && pendingMouseButton.button() == toButton(mouseButton.button());
    }

    private static boolean isMatchingKeyRelease(PendingKey pendingKey, RawEvent event) {
        return event instanceof RawKey key
                && !key.pressed()
                && pendingKey.nativeKeyCode() == key.keyCode();
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

    private record PendingMouseButton(long delayBeforeMs,
                                      MouseButton button,
                                      int pressX,
                                      int pressY,
                                      long heldDurationMs,
                                      boolean moved) {
        private PendingMouseButton recordMovement(long delayMs, int x, int y) {
            boolean nextMoved = moved || x != pressX || y != pressY;
            return new PendingMouseButton(
                    delayBeforeMs,
                    button,
                    pressX,
                    pressY,
                    heldDurationMs + delayMs,
                    nextMoved
            );
        }

        private boolean isClickRelease(int releaseX, int releaseY) {
            return !moved && pressX == releaseX && pressY == releaseY;
        }

        private MouseButtonAction toDownAction() {
            return new MouseButtonAction(delayBeforeMs, button, MouseButtonActionType.DOWN, pressX, pressY);
        }

        private MouseButtonAction toClickAction() {
            return new MouseButtonAction(delayBeforeMs, button, MouseButtonActionType.CLICK, pressX, pressY);
        }

        private MouseButtonAction toUpAction(long delayBeforeMs, int releaseX, int releaseY) {
            return new MouseButtonAction(delayBeforeMs, button, MouseButtonActionType.UP, releaseX, releaseY);
        }
    }

    private record PendingKey(long delayBeforeMs, int nativeKeyCode, int awtKeyCode, long heldDurationMs) {
        private PendingKey withHeldDuration(long delayMs) {
            return new PendingKey(delayBeforeMs, nativeKeyCode, awtKeyCode, heldDurationMs + delayMs);
        }

        private KeyAction toDownAction() {
            return new KeyAction(delayBeforeMs, awtKeyCode, KeyActionType.DOWN);
        }

        private KeyAction toClickAction() {
            return new KeyAction(delayBeforeMs, awtKeyCode, KeyActionType.CLICK);
        }
    }
}
