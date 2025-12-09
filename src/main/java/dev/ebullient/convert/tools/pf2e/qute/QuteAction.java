package dev.ebullient.convert.tools.pf2e.qute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools Action attributes ({@code action2md.txt})
 *
 * Extension of {@link dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase Pf2eQuteBase}
 */
@TemplateData
public class QuteAction extends Pf2eQuteBase {

    /** Trigger for this action */
    public final String trigger;
    /** Collection of traits (decorated links) */
    public final Collection<String> traits;
    /** Situational requirements for performing this action */
    public final String requirements;
    /** Prerequisite trait or characteristic for performing this action */
    public final String prerequisites;
    /**
     * {@link dev.ebullient.convert.tools.pf2e.qute.QuteDataFrequency QuteDataFrequency}.
     * How often this action can be used/activated. Use directly to get a formatted string.
     */
    public final QuteDataFrequency frequency;
    /** The cost of using this action */
    public final String cost;
    /** Type of action (as {@link dev.ebullient.convert.tools.pf2e.qute.QuteAction.ActionType ActionType}) */
    public final ActionType actionType;
    /** Activity/Activation cost (as {@link dev.ebullient.convert.tools.pf2e.qute.QuteDataActivity QuteDataActivity}) */
    public final QuteDataActivity activity;

    private final List<String> altNames;

    public QuteAction(Pf2eSources sources, List<String> text, Tags tags,
            String cost, String trigger, List<String> aliases, Collection<String> traits,
            String prerequisites, String requirements, QuteDataFrequency frequency,
            QuteDataActivity activity, ActionType actionType) {
        super(sources, text, tags);
        this.trigger = trigger;
        this.altNames = aliases;
        this.traits = traits;

        this.prerequisites = prerequisites;
        this.requirements = requirements;
        this.cost = cost;
        this.frequency = frequency;

        this.activity = activity;
        this.actionType = actionType;
    }

    @Override
    public List<String> getAltNames() {
        // Used by getAliases in QuteBase
        return altNames;
    }

    /** True if this is a basic action. Same as `{resource.actionType.basic}`. */
    public boolean isBasic() {
        return actionType != null && actionType.basic;
    }

    /** True if this action is an item action. Same as `{resource.actionType.item}`. */
    public boolean isItem() {
        return actionType != null && actionType.item;
    }

    /**
     * Pf2eTools Action type attributes.
     *
     *
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * To use it, reference this attribute directly: `{resource.actionType}`.
     *
     */
    @TemplateData
    public static class ActionType {
        /** True if this is a basic action */
        public final boolean basic;
        /** True if this an item action */
        public final boolean item;
        /** Skills used or required by this action */
        public final String skills;
        /** List of ancestries associated with this action */
        public final List<String> ancestry;
        /** List of archetypes associated with this action */
        public final List<String> archetype;
        /** List of heritages associated with this action */
        public final List<String> heritage;
        /** List of versatile heritages associated with this action */
        public final List<String> versatileHeritage;
        /** List of classes associated with this action */
        public final List<String> classType;
        /** List of subclasses associated with this action */
        public final List<String> subclass;
        /** List of variant rules associated with this action */
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
