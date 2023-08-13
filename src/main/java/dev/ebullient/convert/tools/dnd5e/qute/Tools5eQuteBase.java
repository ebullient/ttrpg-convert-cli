package dev.ebullient.convert.tools.dnd5e.qute;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndexType;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Attributes for notes that are generated from the 5eTools data.
 * This is a trivial extension of {@link dev.ebullient.convert.qute.QuteBase QuteBase}.
 * <p>
 * Notes created from {@code Tools5eQuteBase} will use a specific template
 * for the type. For example, {@code QuteBackground} will use {@code background2md.txt}.
 * </p>
 */
@TemplateData
@RegisterForReflection
public class Tools5eQuteBase extends QuteBase {

    String targetPath;
    String filename;
    String template;

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

    public Tools5eQuteBase withTargetFile(String filename) {
        this.filename = filename;
        return this;
    }

    public String targetFile() {
        if (filename != null) {
            return filename;
        }
        Tools5eSources sources = (Tools5eSources) sources();
        if (sources != null) {
            return getName() + sourceIfNotDefault(sources);
        }
        return getName();
    }

    public Tools5eQuteBase withTargetPath(String path) {
        this.targetPath = path;
        return this;
    }

    public String targetPath() {
        if (targetPath != null) {
            return targetPath;
        }
        Tools5eSources sources = (Tools5eSources) sources();
        if (sources != null) {
            return Tools5eQuteBase.getRelativePath(sources.getType());
        }
        return ".";
    }

    public Tools5eQuteBase withTemplate(String template) {
        this.template = template;
        return this;
    }

    public String template() {
        return template == null ? super.template() : template;
    }
}
