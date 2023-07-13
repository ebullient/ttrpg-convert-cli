package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;
import io.quarkus.qute.TemplateData;

@TemplateData
public class QuteAbility extends Pf2eQuteNote {

    public final Collection<String> traits;
    public final String components;

    public final String trigger;
    public final String requirements;
    public final String frequency;
    public final String cost;
    public final String special;
    public final boolean embedded;

    public final QuteDataActivity activity;

    public QuteAbility(String name, List<String> text, Tags tags,
            Collection<String> traits, QuteDataActivity activity,
            String components, String requirements,
            String cost, String trigger, String frequency, String special, boolean embedded) {
        super(Pf2eIndexType.ability, name, null, text, tags);
        this.traits = traits;

        this.activity = activity;
        this.components = components;
        this.requirements = requirements;
        this.cost = cost;
        this.trigger = trigger;
        this.frequency = frequency;
        this.special = special;
        this.embedded = embedded;
    }

    public boolean getHasBullets() {
        return getHasEffect() || isPresent(cost);
    }

    public boolean getHasEffect() {
        return isPresent(frequency) || isPresent(trigger) || isPresent(requirements);
    }

    boolean isPresent(String s) {
        return s != null && !s.isEmpty();
    }

    @Override
    public String template() {
        return "ability2md.txt";
    }
}
