package macrosnik.domain;

import macrosnik.domain.enums.MouseButton;
import macrosnik.domain.enums.MouseButtonActionType;

public class MouseButtonAction extends Action {
    public MouseButton button = MouseButton.LEFT;
    public MouseButtonActionType action = MouseButtonActionType.CLICK;
    public Integer x;
    public Integer y;

    public MouseButtonAction() { }

    public MouseButtonAction(long delayBeforeMs, MouseButton button, MouseButtonActionType action) {
        this(delayBeforeMs, button, action, null, null);
    }

    public MouseButtonAction(long delayBeforeMs, MouseButton button, MouseButtonActionType action, Integer x, Integer y) {
        super(delayBeforeMs);
        this.button = button;
        this.action = action;
        this.x = x;
        this.y = y;
    }

    @Override
    public ActionType actionType() {
        return ActionType.MOUSE_BUTTON;
    }

    public boolean hasCoordinates() {
        return x != null && y != null;
    }
}
