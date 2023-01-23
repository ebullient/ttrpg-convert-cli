package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.tools.pf2e.Pf2eSources;

public class QuteAffliction extends Pf2eQuteBase {

    public final List<String> traits;
    public final List<String> aliases;

    public final String level;
    public final String type;
    public final String temptedCurse;

    public QuteAffliction(Pf2eSources sources, List<String> text, Collection<String> tags,
            List<String> traits, List<String> aliases,
            String level, String type, String temptedCurse) {
        super(sources, text, tags);
        this.traits = traits;
        this.aliases = aliases;
        this.level = level;
        this.type = type;
        this.temptedCurse = temptedCurse;
    }

    @Override
    public boolean getHasSections() {
        return super.getHasSections() || (temptedCurse != null && !temptedCurse.isEmpty());
    }
}
