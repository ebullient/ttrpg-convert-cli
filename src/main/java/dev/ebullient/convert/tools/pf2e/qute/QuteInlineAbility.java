package dev.ebullient.convert.tools.pf2e.qute;

import java.util.List;

import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class QuteInlineAbility extends Pf2eQuteNote {

    public final List<String> traits;
    public final String components;

    public final String trigger;
    public final String requirements;
    public final String frequency;
    public final String cost;
    public final String special;

    public final QuteActivityType activity;

    public QuteInlineAbility(String name, List<String> text, List<String> tags,
            List<String> traits, QuteActivityType activity,
            String components, String requirements,
            String cost, String trigger, String frequency, String special) {
        super(Pf2eIndexType.ability, name, null, text, tags);
        this.traits = traits;

        this.activity = activity;
        this.components = components;
        this.requirements = requirements;
        this.cost = cost;
        this.trigger = trigger;
        this.frequency = frequency;
        this.special = special;
    }

    @Override
    public String template() {
        return "inline-ability2md.txt";
    }
}
