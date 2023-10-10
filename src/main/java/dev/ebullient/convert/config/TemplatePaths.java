package dev.ebullient.convert.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import dev.ebullient.convert.io.Tui;
import picocli.CommandLine.Option;

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

    @Option(names = { "--background" }, order = 1, hidden = true, description = "Path to Qute template for Backgrounds")
    void setBackgroundTemplatePath(Path path) {
        setCustomTemplate("background", path);
    }

    @Option(names = { "--class" }, order = 2, hidden = true, description = "Path to Qute template for Classes")
    void setClassTemplatePath(Path path) {
        setCustomTemplate("class", path);
    }

    @Option(names = { "--deity" }, order = 3, hidden = true, description = "Path to Qute template for Deities")
    void setDeityTemplatePath(Path path) {
        setCustomTemplate("deity", path);
    }

    @Option(names = { "--feat" }, order = 4, hidden = true, description = "Path to Qute template for Feats")
    void setFeatTemplatePath(Path path) {
        setCustomTemplate("feat", path);
    }

    @Option(names = { "--item" }, order = 5, hidden = true, description = "Path to Qute template for Items")
    void setItemTemplatePath(Path path) {
        setCustomTemplate("item", path);
    }

    @Option(names = { "--monster" }, order = 6, hidden = true, description = "Path to Qute template for Monsters")
    void setMonsterTemplatePath(Path path) {
        setCustomTemplate("monster", path);
    }

    @Option(names = { "--name" }, order = 7, hidden = true, description = "Path to Qute template for Names")
    void setNameTemplatePath(Path path) {
        setCustomTemplate("name", path);
    }

    @Option(names = { "--note" }, order = 8, hidden = true, description = "Path to Qute template for Notes")
    void setNoteTemplatePath(Path path) {
        setCustomTemplate("note", path);
    }

    @Option(names = { "--race" }, order = 9, hidden = true, description = "Path to Qute template for Races")
    void setRaceTemplatePath(Path path) {
        setCustomTemplate("race", path);
    }

    @Option(names = { "--spell" }, order = 10, hidden = true, description = "Path to Qute template for Spells")
    void setSpellTemplatePath(Path path) {
        setCustomTemplate("spell", path);
    }

    @Option(names = { "--subclass" }, order = 11, hidden = true, description = "Path to Qute template for Subclasses")
    void setSubclassTemplatePath(Path path) {
        setCustomTemplate("subclass", path);
    }

    public Path get(String id) {
        return customTemplates.get(id);
    }

    public void verify(Tui tui) {
        Map<String, Path> badKeys = new HashMap<>();

        // Check template keys after game system config has been loaded
        customTemplates.forEach((k, v) -> {
            if (!TtrpgConfig.getTemplateKeys().contains(toConfigKey(k))) {
                badKeys.put(k, v);
            }
        });
        if (badKeys.isEmpty() && badTemplates.isEmpty()) {
            return;
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
        tui.throwInvalidArgumentException("Bad template specified");
    }

    private String toTemplateKey(String key) {
        return key + (key.startsWith("index") ? ".txt" : "2md.txt");
    }

    private String toConfigKey(String key) {
        return key.replace("2md.txt", "")
                .replace(".txt", "");
    }
}
