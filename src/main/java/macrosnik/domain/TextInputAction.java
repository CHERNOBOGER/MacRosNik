package macrosnik.domain;

public class TextInputAction extends Action {
    public String text = "";

    public TextInputAction() { }

    public TextInputAction(long delayBeforeMs, String text) {
        super(delayBeforeMs);
        this.text = text == null ? "" : text;
    }

    @Override
    public ActionType actionType() {
        return ActionType.TEXT_INPUT;
    }
}
