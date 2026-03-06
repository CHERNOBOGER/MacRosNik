package macrosnik.play;

import macrosnik.domain.*;
import macrosnik.domain.enums.KeyActionType;
import macrosnik.domain.enums.MouseButton;
import macrosnik.domain.enums.MouseButtonActionType;

import java.awt.*;
import java.awt.event.InputEvent;

public class ActionExecutor {

    public void execute(Action action, Robot robot, MacroPlayer player) throws InterruptedException {
        execute(action, new AwtRobotAdapter(robot), player);
    }

    void execute(Action action, RobotAdapter robot, MacroPlayer player) throws InterruptedException {
        if (action.delayBeforeMs > 0) {
            sleepInterruptible(action.delayBeforeMs, player);
        }
        player.checkpoint();

        if (action instanceof DelayAction delayAction) {
            executeDelay(delayAction, player);
            return;
        }

        if (action instanceof MouseMovePathAction mouseMovePathAction) {
            executeMouseMovePath(mouseMovePathAction, robot, player);
            return;
        }

        if (action instanceof MouseButtonAction mouseButtonAction) {
            executeMouseButton(mouseButtonAction, robot, player);
            return;
        }

        if (action instanceof KeyAction keyAction) {
            executeKey(keyAction, robot, player);
            return;
        }

        throw new IllegalArgumentException("Unknown action class: " + action.getClass());
    }

    private void executeDelay(DelayAction action, MacroPlayer player) throws InterruptedException {
        if (action.durationMs > 0) {
            sleepInterruptible(action.durationMs, player);
        }
    }

    private void executeMouseMovePath(MouseMovePathAction action, RobotAdapter robot, MacroPlayer player) throws InterruptedException {
        for (var point : action.points) {
            player.checkpoint();
            robot.mouseMove(point.x, point.y);
            if (point.dtMs > 0) {
                sleepInterruptible(point.dtMs, player);
            }
        }
    }

    private void executeMouseButton(MouseButtonAction action, RobotAdapter robot, MacroPlayer player) throws InterruptedException {
        player.checkpoint();
        int mask = toMask(action.button);

        switch (action.action) {
            case DOWN -> robot.mousePress(mask);
            case UP -> robot.mouseRelease(mask);
            case CLICK -> {
                robot.mousePress(mask);
                sleepInterruptible(30, player);
                robot.mouseRelease(mask);
            }
        }
    }

    private void executeKey(KeyAction action, RobotAdapter robot, MacroPlayer player) throws InterruptedException {
        player.checkpoint();
        validateKeyCode(action.keyCode);

        if (action.action == KeyActionType.DOWN) {
            robot.keyPress(action.keyCode);
        } else {
            robot.keyRelease(action.keyCode);
        }
    }

    private int toMask(MouseButton button) {
        return switch (button) {
            case LEFT -> InputEvent.BUTTON1_DOWN_MASK;
            case RIGHT -> InputEvent.BUTTON3_DOWN_MASK;
            case MIDDLE -> InputEvent.BUTTON2_DOWN_MASK;
        };
    }

    private void validateKeyCode(int keyCode) {
        if (keyCode <= 0 || keyCode == java.awt.event.KeyEvent.VK_UNDEFINED) {
            throw new IllegalArgumentException("Некорректный код клавиши: " + keyCode);
        }
    }

    private void sleepInterruptible(long ms, MacroPlayer player) throws InterruptedException {
        long remaining = ms;
        while (remaining > 0) {
            if (player.isStopRequested()) {
                throw new InterruptedException();
            }
            long chunk = Math.min(remaining, 20);
            Thread.sleep(chunk);
            remaining -= chunk;
        }
    }
}
