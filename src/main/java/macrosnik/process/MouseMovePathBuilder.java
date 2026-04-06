package macrosnik.process;

import macrosnik.domain.MouseMovePathAction;
import macrosnik.domain.MouseMovePathAction.PathPoint;
import macrosnik.record.RawMouseMove;

import java.util.ArrayList;
import java.util.List;

class MouseMovePathBuilder {

    private final AggregationConfig config;

    private final List<PathPoint> points = new ArrayList<>();
    private long accumulatedDelayMs = 0;

    private Integer lastX = null;
    private Integer lastY = null;
    private long lastAcceptedTimeMs = 0;

    MouseMovePathBuilder(AggregationConfig config) {
        this.config = config;
    }

    void accept(RawMouseMove ev, long delayMs) {
        accumulatedDelayMs += delayMs;

        if (lastX == null) {
            addPoint(ev.x(), ev.y(), 0);
            return;
        }

        long nowMs = lastAcceptedTimeMs + accumulatedDelayMs;
        long dt = nowMs - lastAcceptedTimeMs;

        int dx = ev.x() - lastX;
        int dy = ev.y() - lastY;
        double dist = Math.hypot(dx, dy);

        if (dt < config.minMouseMoveIntervalMs && dist < config.minMouseMoveDistancePx) {
            return;
        }

        addPoint(ev.x(), ev.y(), (int) dt);
        accumulatedDelayMs = 0;
    }

    MouseMovePathAction buildAndReset() {
        if (points.isEmpty()) {
            reset();
            return null;
        }

        MouseMovePathAction action = new MouseMovePathAction();
        action.delayBeforeMs = totalPathDurationMs();

        PathPoint lastPoint = points.getLast();
        action.points.add(new PathPoint(lastPoint.x, lastPoint.y, 0));

        reset();
        return action;
    }

    private void addPoint(int x, int y, int dtMs) {
        points.add(new PathPoint(x, y, dtMs));
        lastX = x;
        lastY = y;
        lastAcceptedTimeMs += dtMs;
    }

    private void reset() {
        points.clear();
        accumulatedDelayMs = 0;
        lastX = null;
        lastY = null;
        lastAcceptedTimeMs = 0;
    }

    private long totalPathDurationMs() {
        long total = 0;
        for (PathPoint point : points) {
            total += point.dtMs;
        }
        return total;
    }
}
