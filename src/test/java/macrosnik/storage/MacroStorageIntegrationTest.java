package macrosnik.storage;

import macrosnik.domain.KeyAction;
import macrosnik.domain.Macro;
import macrosnik.domain.MouseButtonAction;
import macrosnik.domain.MouseMovePathAction;
import macrosnik.domain.TextInputAction;
import macrosnik.domain.enums.KeyActionType;
import macrosnik.domain.enums.MouseButton;
import macrosnik.domain.enums.MouseButtonActionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MacroStorageIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadRoundTripKeepsPolymorphicActions() throws Exception {
        MacroStorage storage = new MacroStorage();
        Macro macro = new Macro("Тестовый макрос");
        macro.actions.add(new KeyAction(25, 65, KeyActionType.DOWN));
        macro.actions.add(new MouseButtonAction(40, MouseButton.RIGHT, MouseButtonActionType.CLICK));
        MouseMovePathAction pathAction = new MouseMovePathAction(5);
        pathAction.points.add(new MouseMovePathAction.PathPoint(10, 20, 0));
        pathAction.points.add(new MouseMovePathAction.PathPoint(25, 35, 15));
        macro.actions.add(pathAction);
        macro.actions.add(new TextInputAction(0, "Привет"));

        Path path = tempDir.resolve("nested/macros/demo.json");
        storage.save(path, macro);
        assertTrue(path.toFile().isFile());
        assertTrue(Files.readString(path, StandardCharsets.UTF_8).contains("Тестовый макрос"));

        Macro loaded = storage.load(path);
        assertEquals("Тестовый макрос", loaded.name);
        assertEquals(4, loaded.actions.size());
        assertInstanceOf(KeyAction.class, loaded.actions.get(0));
        assertInstanceOf(MouseButtonAction.class, loaded.actions.get(1));
        assertInstanceOf(MouseMovePathAction.class, loaded.actions.get(2));
        TextInputAction textInputAction = assertInstanceOf(TextInputAction.class, loaded.actions.get(3));
        assertEquals("Привет", textInputAction.text);
    }
}
