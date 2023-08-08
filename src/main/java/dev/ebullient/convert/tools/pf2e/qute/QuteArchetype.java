package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools Archetype attributes ({@code archetype2md.txt})
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase Pf2eQuteBase}
 * </p>
 */
@TemplateData
public class QuteArchetype extends Pf2eQuteBase {

    /** Collection of traits (decorated links) */
    public final Collection<String> traits;

    public final int dedicationLevel;
    public final List<String> benefits;
    public final List<String> feats;

    public QuteArchetype(Pf2eSources sources, List<String> text, Tags tags,
            Collection<String> traits, int dedicationLevel, List<String> benefits, List<String> feats) {
        super(sources, text, tags);

        this.traits = traits;
        this.dedicationLevel = dedicationLevel;
        this.benefits = benefits;
        this.feats = feats;
    }
}
