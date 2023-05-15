package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

@TemplateData
public class QuteFeat extends Pf2eQuteBase {

    public final Collection<String> traits;
    public final List<String> aliases;

    public final String level;
    public final String access;
    public final String frequency;
    public final QuteDataActivity activity;
    public final String trigger;
    public final String cost;
    public final String requirements;
    public final String prerequisites;
    public final String special;
    public final List<String> leadsTo;
    public final String note;
    public final boolean embedded;

    public QuteFeat(Pf2eSources sources, List<String> text, Collection<String> tags,
            Collection<String> traits, List<String> aliases,
            String level, String access, String frequency, QuteDataActivity activity, String trigger,
            String cost, String requirements, String prerequisites, String special, String note,
            List<String> leadsTo, boolean embedded) {
        super(sources, text, tags);
        this.traits = traits;
        this.aliases = aliases;

        this.level = level;
        this.access = access;
        this.frequency = frequency;
        this.activity = activity;
        this.trigger = trigger;
        this.cost = cost;
        this.requirements = requirements;
        this.prerequisites = prerequisites;
        this.special = special;
        this.note = note;
        this.leadsTo = leadsTo;
        this.embedded = embedded;
    }

    @Override
    public boolean getHasSections() {
        return super.getHasSections() || (leadsTo != null && !leadsTo.isEmpty());
    }

    public String title() {
        return String.format("%s%s, *Feat %s*", getName(),
                activity == null
                        ? ""
                        : " " + activity,
                level);
    }
}
