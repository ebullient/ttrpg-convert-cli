package dev.ebullient.convert.io;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

public class TemplatePaths {

    final static List<String> KEYS = List.of("background", "class", "deity",
            "feat", "item", "monster", "name", "note", "race", "spell",
            "subclass");

    public final Map<String, Path> customTemplates = new HashMap<>();
    public final Map<String, Path> badKeys = new HashMap<>();
    public final Map<String, Path> badTemplates = new HashMap<>();

    @Inject
    Json5eTui tui;

    public void setCustomTemplate(String key, Path path) {
        if (!KEYS.contains(key)) {
            badKeys.put(key, path);
            return;
        }

        if (Files.isRegularFile(path)) {
            customTemplates.put(key + "2md.txt", path);
            return;
        }

        Path resolved = Path.of("").resolve(path);
        if (Files.isRegularFile(resolved)) {
            customTemplates.put(key + "2md.txt", path);
            return;
        }
        badTemplates.put(key, path);
    }

    @Option(names = { "--background" }, order = 1, description = "Path to Qute template for Backgrounds")
    void setBackgroundTemplatePath(Path path) {
        setCustomTemplate("background", path);
    }

    @Option(names = { "--class" }, order = 2, description = "Path to Qute template for Classes")
    void setClassTemplatePath(Path path) {
        setCustomTemplate("class", path);
    }

    @Option(names = { "--deity" }, order = 3, description = "Path to Qute template for Deities")
    void setDeityTemplatePath(Path path) {
        setCustomTemplate("deity", path);
    }

    @Option(names = { "--feat" }, order = 4, description = "Path to Qute template for Feats")
    void setFeatTemplatePath(Path path) {
        setCustomTemplate("feat", path);
    }

    @Option(names = { "--item" }, order = 5, description = "Path to Qute template for Items")
    void setItemTemplatePath(Path path) {
        setCustomTemplate("item", path);
    }

    @Option(names = { "--monster" }, order = 6, description = "Path to Qute template for Monsters")
    void setMonsterTemplatePath(Path path) {
        setCustomTemplate("monster", path);
    }

    @Option(names = { "--name" }, order = 7, description = "Path to Qute template for Names")
    void setNameTemplatePath(Path path) {
        setCustomTemplate("name", path);
    }

    @Option(names = { "--note" }, order = 8, description = "Path to Qute template for Notes")
    void setNoteTemplatePath(Path path) {
        setCustomTemplate("note", path);
    }

    @Option(names = { "--race" }, order = 9, description = "Path to Qute template for Races")
    void setRaceTemplatePath(Path path) {
        setCustomTemplate("race", path);
    }

    @Option(names = { "--spell" }, order = 10, description = "Path to Qute template for Spells")
    void setSpellTemplatePath(Path path) {
        setCustomTemplate("spell", path);
    }

    @Option(names = { "--subclass" }, order = 11, description = "Path to Qute template for Subclasses")
    void setSubclassTemplatePath(Path path) {
        setCustomTemplate("subclass", path);
    }

    public Path get(String id) {
        return customTemplates.get(id);
    }

    void verify(Json5eTui tui) {
        if (badKeys.isEmpty() && badTemplates.isEmpty()) {
            return;
        }
        badKeys.forEach((k, v) -> {
            tui.errorf("Unknown template key %s. Valid keys: %s",
                    k, KEYS);
        });
        badTemplates.forEach((k, v) -> {
            tui.errorf("Template file specified for '%s' (%s) does not exist or is not a file.",
                    k, v);
        });
        throw new ParameterException(tui.getCommandLine(), "Incorrect template specification.");
    }
}
