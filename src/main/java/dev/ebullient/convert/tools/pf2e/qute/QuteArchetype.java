package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.tools.pf2e.Pf2eSources;

public class QuteArchetype extends Pf2eQuteBase {

    public final List<String> traits;

    public final int dedicationLevel;
    public final List<String> benefits;
    public final List<String> extraFeats;

    public QuteArchetype(Pf2eSources sources, List<String> text, Collection<String> tags,
            List<String> traits, int dedicationLevel, List<String> benefits, List<String> extraFeats) {
        super(sources, text, tags);

        this.traits = traits;
        this.dedicationLevel = dedicationLevel;
        this.benefits = benefits;
        this.extraFeats = extraFeats;
    }

    @Override
    public boolean getHasSections() {
        return super.getHasSections() || (extraFeats != null && !extraFeats.isEmpty());
    }
}
