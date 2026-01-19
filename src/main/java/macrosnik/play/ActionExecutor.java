package macrosnik.play;

import macrosnik.domain.*;
import macrosnik.domain.MouseMovePathAction.PathPoint;
import macrosnik.domain.enums.KeyActionType;
import macrosnik.domain.enums.MouseButton;
import macrosnik.domain.enums.MouseButtonActionType;

import java.awt.*;
import java.awt.event.InputEvent;

public class ActionExecutor {

    public void execute(Action action, Robot robot, MacroPlayer player) throws InterruptedException {
        // delayBefore
        if (action.delayBeforeMs > 0) {
            sleepInterruptible(action.delayBeforeMs, player);
        }
        if (player.isStopRequested()) throw new InterruptedException();

        if (action instanceof DelayAction da) {
            sleepInterruptible(da.durationMs, player);
            return;
        }

        if (action instanceof MouseMovePathAction mm) {
            for (var p : mm.points) {
                player.checkpoint();
                robot.mouseMove(p.x, p.y);
                if (p.dtMs > 0) sleepInterruptible(p.dtMs, player);
            }
            return;
        }

        if (action instanceof MouseButtonAction mba) {
            if (player.isStopRequested()) throw new InterruptedException();
            return;
        }

        if (action instanceof KeyAction ka) {
            if (player.isStopRequested()) throw new InterruptedException();
            return;
        }

        throw new IllegalArgumentException("Unknown action class: " + action.getClass());
    }


    private int toMask(MouseButton b) {
        return switch (b) {
            case LEFT -> InputEvent.BUTTON1_DOWN_MASK;
            case RIGHT -> InputEvent.BUTTON3_DOWN_MASK;
            case MIDDLE -> InputEvent.BUTTON2_DOWN_MASK;
        };
    }

    private void sleepInterruptible(long ms, MacroPlayer player) throws InterruptedException {
        long remaining = ms;
        while (remaining > 0) {
            if (player.isStopRequested()) throw new InterruptedException();
            long chunk = Math.min(remaining, 20); // 20мс — быстрая реакция
            Thread.sleep(chunk);
            remaining -= chunk;
        }
    }

}
