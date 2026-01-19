package macrosnik.domain;

import java.util.ArrayList;
import java.util.List;

public class MouseMovePathAction extends Action {

    public CoordinateMode coordinateMode = CoordinateMode.SCREEN_ABSOLUTE;
    public List<PathPoint> points = new ArrayList<>();

    public MouseMovePathAction() { }

    public MouseMovePathAction(long delayBeforeMs) {
        super(delayBeforeMs);
    }

    @Override
    public ActionType actionType() {
        return ActionType.MOUSE_MOVE_PATH;
    }

    public static class PathPoint {
        public int x;
        public int y;
        public int dtMs;

        public PathPoint() { }

        public PathPoint(int x, int y, int dtMs) {
            this.x = x;
            this.y = y;
            this.dtMs = dtMs;
        }
    }
}
