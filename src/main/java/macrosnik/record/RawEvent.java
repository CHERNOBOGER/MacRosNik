package macrosnik.record;

public sealed interface RawEvent
        permits RawMouseMove, RawMouseButton, RawMouseWheel, RawKey {

    long timeNanos();
}
