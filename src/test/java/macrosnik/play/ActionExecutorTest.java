package macrosnik.play;

import macrosnik.domain.KeyAction;
import macrosnik.domain.MouseButtonAction;
import macrosnik.domain.enums.KeyActionType;
import macrosnik.domain.enums.MouseButton;
import macrosnik.domain.enums.MouseButtonActionType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActionExecutorTest {

    @Test
    void executesMouseClickAndKeyPress() throws Exception {
        ActionExecutor executor = new ActionExecutor();
        MacroPlayer player = new MacroPlayer();
        FakeRobot robot = new FakeRobot();

        executor.execute(new MouseButtonAction(0, MouseButton.LEFT, MouseButtonActionType.CLICK), robot, player);
        executor.execute(new KeyAction(0, 65, KeyActionType.DOWN), robot, player);
        executor.execute(new KeyAction(0, 65, KeyActionType.UP), robot, player);

        assertEquals(List.of("mousePress:1024", "mouseRelease:1024", "keyPress:65", "keyRelease:65"), robot.calls);
    }

    private static class FakeRobot implements RobotAdapter {
        private final List<String> calls = new ArrayList<>();

        @Override
        public void mouseMove(int x, int y) {
            calls.add("mouseMove:" + x + "," + y);
        }

        @Override
        public void mousePress(int buttonsMask) {
            calls.add("mousePress:" + buttonsMask);
        }

        @Override
        public void mouseRelease(int buttonsMask) {
            calls.add("mouseRelease:" + buttonsMask);
        }

        @Override
        public void keyPress(int keyCode) {
            calls.add("keyPress:" + keyCode);
        }

        @Override
        public void keyRelease(int keyCode) {
            calls.add("keyRelease:" + keyCode);
        }
    }
}
