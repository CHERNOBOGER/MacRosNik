package macrosnik.domain;

public class DelayAction extends Action {
    public long durationMs;

    public DelayAction() { }

    public DelayAction(long delayBeforeMs, long durationMs) {
        super(delayBeforeMs);
        this.durationMs = durationMs;
    }

    @Override
    public ActionType actionType() {
        return ActionType.DELAY;
    }
}
