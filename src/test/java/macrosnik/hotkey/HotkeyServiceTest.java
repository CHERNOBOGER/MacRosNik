package macrosnik.hotkey;

import macrosnik.play.MacroPlayer;
import macrosnik.play.PlayerState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HotkeyServiceTest {

    @Test
    void pauseHotkeyTogglesOnlyOnceUntilRelease() {
        TestMacroPlayer player = new TestMacroPlayer();
        player.state = PlayerState.PLAYING;
        HotkeyService service = new HotkeyService(player, 8, 12, () -> {});
        service.startForTest();

        service.handleKeyPressed(8);
        service.handleKeyPressed(8);
        service.handleKeyReleased(8);
        service.handleKeyPressed(8);

        assertEquals(List.of("pause", "resume"), player.calls);
    }

    @Test
    void stopHotkeyRunsEmergencyStopOnlyOnceUntilRelease() {
        TestMacroPlayer player = new TestMacroPlayer();
        List<String> calls = new ArrayList<>();
        HotkeyService service = new HotkeyService(player, 8, 12, () -> calls.add("stop"));
        service.startForTest();

        service.handleKeyPressed(12);
        service.handleKeyPressed(12);
        service.handleKeyReleased(12);
        service.handleKeyPressed(12);

        assertEquals(List.of("stop", "stop"), calls);
    }

    private static final class TestMacroPlayer extends MacroPlayer {
        private final List<String> calls = new ArrayList<>();
        private PlayerState state = PlayerState.IDLE;

        @Override
        public PlayerState getState() {
            return state;
        }

        @Override
        public void pause() {
            calls.add("pause");
            state = PlayerState.PAUSED;
        }

        @Override
        public void resume() {
            calls.add("resume");
            state = PlayerState.PLAYING;
        }
    }
}
