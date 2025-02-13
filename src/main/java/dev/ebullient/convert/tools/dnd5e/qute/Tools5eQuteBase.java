package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.JsonTextConverter.SourceField;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Json2QuteDeity.DeityField;
import dev.ebullient.convert.tools.dnd5e.JsonSource.Tools5eFields;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndexType;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;

/**
 * Attributes for notes that are generated from the 5eTools data.
 * This is a trivial extension of {@link dev.ebullient.convert.qute.QuteBase}.
 *
 * Notes created from {@code Tools5eQuteBase} will use a specific template
 * for the type. For example, {@code QuteBackground} will use {@code background2md.txt}.
 */
@TemplateData
public class Tools5eQuteBase extends QuteBase {

    /** List of images as {@link dev.ebullient.convert.qute.ImageRef} (optional) */
    public final List<ImageRef> fluffImages;

    String targetPath;
    String filename;
    String template;

    public Tools5eQuteBase(CompendiumSources sources, String name, String source, List<ImageRef> fluffImages, String text,
            Tags tags) {
        super(sources, name, source, text, tags);
        this.fluffImages = isPresent(fluffImages) ? fluffImages : List.of();
    }

    /**
     * Return true if any images are present
     */
    public boolean getHasImages() {
        return !fluffImages.isEmpty();
    }

    /**
     * Return true if more than one image is present
     */
    public boolean getHasMoreImages() {
        return fluffImages.size() > 1;
    }

    /**
     * Return an embedded wikilink to the first image
     * Will have the "right" anchor tag.
     */
    public String getShowPortraitImage() {
        if (fluffImages.isEmpty()) {
            return "";
        }
        return fluffImages.get(0).getEmbeddedLink("right");
    }

    /**
     * Return embedded wikilinks for all images
     * If there is more than one, they will be displayed in a gallery.
     */
    public String getShowAllImages() {
        return createImageLinks(false);
    }

    /**
     * Return embedded wikilinks for all but the first image
     * If there is more than one, they will be displayed in a gallery.
     */
    public String getShowMoreImages() {
        return createImageLinks(true);
    }

    private String createImageLinks(boolean omitFirst) {
        if (fluffImages.isEmpty()) {
            return "";
        }
        if (fluffImages.size() == 1 && !omitFirst) {
            return fluffImages.get(0).getEmbeddedLink("center");
        }
        if (fluffImages.size() == 2 && omitFirst) {
            return fluffImages.get(1).getEmbeddedLink("center");
        }
        List<String> lines = new ArrayList<>();
        lines.add("> [!gallery]");
        for (int i = omitFirst ? 1 : 0; i < fluffImages.size(); i++) {
            lines.add(fluffImages.get(i).getEmbeddedLink("")); // no anchor
        }
        return String.join("\n", lines);
    }

    public static String fixFileName(String name, Tools5eSources sources) {
        Tools5eIndexType type = sources.getType();
        JsonNode node = sources.findNode();
        String primarySource = sources.primarySource();
        return switch (type) {
            case background -> fixFileName(type.decoratedName(node), primarySource, type);
            case deity -> getDeityResourceName(name, primarySource, DeityField.pantheon.getTextOrEmpty(node));
            case subclass -> getSubclassResource(name,
                    Tools5eFields.className.getTextOrEmpty(node),
                    Tools5eFields.classSource.getTextOrEmpty(node),
                    primarySource);
            case optionalFeatureTypes -> getOptionalFeatureTypeResource(name);
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
        if (type == Tools5eIndexType.optionalFeatureTypes) {
            return getOptionalFeatureTypeResource(name);
        }
        return Tui.slugify(name.replaceAll(" \\(\\*\\)", "-gv")
                + sourceIfNotDefault(source, type));
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
                return "-" + Tui.slugify(source);
        }
    }

    public static String monsterPath(boolean isNpc, String type) {
        return Tools5eIndexType.monster.getRelativePath() + "/" + (isNpc ? "npc" : type);
    }

    public static String getClassResource(String className, String classSource) {
        return fixFileName(className, classSource, Tools5eIndexType.classtype);
    }

    public static String getSubclassResource(String subclass, String parentClass, String classSource, String subclassSource) {
        String parentFile = Tui.slugify(parentClass);
        if ("xphb".equalsIgnoreCase(classSource)) {
            // For the most part, all subclasses are derived from the basic classes.
            // There wasn't really a need to include the class source in the file name.
            // However, the XPHB has created duplicates of all of the base classes.
            // So if the parent class is from the XPHB, we need to include that in the file name.
            parentFile += "-xphb";
        }
        return fixFileName(
                parentFile + "-" + Tui.slugify(subclass),
                subclassSource,
                Tools5eIndexType.subclass);
    }

    public static String getDeityResourceName(String name, String source, String pantheon) {
        String suffix = "";
        switch (pantheon.toLowerCase()) {
            case "exandria" -> {
                suffix = source.equalsIgnoreCase("egw") ? "" : ("-" + Tui.slugify(source));
            }
            case "dragonlance" -> {
                suffix = source.equalsIgnoreCase("dsotdq") ? "" : ("-" + Tui.slugify(source));
            }
            default -> {
                suffix = sourceIfNotDefault(source, Tools5eIndexType.deity);
            }
        }
        return Tui.slugify(pantheon + "-" + name) + suffix;
    }

    public static String getOptionalFeatureTypeResource(String name) {
        return Tui.slugify("list-optfeaturetype-" + name);
    }

    public static String getClassSpellList(String className) {
        return "list-spells-%s-%s".formatted(
                Tools5eIndexType.classtype.getRelativePath(),
                className.toLowerCase());
    }

    public static String getClassSpellList(JsonNode classNode) {
        return "list-spells-%s-%s".formatted(
                Tools5eIndexType.classtype.getRelativePath(),
                SourceField.name.getTextOrEmpty(classNode).toLowerCase());
    }

    public static String getSpellList(String name, Tools5eSources sources) {
        Tools5eIndexType type = sources.getType();
        JsonNode node = sources.findNode();
        if (type == Tools5eIndexType.classtype) {
            return getClassSpellList(node);
        }
        final String fileResource = fixFileName(name, sources);
        return "list-spells-%s-%s".formatted(type.getRelativePath(), fileResource);
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
