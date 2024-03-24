package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndexType;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;

/**
 * Attributes for notes that are generated from the 5eTools data.
 * This is a trivial extension of {@link dev.ebullient.convert.qute.QuteBase}.
 * <p>
 * Notes created from {@code Tools5eQuteBase} will use a specific template
 * for the type. For example, {@code QuteBackground} will use {@code background2md.txt}.
 * </p>
 */
@TemplateData
public class Tools5eQuteBase extends QuteBase {

    String targetPath;
    String filename;
    String template;

    public Tools5eQuteBase(CompendiumSources sources, String name, String source, String text, Tags tags) {
        super(sources, name, source, text, tags);
    }

    public static String fixFileName(String name, Tools5eSources sources) {
        Tools5eIndexType type = sources.getType();
        JsonNode node = sources.findNode();
        String primarySource = sources.primarySource();
        return switch (type) {
            case background -> fixFileName(type.decoratedName(node), primarySource, type);
            case deity -> Tui.slugify(getDeityResourceName(name, primarySource, node.get("pantheon").asText()));
            case subclass -> getSubclassResource(name, node.get("className").asText(), primarySource);
            default -> fixFileName(name, primarySource, type);
        };
    }

    public static String fixFileName(String name, String source, Tools5eIndexType type) {
        if (type == Tools5eIndexType.adventureData
                || type == Tools5eIndexType.adventure
                || type == Tools5eIndexType.book
                || type == Tools5eIndexType.bookData
                || type == Tools5eIndexType.tableGroup) {
            return Tui.slugify(name); // file name is based on chapter, etc.
        }
        name = Tui.slugify(name.replaceAll(" \\(\\*\\)", "-gv"));
        return name + sourceIfNotDefault(source, type);
    }

    private static String sourceIfNotDefault(String source, Tools5eIndexType type) {
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

    public static String monsterPath(boolean isNpc, String type) {
        return Tools5eIndexType.monster.getRelativePath() + "/" + (isNpc ? "npc" : type);
    }

    public static String getClassResource(String className, String classSource) {
        return fixFileName(className, classSource, Tools5eIndexType.classtype);
    }

    public static String getSubclassResource(String subclass, String parentClass, String subclassSource) {
        return fixFileName(
                Tui.slugify(parentClass) + "-" + Tui.slugify(subclass),
                subclassSource,
                Tools5eIndexType.subclass);
    }

    public static String getDeityResourceName(String name, String source, String pantheon) {
        String suffix = "";
        switch (pantheon.toLowerCase()) {
            case "exandria" -> {
                if (!source.equalsIgnoreCase("egw")) {
                    suffix = "-" + source.toLowerCase();
                }
            }
        }
        return pantheon + "-" + name + suffix;
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
        return fixFileName(getName(), sources);
    }

    public Tools5eQuteBase withTargetPath(String path) {
        this.targetPath = path;
        return this;
    }

    public String targetPath() {
        if (targetPath != null) {
            return targetPath;
        }
        if (sources() != null) {
            Tools5eSources sources = (Tools5eSources) sources();
            return sources.getType().getRelativePath();
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

    public Collection<QuteBase> inlineNotes() {
        return sources() == null
                ? List.of()
                : Tools5eSources.getInlineNotes(sources().getKey());
    }
}
