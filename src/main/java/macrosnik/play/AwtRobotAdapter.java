package macrosnik.play;

import java.awt.*;

public class AwtRobotAdapter implements RobotAdapter {
    private final Robot robot;

    public AwtRobotAdapter(Robot robot) {
        this.robot = robot;
    }

    @Override
    public void mouseMove(int x, int y) {
        robot.mouseMove(x, y);
    }

    @Override
    public void mousePress(int buttonsMask) {
        robot.mousePress(buttonsMask);
    }

    @Override
    public void mouseRelease(int buttonsMask) {
        robot.mouseRelease(buttonsMask);
    }

    @Override
    public void keyPress(int keyCode) {
        robot.keyPress(keyCode);
    }

    @Override
    public void keyRelease(int keyCode) {
        robot.keyRelease(keyCode);
    }
}
