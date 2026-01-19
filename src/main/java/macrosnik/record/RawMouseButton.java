package macrosnik.record;

public record RawMouseButton(
        int button,          // NativeMouseEvent.BUTTON1 / 2 / 3
        boolean pressed,
        int x,
        int y,
        long timeNanos
) implements RawEvent { }
