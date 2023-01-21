package dev.ebullient.convert.tools.pf2e.qute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class QuteSpell extends Pf2eQuteBase {

    public final String level;
    public final String spellType;
    public final List<String> traits;
    public final List<String> aliases;

    public final Casting casting;
    public final Targeting targeting;
    public final SaveDuration saveDuration;

    public final List<String> domains;
    public final List<String> traditions;
    public final List<String> spellLists;

    public final Subclass subclass;
    public final Map<String, String> heightened;

    public QuteSpell(Pf2eSources sources, String text, Collection<String> tags,
            String level, String spellType, List<String> traits, List<String> aliases,
            Casting casting, Targeting targeting, SaveDuration saveDuration,
            List<String> domains, List<String> traditions, List<String> spellLists,
            Subclass subclass, Map<String, String> heightened) {
        super(sources, sources.getName(), sources.getSourceText(), text, tags);

        this.level = level;
        this.spellType = spellType;
        this.traits = traits;
        this.aliases = aliases;
        this.casting = casting;
        this.targeting = targeting;
        this.saveDuration = saveDuration;
        this.domains = domains;
        this.traditions = traditions;
        this.spellLists = spellLists;
        this.subclass = subclass;
        this.heightened = heightened;
    }

    @RegisterForReflection
    public static class Casting {
        public String cast;
        public List<String> components;
        public String cost;
        public String trigger;
        public String requirements;

        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append(cast);
            if (components != null) {
                sb.append(" ").append(String.join(", ", components));
            }

            if (cost != null) {
                sb.append("; **Cost** ").append(cost);
            }
            if (trigger != null) {
                sb.append("; **Trigger** ").append(trigger);
            }
            if (requirements != null) {
                sb.append("; **Requirements** ").append(requirements);
            }

            return sb.toString();
        }
    }

    @RegisterForReflection
    public static class SaveDuration {
        public boolean basic;
        public boolean hidden;
        public String savingThrow;
        public String duration;

        public String toString() {
            List<String> parts = new ArrayList<>();
            if (savingThrow != null && !hidden) {
                parts.add(String.format("**Saving Throw** %s%s",
                        basic ? " basic" : "",
                        savingThrow));
            }
            if (duration != null) {
                parts.add(String.format("**Duration** %s", duration));
            }
            return String.join("; ", parts);
        }
    }

    @RegisterForReflection
    public static class Subclass {
        public String category;
        public String text;
    }

    @RegisterForReflection
    public static class Targeting {
        public String range;
        public String area;
        public String targets;

        public String toString() {
            List<String> parts = new ArrayList<>();
            if (range != null) {
                parts.add(String.format("**Range** %s", range));
            }
            if (area != null) {
                parts.add(String.format("**Area** %s", area));
            }
            if (targets != null) {
                parts.add(String.format("**Targets** %s", targets));
            }
            return String.join("; ", parts);
        }
    }

    @RegisterForReflection
    public static class Amp {
        public String text;
        public Map<String, String> ampEffects;
    }
}
