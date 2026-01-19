package macrosnik.domain;

import macrosnik.domain.enums.MouseButton;
import macrosnik.domain.enums.MouseButtonActionType;

public class MouseButtonAction extends Action {
    public MouseButton button = MouseButton.LEFT;
    public MouseButtonActionType action = MouseButtonActionType.CLICK;

    public MouseButtonAction() { }

    public MouseButtonAction(long delayBeforeMs, MouseButton button, MouseButtonActionType action) {
        super(delayBeforeMs);
        this.button = button;
        this.action = action;
    }

    @Override
    public ActionType actionType() {
        return ActionType.MOUSE_BUTTON;
    }
}
