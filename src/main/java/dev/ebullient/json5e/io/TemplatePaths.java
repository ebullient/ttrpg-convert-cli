package dev.ebullient.json5e.io;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import picocli.CommandLine.Option;

public class TemplatePaths {
    public final Map<String, Path> customTemplates = new HashMap<>();

    @Inject
    Json5eTui tui;

    public void setCustomTemplate(String key, Path path) {
        if (Files.exists(path)) {
            customTemplates.put(key, path);
        } else {
            tui.errorf("Specified template '%s' does not exist", path);
        }
    }

    @Option(names = { "--background" }, order = 1, description = "Path to Qute template for Backgrounds")
    void setBackgroundTemplatePath(Path path) {
        setCustomTemplate("background2md.txt", path);
    }

    @Option(names = { "--class" }, order = 2, description = "Path to Qute template for Classes")
    void setClassTemplatePath(Path path) {
        setCustomTemplate("class2md.txt", path);
    }

    @Option(names = { "--deity" }, order = 3, description = "Path to Qute template for Deities")
    void setDeityTemplatePath(Path path) {
        setCustomTemplate("deity2md.txt", path);
    }

    @Option(names = { "--feat" }, order = 4, description = "Path to Qute template for Feats")
    void setFeatTemplatePath(Path path) {
        setCustomTemplate("feat2md.txt", path);
    }

    @Option(names = { "--item" }, order = 5, description = "Path to Qute template for Items")
    void setItemTemplatePath(Path path) {
        setCustomTemplate("item2md.txt", path);
    }

    @Option(names = { "--monster" }, order = 6, description = "Path to Qute template for Monsters")
    void setMonsterTemplatePath(Path path) {
        setCustomTemplate("monster2md.txt", path);
    }

    @Option(names = { "--name" }, order = 7, description = "Path to Qute template for Names")
    void setNameTemplatePath(Path path) {
        setCustomTemplate("name2md.txt", path);
    }

    @Option(names = { "--note" }, order = 8, description = "Path to Qute template for Notes")
    void setNoteTemplatePath(Path path) {
        setCustomTemplate("note2md.txt", path);
    }

    @Option(names = { "--race" }, order = 9, description = "Path to Qute template for Races")
    void setRaceTemplatePath(Path path) {
        setCustomTemplate("race2md.txt", path);
    }

    @Option(names = { "--spell" }, order = 10, description = "Path to Qute template for Spells")
    void setSpellTemplatePath(Path path) {
        setCustomTemplate("spell2md.txt", path);
    }

    @Option(names = { "--subclass" }, order = 11, description = "Path to Qute template for Subclasses")
    void setSubclassTemplatePath(Path path) {
        setCustomTemplate("subclass2md.txt", path);
    }

    public Path get(String id) {
        return customTemplates.get(id);
    }
}
