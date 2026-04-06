package macrosnik.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import macrosnik.domain.Action;
import macrosnik.domain.KeyAction;
import macrosnik.domain.Macro;
import macrosnik.util.NativeKeyCodeMapper;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
        normalizeLegacyKeyCodes(macro);
        return macro;
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
}
