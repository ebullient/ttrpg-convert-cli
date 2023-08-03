package dev.ebullient.convert.tools.dnd5e.qute;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndexType;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;

public class Tools5eQuteBase extends QuteBase {

    public Tools5eQuteBase(CompendiumSources sources, String name, String source, String text, Tags tags) {
        super(sources, name, source, text, tags);
    }

    public static String sourceIfNotDefault(Tools5eSources sources) {
        return sourceIfNotDefault(sources.primarySource(), sources.getType());
    }

    public static String sourceIfNotDefault(String source, Tools5eIndexType type) {
        switch (source.toLowerCase()) {
            case "phb":
            case "mm":
            case "dmg":
                return "";
            default:
                if (type != null && source.equalsIgnoreCase(type.defaultSourceString())) {
                    return "";
                }
                return "-" + source.toLowerCase();
        }
    }

    public static String getRelativePath(Tools5eIndexType type) {
        return switch (type) {
            case adventureData -> "adventures";
            case bookData -> "books";
            case card, deck -> "items";
            case deity -> "deities";
            case monster -> "bestiary";
            case optionalfeature, optionalFeatureTypes -> "optional-features";
            case race, subrace -> "races";
            case subclass, classtype -> "classes";
            case table, tableGroup -> "tables";
            case trap, hazard -> "traps-hazards";
            case variantrule -> "variant-rules";
            default -> {
                if (!type.writeFile() && !type.useQuteNote()) {
                    throw new IllegalArgumentException("Verify the relative path usage for " + type);
                }
                yield type.name() + 's';
            }
        };
    }

    public static String monsterPath(boolean isNpc, String type) {
        return Tools5eQuteBase.getRelativePath(Tools5eIndexType.monster) + "/" + (isNpc ? "npc" : type);
    }

    public static String getClassResource(String className, String classSource) {
        return Tui.slugify(className) + Tools5eQuteBase.sourceIfNotDefault(classSource, Tools5eIndexType.classtype);
    }

    public static String getSubclassResource(String subclass, String parentClass, String subclassSource) {
        return Tui.slugify(parentClass) + "-" + Tui.slugify(subclass) +
                Tools5eQuteBase.sourceIfNotDefault(subclassSource, Tools5eIndexType.subclass);
    }

    public static String getDeityResourceName(String name, String pantheon) {
        return pantheon + "-" + name;
    }

    @Override
    public String targetFile() {
        Tools5eSources sources = (Tools5eSources) sources();
        if (sources != null) {
            return getName() + sourceIfNotDefault(sources);
        }
        return getName();
    }

    public String targetPath() {
        Tools5eSources sources = (Tools5eSources) sources();
        if (sources != null) {
            return Tools5eQuteBase.getRelativePath(sources.getType());
        }
        return ".";
    }
}
