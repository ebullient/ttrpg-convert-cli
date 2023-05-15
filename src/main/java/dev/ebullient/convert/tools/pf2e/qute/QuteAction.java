package dev.ebullient.convert.tools.pf2e.qute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

@TemplateData
public class QuteAction extends Pf2eQuteBase {

    public final String trigger;
    public final List<String> aliases;
    public final Collection<String> traits;

    public final String requirements;
    public final String prerequisites;
    public final String frequency;
    public final String cost;

    public final ActionType actionType;
    public final QuteDataActivity activity;

    public QuteAction(Pf2eSources sources, List<String> text, Collection<String> tags,
            String cost, String trigger, List<String> aliases, Collection<String> traits,
            String prerequisites, String requirements, String frequency,
            QuteDataActivity activity, ActionType actionType) {
        super(sources, text, tags);
        this.trigger = trigger;
        this.aliases = aliases;
        this.traits = traits;

        this.prerequisites = prerequisites;
        this.requirements = requirements;
        this.cost = cost;
        this.frequency = frequency;

        this.activity = activity;
        this.actionType = actionType;
    }

    public boolean isBasic() {
        return actionType == null ? false : actionType.basic;
    }

    public boolean isItem() {
        return actionType == null ? false : actionType.item;
    }

    @TemplateData
    public static class ActionType {
        public final boolean basic;
        public final boolean item;
        public final String skills;
        public final List<String> ancestry;
        public final List<String> archetype;
        public final List<String> heritage;
        public final List<String> versatileHeritage;
        public final List<String> classType;
        public final List<String> subclass;
        public final List<String> variantrule;

        public ActionType(
                boolean basic, boolean item, String skills,
                List<String> classType, List<String> subclass, List<String> archetype,
                List<String> ancestry, List<String> heritage, List<String> versatileHeritage,
                List<String> variantrule) {
            this.basic = basic;
            this.item = item;
            this.skills = skills;
            this.archetype = archetype;
            this.ancestry = ancestry;
            this.heritage = heritage;
            this.versatileHeritage = versatileHeritage;
            this.classType = classType;
            this.subclass = subclass;
            this.variantrule = variantrule;
        }

        public String toString() {
            List<String> entries = new ArrayList<>();

            // skill (trained, untrained, expert, legendary)
            if (skills != null && !skills.isEmpty()) {
                entries.add("**Skill** " + skills);
            }

            if (classType != null && !classType.isEmpty()) {
                List<String> inner = new ArrayList<>();
                inner.add("**Class** " + String.join(", ", classType));
                if (subclass != null && !subclass.isEmpty()) {
                    inner.add("**Subclass** " + String.join(", ", subclass));
                }
                entries.add(String.join("; ", inner));
            }

            if (archetype != null && !archetype.isEmpty()) {
                entries.add("**Archetype** " + String.join(", ", archetype));
            }

            if (ancestry != null && !ancestry.isEmpty()) {
                List<String> inner = new ArrayList<>();
                inner.add("**Ancestry** " + String.join(", ", ancestry));
                if (heritage != null && !heritage.isEmpty()) {
                    inner.add("**Heritage** " + String.join(", ", heritage));
                }
                if (versatileHeritage != null && !versatileHeritage.isEmpty()) {
                    inner.add("**Versatile Heritage** " + String.join(", ", versatileHeritage));
                }
                entries.add(String.join("; ", inner));
            }

            if (variantrule != null && !variantrule.isEmpty()) {
                entries.add("**Variant Rule** " + String.join(", ", variantrule));
            }
            return String.join("\n", entries);
        }
    }
}
