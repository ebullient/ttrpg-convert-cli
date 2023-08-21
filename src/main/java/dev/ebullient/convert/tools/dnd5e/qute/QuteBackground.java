package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * 5eTools background attributes ({@code background2md.txt}).
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.Tools5eQuteBase}.
 * </p>
 */
@TemplateData
@RegisterForReflection
public class QuteBackground extends Tools5eQuteBase {
    /** List of images for this background (as {@link dev.ebullient.convert.qute.ImageRef}) */
    public final List<ImageRef> fluffImages;

    public QuteBackground(Tools5eSources sources,
            String name, String source, String text,
            List<ImageRef> images,
            Tags tags) {
        super(sources, name, source, text, tags);
        this.fluffImages = images;
    }
}
