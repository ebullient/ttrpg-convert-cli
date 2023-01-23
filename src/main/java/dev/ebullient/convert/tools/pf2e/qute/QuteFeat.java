package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.tools.pf2e.Pf2eSources;

public class QuteFeat extends Pf2eQuteBase {

    public final List<String> traits;
    public final List<String> aliases;

    public final String level;
    public final String access;
    public final String frequency;
    public final String trigger;
    public final String cost;
    public final String requirements;
    public final String prerequisites;
    public final List<String> leadsTo;

    public QuteFeat(Pf2eSources sources, List<String> text, Collection<String> tags,
            List<String> traits, List<String> aliases,
            String level, String access, String frequency, String trigger,
            String cost, String requirements, String prerequisites,
            List<String> leadsTo) {
        super(sources, text, tags);
        this.traits = traits;
        this.aliases = aliases;

        this.level = level;
        this.access = access;
        this.frequency = frequency;
        this.trigger = trigger;
        this.cost = cost;
        this.requirements = requirements;
        this.prerequisites = prerequisites;
        this.leadsTo = leadsTo;
    }

    @Override
    public boolean getHasSections() {
        return super.getHasSections() || (leadsTo != null && !leadsTo.isEmpty());
    }
}
