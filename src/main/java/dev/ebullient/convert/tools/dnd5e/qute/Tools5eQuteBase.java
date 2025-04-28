package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eLinkifier;
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

    @Override
    public Tools5eSources sources() {
        return (Tools5eSources) sources;
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

    public Tools5eQuteBase withTargetFile(String filename) {
        this.filename = filename;
        return this;
    }

    public String targetFile() {
        if (filename != null) {
            return filename;
        }
        return linkifier().getTargetFileName(getName(), sources());
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
            return linkifier().getRelativePath(sources());
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

    protected Tools5eLinkifier linkifier() {
        return Tools5eLinkifier.instance();
    }
}
