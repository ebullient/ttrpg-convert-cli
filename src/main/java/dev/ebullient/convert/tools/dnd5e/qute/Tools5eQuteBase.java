package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.Collection;

import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.tools.CompendiumSources;

public class Tools5eQuteBase extends QuteBase {

    public static final String ADVENTURE_PATH = "adventures";
    public static final String BOOK_PATH = "books";
    public static final String BACKGROUND_PATH = "backgrounds";
    public static final String CLASSES_PATH = "classes";
    public static final String DEITIES_PATH = "deities";
    public static final String FEATS_PATH = "feats";
    public static final String ITEMS_PATH = "items";
    public static final String MONSTERS_BASE_PATH = "bestiary";
    public static final String RACES_PATH = "races";
    public static final String SPELLS_PATH = "spells";
    public static final String TABLES_PATH = "tables";
    public static final String VR_PATH = "variant-rules";

    public Tools5eQuteBase(CompendiumSources sources, String name, String source, String text, Collection<String> tags) {
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
        return Tools5eQuteBase.MONSTERS_BASE_PATH + "/" + (isNpc ? "npc" : type);
    }

    public static String getSubclassResource(String subclass, String parentClass, String subclassSource) {
        return parentClass + "-" + subclass + Tools5eQuteBase.sourceIfNotCore(subclassSource);
    }

    public static String getDeityResourceName(String name, String pantheon) {
        return pantheon + "-" + name;
    }

    @Override
    public String targetFile() {
        return getName() + sourceIfNotCore(sources().primarySource());
    }
}
