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


    private volatile boolean started = false;

    public HotkeyService(MacroPlayer player, int pauseResumeKey, int stopKey, Runnable emergencyStop) {
        this.player = player;
        this.emergencyStop = emergencyStop;
        this.pauseResumeKey = pauseResumeKey;
        this.stopKey = stopKey;
    }


    public void start() {
        if (started) return;

        try {
            System.out.println("Registering native hook...");
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            started = true;
            System.out.println("Native hook registered OK");
        } catch (NativeHookException e) {
            throw new RuntimeException("Не удалось зарегистрировать глобальный перехват клавиатуры", e);
        }
    }


    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        System.out.println("GLOBAL KEY: " + NativeKeyEvent.getKeyText(e.getKeyCode()));

        if (e.getKeyCode() == stopKey) {
            System.out.println("EMERGENCY STOP pressed");
            emergencyStop.run();
            return;
        }

        if (e.getKeyCode() == pauseResumeKey) {
            System.out.println("PAUSE pressed");
            if (player.getState() == PlayerState.PAUSED) {
                player.resume();
            } else if (player.getState() == PlayerState.PLAYING) {
                player.pause();
            }
        }
    }



    @Override public void nativeKeyReleased(NativeKeyEvent e) { }
    @Override public void nativeKeyTyped(NativeKeyEvent e) { }

    @Override
    public void close() {
        if (!started) return;
        try {
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException ignored) {
        } finally {
            started = false;
        }
    }
}
