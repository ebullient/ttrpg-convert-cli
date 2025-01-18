package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.Tags;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools hazard attributes ({@code hazard2md.txt})
 *
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase}.
 */
@TemplateData
public class QuteHazard extends Tools5eQuteBase {

    /** Type of hazard: "Magical Trap", "Wilderness Hazard" */
    public final String hazardType;

    public QuteHazard(CompendiumSources sources, String name, String source,
            String hazardType,
            List<ImageRef> images, String text, Tags tags) {
        super(sources, name, source, images, text, tags);
        this.hazardType = hazardType;
        withTemplate("hazard2md.txt"); // not trap or hazard (types)
    }
}
