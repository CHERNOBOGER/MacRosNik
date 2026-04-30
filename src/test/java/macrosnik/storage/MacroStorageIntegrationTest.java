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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MacroStorageIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadRoundTripKeepsPolymorphicActions() throws Exception {
        MacroStorage storage = new MacroStorage();
        Macro macro = new Macro("test macro");
        macro.actions.add(new KeyAction(25, 65, KeyActionType.DOWN));
        macro.actions.add(new MouseButtonAction(40, MouseButton.RIGHT, MouseButtonActionType.CLICK, 15, 25));
        MouseMovePathAction pathAction = new MouseMovePathAction(5);
        pathAction.points.add(new MouseMovePathAction.PathPoint(10, 20, 0));
        pathAction.points.add(new MouseMovePathAction.PathPoint(25, 35, 15));
        macro.actions.add(pathAction);
        macro.actions.add(new TextInputAction(0, "hello"));

        Path path = tempDir.resolve("nested/macros/demo.json");
        storage.save(path, macro);
        assertTrue(path.toFile().isFile());
        assertTrue(Files.readString(path, StandardCharsets.UTF_8).contains("test macro"));

        Macro loaded = storage.load(path);
        assertEquals("test macro", loaded.name);
        assertEquals(4, loaded.actions.size());
        assertInstanceOf(KeyAction.class, loaded.actions.get(0));
        MouseButtonAction click = assertInstanceOf(MouseButtonAction.class, loaded.actions.get(1));
        assertEquals(15, click.x);
        assertEquals(25, click.y);
        assertInstanceOf(MouseMovePathAction.class, loaded.actions.get(2));
        TextInputAction textInputAction = assertInstanceOf(TextInputAction.class, loaded.actions.get(3));
        assertEquals("hello", textInputAction.text);
    }

    @Test
    void loadCollapsesLegacyMoveAndMouseButtonIntoSingleCoordinateAction() throws Exception {
        MacroStorage storage = new MacroStorage();
        Path path = tempDir.resolve("legacy.json");
        Files.writeString(path, """
                {
                  "version": 1,
                  "name": "legacy",
                  "actions": [
                    {
                      "type": "MOUSE_MOVE_PATH",
                      "delayBeforeMs": 15,
                      "coordinateMode": "SCREEN_ABSOLUTE",
                      "points": [
                        { "x": 300, "y": 200, "dtMs": 0 }
                      ]
                    },
                    {
                      "type": "MOUSE_BUTTON",
                      "delayBeforeMs": 0,
                      "button": "LEFT",
                      "action": "CLICK"
                    }
                  ]
                }
                """, StandardCharsets.UTF_8);

        Macro loaded = storage.load(path);

        assertEquals(1, loaded.actions.size());
        MouseButtonAction action = assertInstanceOf(MouseButtonAction.class, loaded.actions.getFirst());
        assertEquals(15, action.delayBeforeMs);
        assertEquals(MouseButton.LEFT, action.button);
        assertEquals(MouseButtonActionType.CLICK, action.action);
        assertEquals(300, action.x);
        assertEquals(200, action.y);
    }
}
