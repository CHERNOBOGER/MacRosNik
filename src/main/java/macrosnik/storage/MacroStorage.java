package macrosnik.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import macrosnik.domain.Action;
import macrosnik.domain.KeyAction;
import macrosnik.domain.Macro;
import macrosnik.domain.MouseButtonAction;
import macrosnik.domain.MouseMovePathAction;
import macrosnik.util.NativeKeyCodeMapper;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MacroStorage {
    private final ObjectMapper mapper = ObjectMapperFactory.create();

    public void save(Path path, Macro macro) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), macro);
    }

    public Macro load(Path path) throws IOException {
        Macro macro = mapper.readValue(path.toFile(), Macro.class);
        normalizeMouseCoordinates(macro);
        normalizeLegacyKeyCodes(macro);
        return macro;
    }

    private void normalizeMouseCoordinates(Macro macro) {
        if (macro == null || macro.actions == null || macro.actions.isEmpty()) {
            return;
        }

        List<Action> normalized = new ArrayList<>();
        for (int i = 0; i < macro.actions.size(); i++) {
            Action current = macro.actions.get(i);
            if (canCollapseLegacyMouseCommand(macro.actions, i)) {
                MouseMovePathAction moveAction = (MouseMovePathAction) current;
                MouseMovePathAction.PathPoint point = moveAction.points.getFirst();
                MouseButtonAction mouseButtonAction = (MouseButtonAction) macro.actions.get(i + 1);
                normalized.add(new MouseButtonAction(
                        moveAction.delayBeforeMs,
                        mouseButtonAction.button,
                        mouseButtonAction.action,
                        point.x,
                        point.y
                ));
                i++;
                continue;
            }
            normalized.add(current);
        }
        macro.actions = normalized;
    }

    private void normalizeLegacyKeyCodes(Macro macro) {
        if (macro == null || macro.actions == null) {
            return;
        }
        for (Action action : macro.actions) {
            if (action instanceof KeyAction keyAction && !isLikelyAwtKeyCode(keyAction.keyCode)) {
                int mapped = NativeKeyCodeMapper.toAwt(keyAction.keyCode);
                if (mapped != KeyEvent.VK_UNDEFINED) {
                    keyAction.keyCode = mapped;
                }
            }
        }
    }

    private boolean isLikelyAwtKeyCode(int keyCode) {
        if (keyCode <= 0 || keyCode == KeyEvent.VK_UNDEFINED) {
            return false;
        }
        String text = KeyEvent.getKeyText(keyCode);
        return text != null && !text.startsWith("Unknown keyCode");
    }

    private boolean canCollapseLegacyMouseCommand(List<Action> actions, int index) {
        if (!(actions.get(index) instanceof MouseMovePathAction moveAction)) {
            return false;
        }
        if (index + 1 >= actions.size()) {
            return false;
        }
        if (!(actions.get(index + 1) instanceof MouseButtonAction mouseButtonAction)) {
            return false;
        }
        if (mouseButtonAction.hasCoordinates() || mouseButtonAction.delayBeforeMs > 0 || moveAction.points.size() != 1) {
            return false;
        }
        return moveAction.points.getFirst().dtMs == 0;
    }
}
