package dev.ebullient.convert.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import dev.ebullient.convert.io.Tui;

public class TemplatePaths {

    public final Map<String, Path> customTemplates = new HashMap<>();
    public final Map<String, Path> badTemplates = new HashMap<>();

    public void setCustomTemplate(String key, Path path) {
        key = toTemplateKey(key);

        if (Files.isRegularFile(path)) {
            customTemplates.put(key, path);
            return;
        }

        Path resolved = Path.of("").resolve(path);
        if (Files.isRegularFile(resolved)) {
            customTemplates.put(key, resolved);
            return;
        }
        badTemplates.put(key, path);
    }

    public Path get(String id) {
        return customTemplates.get(id);
    }

    public boolean verify(Tui tui) {
        Map<String, Path> badKeys = new HashMap<>();

        // Check template keys after game system config has been loaded
        customTemplates.forEach((k, v) -> {
            if (!TtrpgConfig.getTemplateKeys().contains(toConfigKey(k))) {
                badKeys.put(k, v);
            }
        });
        if (badKeys.isEmpty() && badTemplates.isEmpty()) {
            return true;
        }
        badKeys.forEach((k, v) -> {
            customTemplates.remove(k);
            tui.errorf("Unknown template key %s. Valid keys: %s",
                    toConfigKey(k), TtrpgConfig.getTemplateKeys());
        });
        badTemplates.forEach((k, v) -> {
            tui.errorf("Template file specified for '%s' (%s) does not exist or is not a file.",
                    toConfigKey(k), v);
        });
        return false;
    }

    private String toTemplateKey(String key) {
        return key + (key.startsWith("index") ? ".txt" : "2md.txt");
    }

    private String toConfigKey(String key) {
        return key.replace("2md.txt", "")
                .replace(".txt", "");
    }
}
