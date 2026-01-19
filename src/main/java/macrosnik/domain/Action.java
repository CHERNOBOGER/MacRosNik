package macrosnik.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DelayAction.class, name = "DELAY"),
        @JsonSubTypes.Type(value = MouseMovePathAction.class, name = "MOUSE_MOVE_PATH"),
        @JsonSubTypes.Type(value = MouseButtonAction.class, name = "MOUSE_BUTTON"),
        @JsonSubTypes.Type(value = KeyAction.class, name = "KEY")
})
public abstract class Action {
    public long delayBeforeMs;

    protected Action() { }

    protected Action(long delayBeforeMs) {
        this.delayBeforeMs = delayBeforeMs;
    }

    public abstract ActionType actionType();
}
