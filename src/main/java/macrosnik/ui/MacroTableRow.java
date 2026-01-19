package macrosnik.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import macrosnik.domain.*;
import macrosnik.domain.MouseMovePathAction.PathPoint;

public class MacroTableRow {
    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty delay = new SimpleStringProperty();
    private final StringProperty details = new SimpleStringProperty();

    public static MacroTableRow from(int index, Action action) {
        MacroTableRow row = new MacroTableRow();
        row.type.set(index + ". " + action.actionType());
        row.delay.set(String.valueOf(action.delayBeforeMs));

        if (action instanceof DelayAction da) {
            row.details.set("duration=" + da.durationMs + "ms");
        } else if (action instanceof MouseMovePathAction mm) {
            int n = mm.points.size();
            String first = n > 0 ? fmt(mm.points.get(0)) : "-";
            String last  = n > 0 ? fmt(mm.points.get(n - 1)) : "-";
            row.details.set("points=" + n + ", from " + first + " to " + last + ", mode=" + mm.coordinateMode);
        } else if (action instanceof MouseButtonAction mba) {
            row.details.set(mba.button + " " + mba.action);
        } else if (action instanceof KeyAction ka) {
            row.details.set("keyCode=" + ka.keyCode + " " + ka.action);
        } else {
            row.details.set(action.getClass().getSimpleName());
        }
        return row;
    }

    private static String fmt(PathPoint p) {
        return "(" + p.x + "," + p.y + ") dt=" + p.dtMs;
    }

    public StringProperty typeProperty() { return type; }
    public StringProperty delayProperty() { return delay; }
    public StringProperty detailsProperty() { return details; }
}
