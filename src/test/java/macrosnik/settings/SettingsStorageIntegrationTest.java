package macrosnik.settings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SettingsStorageIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void loadOrDefaultCreatesFileWhenItDoesNotExist() {
        Path path = tempDir.resolve("config/settings.json");
        SettingsStorage storage = new SettingsStorage(path);
        AppSettings settings = storage.loadOrDefault();
        assertEquals(0, settings.pauseResumeKey);
        assertEquals(0, settings.stopKey);
        assertTrue(Files.exists(path));
    }

    @Test
    void saveAndLoadRoundTripKeepsCustomValues() throws Exception {
        Path path = tempDir.resolve("config/settings.json");
        SettingsStorage storage = new SettingsStorage(path);
        AppSettings original = new AppSettings();
        original.pauseResumeKey = 119;
        original.stopKey = 123;
        storage.save(original);
        AppSettings loaded = storage.loadOrDefault();
        assertEquals(119, loaded.pauseResumeKey);
        assertEquals(123, loaded.stopKey);
    }

    @Test
    void loadOrDefaultResetsCorruptedFileToDefaults() throws Exception {
        Path path = tempDir.resolve("config/settings.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, "{ broken json }");
        SettingsStorage storage = new SettingsStorage(path);
        AppSettings loaded = storage.loadOrDefault();
        assertEquals(0, loaded.pauseResumeKey);
        assertEquals(0, loaded.stopKey);
        String normalizedJson = Files.readString(path);
        assertTrue(normalizedJson.contains("pauseResumeKey"));
        assertTrue(normalizedJson.contains("stopKey"));
    }
}
