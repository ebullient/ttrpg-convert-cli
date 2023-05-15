package dev.ebullient.convert.tools.dnd5e.qute;

import java.nio.file.Path;
import java.util.Collection;

import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.tools.CompendiumSources;

public class QuteSource extends QuteBase {
    public static String BACKGROUND_PATH = "backgrounds";
    public static String CLASSES_PATH = "classes";
    public static String DEITIES_PATH = "deities";
    public static String FEATS_PATH = "feats";
    public static String ITEMS_PATH = "items";
    public static String MONSTERS_BASE_PATH = "bestiary";
    public static String RACES_PATH = "races";
    public static String SPELLS_PATH = "spells";

    public QuteSource(CompendiumSources sources, String name, String source, String text, Collection<String> tags) {
        super(sources, name, source, text, tags);
    }

    public static String sourceIfNotCore(String source) {
        switch (source.toLowerCase()) {
            case "phb":
            case "mm":
            case "dmg":
                return "";
            default:
                return "-" + source.toLowerCase();
        }
    }

    public static String sourceIfNotDefault(String source, String defaultSource) {
        if (!source.equalsIgnoreCase(defaultSource)) {
            return "-" + source.toLowerCase();
        }
        return "";
    }

    public static String monsterPath(boolean isNpc, String type) {
        return Path.of(QuteSource.MONSTERS_BASE_PATH, (isNpc ? "npc" : type)).toString();
    }

    public static String getSubclassResource(String subclass, String parentClass, String subclassSource) {
        return parentClass + "-" + subclass + QuteSource.sourceIfNotCore(subclassSource);
    }

    public static String getDeityResourceName(String name, String pantheon) {
        return pantheon + "-" + name;
    }

    @Override
    public String targetFile() {
        return getName() + QuteSource.sourceIfNotCore(sources().primarySource());
    }
}
