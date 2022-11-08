package dev.ebullient.json5e.qute;

import java.nio.file.Path;
import java.util.List;

public interface QuteSource {
    String BACKGROUND_PATH = "backgrounds";
    String CLASSES_PATH = "classes";
    String DEITIES_PATH = "deities";
    String FEATS_PATH = "feats";
    String ITEMS_PATH = "items";
    String MONSTERS_BASE_PATH = "bestiary";
    String RACES_PATH = "races";
    String SPELLS_PATH = "spells";

    static String sourceIfNotCore(String source) {
        switch (source.toLowerCase()) {
            case "phb":
            case "mm":
            case "dmg":
                return "";
            default:
                return "-" + source.toLowerCase();
        }
    }

    static String sourceIfNotDefault(String source, String defaultSource) {
        if (!source.equalsIgnoreCase(defaultSource)) {
            return "-" + source.toLowerCase();
        }
        return "";
    }

    static String monsterPath(boolean isNpc, String type) {
        return Path.of(QuteSource.MONSTERS_BASE_PATH, (isNpc ? "npc" : type)).toString();
    }

    static String getSubclassResourceName(String subclass, String parentClass) {
        return parentClass + "-" + subclass;
    }

    static String getDeityResourceName(String name, String pantheon) {
        return pantheon + "-" + name;
    }

    String getName();

    String getSource();

    List<ImageRef> images();

    String targetFile();

    String targetPath();

    String title();

    String key();
}
