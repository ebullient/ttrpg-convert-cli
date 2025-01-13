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
    /** Formatted text listing other prerequisite conditions (optional) */
    public final String prerequisite;

    public QuteBackground(Tools5eSources sources, String name, String source,
            String prerequisite,
            List<ImageRef> images, String text, Tags tags) {
        super(sources, name, source, images, text, tags);
        this.prerequisite = prerequisite; // optional
    }
}
