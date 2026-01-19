package macrosnik.domain;

import macrosnik.domain.enums.KeyActionType;

public class KeyAction extends Action {
    public int keyCode;
    public KeyActionType action = KeyActionType.DOWN;

    public KeyAction() { }

    public KeyAction(long delayBeforeMs, int keyCode, KeyActionType action) {
        super(delayBeforeMs);
        this.keyCode = keyCode;
        this.action = action;
    }

    @Override
    public ActionType actionType() {
        return ActionType.KEY;
    }
}
