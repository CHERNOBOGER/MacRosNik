package macrosnik.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import macrosnik.domain.Macro;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MacroStorage {
    private final ObjectMapper mapper = ObjectMapperFactory.create();

    public void save(Path path, Macro macro) throws IOException {
        Files.createDirectories(path.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), macro);
    }

    public Macro load(Path path) throws IOException {
        return mapper.readValue(path.toFile(), Macro.class);
    }
}
