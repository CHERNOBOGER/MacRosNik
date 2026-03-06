package macrosnik.storage;

import macrosnik.domain.KeyAction;
import macrosnik.domain.Macro;
import macrosnik.domain.MouseButtonAction;
import macrosnik.domain.MouseMovePathAction;
import macrosnik.domain.enums.KeyActionType;
import macrosnik.domain.enums.MouseButton;
import macrosnik.domain.enums.MouseButtonActionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MacroStorageIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadRoundTripKeepsPolymorphicActions() throws Exception {
        MacroStorage storage = new MacroStorage();
        Macro macro = new Macro("Smoke macro");
        macro.actions.add(new KeyAction(25, 65, KeyActionType.DOWN));
        macro.actions.add(new MouseButtonAction(40, MouseButton.RIGHT, MouseButtonActionType.CLICK));
        MouseMovePathAction pathAction = new MouseMovePathAction(5);
        pathAction.points.add(new MouseMovePathAction.PathPoint(10, 20, 0));
        pathAction.points.add(new MouseMovePathAction.PathPoint(25, 35, 15));
        macro.actions.add(pathAction);

        Path path = tempDir.resolve("nested/macros/demo.json");
        storage.save(path, macro);
        assertTrue(path.toFile().isFile());

        Macro loaded = storage.load(path);
        assertEquals("Smoke macro", loaded.name);
        assertEquals(3, loaded.actions.size());
        assertInstanceOf(KeyAction.class, loaded.actions.get(0));
        assertInstanceOf(MouseButtonAction.class, loaded.actions.get(1));
        assertInstanceOf(MouseMovePathAction.class, loaded.actions.get(2));
    }
}
