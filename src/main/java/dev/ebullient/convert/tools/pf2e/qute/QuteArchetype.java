package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools Archetype attributes ({@code archetype2md.txt})
 *
 * Extension of {@link dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase Pf2eQuteBase}
 */
@TemplateData
public class QuteArchetype extends Pf2eQuteBase {

    /** Collection of traits (collection of {@link QuteDataRef}) */
    public final Collection<QuteDataRef> traits;

    public final int dedicationLevel;
    public final List<String> benefits;
    public final List<String> feats;

    public QuteArchetype(Pf2eSources sources, List<String> text, Tags tags,
            Collection<QuteDataRef> traits, int dedicationLevel, List<String> benefits, List<String> feats) {
        super(sources, text, tags);

        this.traits = traits;
        this.dedicationLevel = dedicationLevel;
        this.benefits = benefits;
        this.feats = feats;
    }
}
