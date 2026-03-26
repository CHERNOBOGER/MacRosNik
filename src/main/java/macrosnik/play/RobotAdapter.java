package macrosnik.play;

public interface RobotAdapter {
    void mouseMove(int x, int y);
    void mousePress(int buttonsMask);
    void mouseRelease(int buttonsMask);
    void keyPress(int keyCode);
    void keyRelease(int keyCode);
    void inputText(String text);
}
