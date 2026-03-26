package macrosnik.play;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;

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

    @Override
    public void inputText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable previousContents = clipboard.getContents(null);
        try {
            clipboard.setContents(new StringSelection(text), null);

            int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
            pressModifiers(shortcutMask);
            robot.keyPress(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_V);
            releaseModifiers(shortcutMask);
            robot.delay(50);
        } finally {
            if (previousContents != null) {
                clipboard.setContents(previousContents, null);
            }
        }
    }

    private void pressModifiers(int mask) {
        if ((mask & java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0) {
            robot.keyPress(KeyEvent.VK_CONTROL);
        }
        if ((mask & java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0) {
            robot.keyPress(KeyEvent.VK_SHIFT);
        }
        if ((mask & java.awt.event.InputEvent.ALT_DOWN_MASK) != 0) {
            robot.keyPress(KeyEvent.VK_ALT);
        }
        if ((mask & java.awt.event.InputEvent.META_DOWN_MASK) != 0) {
            robot.keyPress(KeyEvent.VK_META);
        }
    }

    private void releaseModifiers(int mask) {
        if ((mask & java.awt.event.InputEvent.META_DOWN_MASK) != 0) {
            robot.keyRelease(KeyEvent.VK_META);
        }
        if ((mask & java.awt.event.InputEvent.ALT_DOWN_MASK) != 0) {
            robot.keyRelease(KeyEvent.VK_ALT);
        }
        if ((mask & java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0) {
            robot.keyRelease(KeyEvent.VK_SHIFT);
        }
        if ((mask & java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0) {
            robot.keyRelease(KeyEvent.VK_CONTROL);
        }
    }
}
