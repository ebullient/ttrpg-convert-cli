package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.Tags;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools character creation option attributes ({@code charoption2md.txt})
 *
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase}.
 */
@TemplateData
public class QuteCharoption extends Tools5eQuteBase {

    /** Formatted text listing prerequisite conditions (optional) */
    public final String prerequisite;
    /** Formatted text listing prerequisite conditions, 2024-style phrasing (optional) */
    public final String prerequisite2024;
    /** Comma-separated list of option type display names (e.g. "Supernatural Gift") */
    public final String optionType;

    public QuteCharoption(CompendiumSources sources, String name, String source,
            String prerequisite, String prerequisite2024, String optionType,
            List<ImageRef> images, String text, Tags tags) {
        super(sources, name, source, images, text, tags);
        withTemplate("charoption2md.txt");
        this.prerequisite = prerequisite;
        this.prerequisite2024 = prerequisite2024;
        this.optionType = optionType;
    }
}
