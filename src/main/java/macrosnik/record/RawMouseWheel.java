package macrosnik.record;

public record RawMouseWheel(
        int amount,
        long timeNanos
) implements RawEvent { }
