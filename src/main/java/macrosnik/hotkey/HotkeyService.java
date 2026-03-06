package macrosnik.hotkey;

import macrosnik.play.MacroPlayer;
import macrosnik.play.PlayerState;
import macrosnik.settings.AppSettings;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

public class HotkeyService implements NativeKeyListener, AutoCloseable {

    private final MacroPlayer player;
    private final int pauseResumeKey;
    private final int stopKey;
    private final Runnable emergencyStop;


    private volatile boolean started = false;

    public HotkeyService(MacroPlayer player, AppSettings settings, Runnable emergencyStop) {
        this.player = player;
        this.emergencyStop = emergencyStop;
        this.pauseResumeKey = (settings.pauseResumeKey != 0) ? settings.pauseResumeKey : NativeKeyEvent.VC_F8;
        this.stopKey = (settings.stopKey != 0) ? settings.stopKey : NativeKeyEvent.VC_F12;
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
            e.printStackTrace();
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
