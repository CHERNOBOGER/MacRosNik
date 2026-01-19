package macrosnik.record;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RecorderService
        implements NativeKeyListener,
        NativeMouseInputListener,
        NativeMouseWheelListener {

    private final List<RawEvent> buffer = new ArrayList<>();
    private volatile boolean recording = false;
    private volatile Set<Integer> ignoredKeyCodes = Set.of();

    public void setIgnoredKeyCodes(Set<Integer> ignoredKeyCodes) {
        this.ignoredKeyCodes = (ignoredKeyCodes == null) ? Set.of() : Set.copyOf(ignoredKeyCodes);
    }

    private boolean isIgnoredKey(int keyCode) {
        return ignoredKeyCodes.contains(keyCode);
    }

    public void start() {
        buffer.clear();
        recording = true;

        try {
            GlobalScreen.addNativeKeyListener(this);
            GlobalScreen.addNativeMouseListener(this);
            GlobalScreen.addNativeMouseMotionListener(this);
            GlobalScreen.addNativeMouseWheelListener(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<RawEvent> stop() {
        recording = false;

        GlobalScreen.removeNativeKeyListener(this);
        GlobalScreen.removeNativeMouseListener(this);
        GlobalScreen.removeNativeMouseMotionListener(this);
        GlobalScreen.removeNativeMouseWheelListener(this);

        return List.copyOf(buffer);
    }

    private long now() {
        return System.nanoTime();
    }

    /* ===== keyboard ===== */

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (!recording) return;
        if (isIgnoredKey(e.getKeyCode())) return;
        buffer.add(new RawKey(e.getKeyCode(), true, now()));
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        if (!recording) return;
        if (isIgnoredKey(e.getKeyCode())) return;
        buffer.add(new RawKey(e.getKeyCode(), false, now()));
    }

    /* ===== mouse ===== */

    @Override
    public void nativeMouseMoved(NativeMouseEvent e) {
        if (!recording) return;
        buffer.add(new RawMouseMove(e.getX(), e.getY(), now()));
    }

    @Override
    public void nativeMousePressed(NativeMouseEvent e) {
        if (!recording) return;
        buffer.add(new RawMouseButton(
                e.getButton(), true, e.getX(), e.getY(), now()));
    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent e) {
        if (!recording) return;
        buffer.add(new RawMouseButton(
                e.getButton(), false, e.getX(), e.getY(), now()));
    }

    @Override
    public void nativeMouseWheelMoved(NativeMouseWheelEvent e) {
        if (!recording) return;
        buffer.add(new RawMouseWheel(e.getWheelRotation(), now()));
    }

    public boolean isRecording() {
        return recording;
    }

    @Override public void nativeMouseClicked(NativeMouseEvent e) {}
    @Override public void nativeMouseDragged(NativeMouseEvent e) {}
}
