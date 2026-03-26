package macrosnik.storage;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class ObjectMapperFactory {
    private ObjectMapperFactory() { }

    public static ObjectMapper create() {
        return new ObjectMapper();
    }
}
