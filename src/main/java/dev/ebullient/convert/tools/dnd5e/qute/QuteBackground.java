package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools background attributes ({@code background2md.txt}).
 *
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase}.
 */
@TemplateData
public class QuteBackground extends Tools5eQuteBase {
    /** List of images for this background (as {@link dev.ebullient.convert.qute.ImageRef}) */
    public final List<ImageRef> fluffImages;
    /** Formatted text listing other prerequisite conditions (optional) */
    public final String prerequisite;

    public QuteBackground(Tools5eSources sources, String name, String source,
            String prerequisite,
            String text, List<ImageRef> images, Tags tags) {
        super(sources, name, source, text, tags);
        this.fluffImages = images;
        this.prerequisite = prerequisite; // optional
    }
}
