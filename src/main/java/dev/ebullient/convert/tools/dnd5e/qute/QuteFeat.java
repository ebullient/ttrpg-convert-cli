package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools feat and optional feat attributes ({@code feat2md.txt})
 *
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase}.
 */
@TemplateData
public class QuteFeat extends Tools5eQuteBase {

    /** Prerequisite level */
    public final String level;
    /** Formatted text listing other prerequisite conditions (optional) */
    public final String prerequisite;

    public QuteFeat(Tools5eSources sources, String name, String source,
            String prerequisite, String level,
            List<ImageRef> images, String text, Tags tags) {
        super(sources, name, source, images, text, tags);
        withTemplate("feat2md.txt"); // Feat and OptionalFeature
        this.level = level;
        this.prerequisite = prerequisite; // optional
    }
}
