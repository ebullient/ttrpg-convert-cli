package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.tools.pf2e.Pf2eSources;

public class QuteAffliction extends Pf2eQuteBase {

    public final Collection<String> traits;
    public final List<String> aliases;

    public final String level;
    public final String affliction;
    public final String temptedCurse;

    public QuteAffliction(Pf2eSources sources, List<String> text, Collection<String> tags,
            Collection<String> traits, List<String> aliases,
            String level, String affliction, String temptedCurse) {
        super(sources, text, tags);
        this.traits = traits;
        this.aliases = aliases;
        this.level = level;
        this.affliction = affliction;
        this.temptedCurse = temptedCurse;
    }

    @Override
    public boolean getHasSections() {
        return (temptedCurse != null && !temptedCurse.isEmpty());
    }

    public String title() {
        return String.format("%s, _%s %s_", getName(), affliction, level);
    }
}
