package macrosnik.hotkey;

import com.github.kwhat.jnativehook.NativeLibraryLocator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * JNativeHook's default locator breaks inside a jpackage app-image because the
 * library code source is no longer a plain file URL. This locator extracts the
 * bundled native library to a writable temp folder and points JNativeHook to it.
 */
public class PackagedJNativeHookLibraryLocator implements NativeLibraryLocator {
    @Override
    public Iterator<File> getLibraries() {
        String family = detectFamily();
        String arch = detectArchitecture(family);
        String libraryName = System.mapLibraryName(System.getProperty("jnativehook.lib.name", "JNativeHook"))
                .replaceAll("\\.jnilib$", ".dylib");
        String resourcePath = "native/jnativehook/com/github/kwhat/jnativehook/lib/"
                + family + "/" + arch + "/" + libraryName;

        try (InputStream inputStream = PackagedJNativeHookLibraryLocator.class.getResourceAsStream("/" + resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Не найден native-ресурс JNativeHook: /" + resourcePath);
            }

            Path targetDir = Files.createDirectories(
                    Path.of(System.getProperty("java.io.tmpdir"), "MacRosNik", "jnativehook"));
            Path targetFile = targetDir.resolve(versionedFileName(libraryName, arch));

            if (Files.notExists(targetFile)) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return List.of(targetFile.toFile()).iterator();
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось извлечь native-библиотеку JNativeHook", e);
        }
    }

    private static String detectFamily() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("win")) {
            return "windows";
        }
        if (osName.contains("mac")) {
            return "darwin";
        }
        if (osName.contains("nux") || osName.contains("linux")) {
            return "linux";
        }
        throw new IllegalStateException("Неподдерживаемая ОС для JNativeHook: " + osName);
    }

    private static String detectArchitecture(String family) {
        String osArch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return switch (osArch) {
            case "amd64", "x86_64" -> "x86_64";
            case "x86", "i386", "i486", "i586", "i686" -> "x86";
            case "aarch64", "arm64" -> family.equals("windows") ? "arm" : "arm64";
            case "arm" -> "arm";
            default -> throw new IllegalStateException("Неподдерживаемая архитектура для JNativeHook: " + osArch);
        };
    }

    private static String versionedFileName(String libraryName, String arch) {
        String version = NativeLibraryLocator.class.getPackage().getImplementationVersion();
        String suffix = (version == null || version.isBlank()) ? arch : version + "-" + arch;
        int dotIndex = libraryName.lastIndexOf('.');
        if (dotIndex < 0) {
            return libraryName + "-" + suffix;
        }
        return libraryName.substring(0, dotIndex) + "-" + suffix + libraryName.substring(dotIndex);
    }
}
