package macrosnik.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import macrosnik.storage.ObjectMapperFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SettingsStorage {
    private final ObjectMapper mapper = ObjectMapperFactory.create();
    private final Path path;

    public SettingsStorage(Path path) {
        this.path = path;
    }

    public AppSettings loadOrDefault() {
        try {
            if (!Files.exists(path)) {
                AppSettings s = new AppSettings();
                save(s);
                return s;
            }
            return mapper.readValue(path.toFile(), AppSettings.class);
        } catch (Exception e) {
            AppSettings s = new AppSettings();
            try { save(s); } catch (IOException ignored) { }
            return s;
        }
    }

    public void save(AppSettings settings) throws IOException {
        Files.createDirectories(path.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), settings);
    }
}
