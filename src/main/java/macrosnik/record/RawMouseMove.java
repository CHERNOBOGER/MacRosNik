package macrosnik.record;

public record RawMouseMove(
        int x,
        int y,
        long timeNanos
) implements RawEvent { }
