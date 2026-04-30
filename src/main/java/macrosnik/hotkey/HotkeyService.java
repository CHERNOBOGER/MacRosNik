package macrosnik.hotkey;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import macrosnik.play.MacroPlayer;
import macrosnik.play.PlayerState;

public class HotkeyService implements NativeKeyListener, AutoCloseable {
    public static final int DEFAULT_PAUSE_RESUME_KEY = NativeKeyEvent.VC_F8;
    public static final int DEFAULT_STOP_KEY = NativeKeyEvent.VC_F12;

    private final MacroPlayer player;
    private final int pauseResumeKey;
    private final int stopKey;
    private final Runnable emergencyStop;

    private volatile boolean started;
    private boolean pauseResumePressed;
    private boolean stopPressed;

    public HotkeyService(MacroPlayer player, int pauseResumeKey, int stopKey, Runnable emergencyStop) {
        this.player = player;
        this.emergencyStop = emergencyStop;
        this.pauseResumeKey = pauseResumeKey;
        this.stopKey = stopKey;
    }

    public synchronized void start() {
        if (started) {
            return;
        }

        try {
            if (!GlobalScreen.isNativeHookRegistered()) {
                GlobalScreen.registerNativeHook();
            }
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.addNativeKeyListener(this);
            started = true;
        } catch (NativeHookException e) {
            throw new RuntimeException("Не удалось зарегистрировать глобальный перехват клавиатуры", e);
        }
    }

    @Override
    public synchronized void nativeKeyPressed(NativeKeyEvent event) {
        handleKeyPressed(event.getKeyCode());
    }

    @Override
    public synchronized void nativeKeyReleased(NativeKeyEvent event) {
        handleKeyReleased(event.getKeyCode());
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent event) {
    }

    synchronized void handleKeyPressed(int keyCode) {
        if (!started) {
            return;
        }

        if (keyCode == stopKey) {
            if (!stopPressed) {
                stopPressed = true;
                emergencyStop.run();
            }
            return;
        }

        if (keyCode == pauseResumeKey && !pauseResumePressed) {
            pauseResumePressed = true;
            togglePauseResume();
        }
    }

    synchronized void handleKeyReleased(int keyCode) {
        if (keyCode == stopKey) {
            stopPressed = false;
        }
        if (keyCode == pauseResumeKey) {
            pauseResumePressed = false;
        }
    }

    synchronized void startForTest() {
        started = true;
        pauseResumePressed = false;
        stopPressed = false;
    }

    private void togglePauseResume() {
        if (player.getState() == PlayerState.PAUSED) {
            player.resume();
        } else if (player.getState() == PlayerState.PLAYING) {
            player.pause();
        }
    }

    @Override
    public synchronized void close() {
        if (!started) {
            return;
        }
        try {
            GlobalScreen.removeNativeKeyListener(this);
        } catch (Throwable ignored) {
        } finally {
            started = false;
            pauseResumePressed = false;
            stopPressed = false;
        }
    }
}
