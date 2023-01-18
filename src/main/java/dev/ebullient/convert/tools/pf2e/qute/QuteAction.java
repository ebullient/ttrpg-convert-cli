package dev.ebullient.convert.tools.pf2e.qute;

import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class QuteAction extends Pf2eQuteBase {

    String trigger;
    public final List<String> alias;
    public final String requirements;
    public final List<String> traits;

    public final ActivityType activity;
    public final ActionType actionType;

    public QuteAction(Pf2eSources sources, String name, String sourceText,
            String trigger, List<String> alias, List<String> traits, String requirements,
            ActivityType activity, ActionType actionType,
            String text, List<String> tags) {
        super(sources, name, sourceText, text, tags);
        this.trigger = trigger;
        this.alias = alias;
        this.traits = traits;
        this.requirements = requirements;

        this.activity = activity;
        this.actionType = actionType;
    }

    public ImageRef getGlyph() {
        return activity == null ? null : activity.glyph;
    }

    @Override
    public List<ImageRef> images() {
        return activity == null ? List.of() : activity.image();
    }

    @RegisterForReflection
    public static class ActionType {
        public final boolean basic;
        public final boolean item;
        public final List<String> skills;
        public final List<String> ancestry;
        public final List<String> archetype;
        public final List<String> heritage;
        public final List<String> versatileHeritage;
        public final List<String> classType;
        public final List<String> subclass;
        public final List<String> variantrule;

        public ActionType(
                boolean basic, boolean item, List<String> skills,
                List<String> ancestry, List<String> archetype,
                List<String> heritage, List<String> versatileHeritage,
                List<String> classType, List<String> subclass,
                List<String> variantrule) {
            this.basic = basic;
            this.item = item;
            this.skills = skills;
            this.ancestry = ancestry;
            this.archetype = archetype;
            this.heritage = heritage;
            this.versatileHeritage = versatileHeritage;
            this.classType = classType;
            this.subclass = subclass;
            this.variantrule = variantrule;
        }

        public String toString() {
            return "ME, TOO!";
        }
    }

    @RegisterForReflection
    public static class ActivityType {
        public final ImageRef glyph;
        public final String text;
        public final String textGlyph;

        public ActivityType(String text, ImageRef glyph, String textGlyph) {
            this.glyph = glyph;
            this.text = text;
            this.textGlyph = textGlyph;
        }

        public String toString() {
            return "ME!";
        }

        public List<ImageRef> image() {
            return glyph == null ? List.of() : List.of(glyph);
        }
    }
}
