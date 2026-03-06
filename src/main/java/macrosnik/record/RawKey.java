package macrosnik.record;

public record RawKey(
        int keyCode,         // NativeKeyEvent.VC_*
        boolean pressed,
        long timeNanos
) implements RawEvent { }
