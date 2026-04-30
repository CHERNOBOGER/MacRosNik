package macrosnik.ui;

import macrosnik.domain.Macro;

import java.nio.file.Path;

final class MacroDocument {
    private static final String DEFAULT_NAME = "Новый макрос";

    private Macro macro = new Macro(DEFAULT_NAME);
    private Path filePath;
    private boolean dirty;

    Macro macro() {
        return macro;
    }

    void replaceMacro(Macro macro) {
        this.macro = macro == null ? new Macro(DEFAULT_NAME) : macro;
    }

    void reset() {
        macro = new Macro(DEFAULT_NAME);
        filePath = null;
        dirty = false;
    }

    Path filePath() {
        return filePath;
    }

    void setFilePath(Path filePath) {
        this.filePath = filePath;
    }

    boolean isDirty() {
        return dirty;
    }

    void markDirty() {
        dirty = true;
    }

    void markClean() {
        dirty = false;
    }

    boolean hasActions() {
        return macro.actions != null && !macro.actions.isEmpty();
    }

    void syncMacroNameToFile() {
        macro.name = displayName();
    }

    String displayName() {
        if (filePath != null) {
            String fileName = filePath.getFileName().toString();
            int dot = fileName.lastIndexOf('.');
            return dot > 0 ? fileName.substring(0, dot) : fileName;
        }
        return macro.name == null || macro.name.isBlank() ? DEFAULT_NAME : macro.name;
    }

    String windowTitle() {
        return "MacRosNik — " + displayName() + (dirty ? " *" : "");
    }

    String fileLabel() {
        return "Файл: " + (filePath == null ? "не выбран" : filePath);
    }

    Path ensureJsonExtension(Path path) {
        String value = path.toString();
        return value.toLowerCase().endsWith(".json") ? path : Path.of(value + ".json");
    }
}
