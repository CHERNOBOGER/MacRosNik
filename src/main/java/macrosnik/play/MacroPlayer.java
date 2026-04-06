package macrosnik.play;

import macrosnik.domain.Action;
import macrosnik.domain.Macro;

import java.awt.*;
import java.util.function.Consumer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MacroPlayer {

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "macro-player");
        t.setDaemon(true);
        return t;
    });

    private final Object lock = new Object();
    private volatile boolean stopRequested = false;
    private volatile boolean paused = false;
    private volatile PlayerState state = PlayerState.IDLE;
    private volatile Thread runningThread = null;
    private volatile Consumer<PlayerState> stateListener = state -> {};


    private final ActionExecutor actionExecutor = new ActionExecutor();

    public PlayerState getState() {
        return state;
    }

    public void setStateListener(Consumer<PlayerState> stateListener) {
        this.stateListener = stateListener == null ? state -> {} : stateListener;
    }

    public void play(Macro macro) {
        if (state == PlayerState.PLAYING || state == PlayerState.PAUSED) {
            return;
        }
        stopRequested = false;
        paused = false;
        setState(PlayerState.PLAYING);

        executor.submit(() -> {
            runningThread = Thread.currentThread();
            try {
                Robot robot = new Robot();
                robot.setAutoDelay(0);

                for (Action action : macro.actions) {
                    if (stopRequested) break;
                    waitIfPaused();

                    actionExecutor.execute(action, robot, this);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                runningThread = null;
                setState(stopRequested ? PlayerState.STOPPED : PlayerState.IDLE);
            }
        });

    }

    public void pause() {
        paused = true;
        setState(PlayerState.PAUSED);
    }

    public void resume() {
        synchronized (lock) {
            paused = false;
            setState(PlayerState.PLAYING);
            lock.notifyAll();
        }
    }

    public boolean isStopRequested() {
        return stopRequested;
    }

    public void checkpoint() throws InterruptedException {
        if (stopRequested) throw new InterruptedException();
        if (paused) {
            waitIfPaused();
        }
    }

    public void stop() {
        stopRequested = true;
        resume();

        Thread t = runningThread;
        if (t != null) {
            t.interrupt();
        }
        setState(PlayerState.STOPPED);
    }


    private void waitIfPaused() throws InterruptedException {
        synchronized (lock) {
            while (paused && !stopRequested) {
                lock.wait();
            }
        }
    }

    private void setState(PlayerState next) {
        state = next;
        try {
            stateListener.accept(next);
        } catch (Exception ignored) {
        }
    }
}
