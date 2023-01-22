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

    public final QuteSpellCasting casting;
    public final QuteSpellTarget targeting;
    public final QuteSpellSaveDuration saveDuration;
    public final QuteSpellAmp amp;

    public final List<String> domains;
    public final List<String> traditions;
    public final List<String> spellLists;

    public final List<QuteSpellSubclass> subclass;
    public final Map<String, String> heightened;

    public QuteSpell(Pf2eSources sources, String text, Collection<String> tags,
            String level, String spellType, List<String> traits, List<String> aliases,
            QuteSpellCasting casting, QuteSpellTarget targeting, QuteSpellSaveDuration saveDuration,
            List<String> domains, List<String> traditions, List<String> spellLists,
            List<QuteSpellSubclass> subclass, Map<String, String> heightened, QuteSpellAmp amp) {
        super(sources, text, tags);

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
        this.amp = amp;
    }

    @Override
    public boolean getHasSections() {
        return super.getHasSections() || amp != null;
    }

    @RegisterForReflection
    public static class QuteSpellCasting {
        public String cast;
        public List<String> components;
        public String cost;
        public String trigger;
        public String requirements;

        public String toString() {
            List<String> parts = new ArrayList<>();

            parts.add(cast + (components != null && !components.isEmpty()
                    ? ""
                    : " " + String.join(", ", components)));

            if (cost != null) {
                parts.add(String.format("**Cost** %s", cost));
            }
            if (trigger != null) {
                parts.add(String.format("**Trigger** %s", trigger));
            }
            if (requirements != null) {
                parts.add(String.format("**Requirements** %s", requirements));
            }

            return String.join("\n- ", parts);
        }
    }

    @RegisterForReflection
    public static class QuteSpellSaveDuration {
        public boolean basic;
        public String savingThrow;
        public String duration;

        public String toString() {
            List<String> parts = new ArrayList<>();
            if (savingThrow != null) {
                parts.add(String.format("**Saving Throw** %s%s",
                        basic ? " basic " : "",
                        savingThrow));
            }
            if (duration != null) {
                parts.add(String.format("**Duration** %s", duration));
            }
            return String.join("\n- ", parts);
        }
    }

    @RegisterForReflection
    public static class QuteSpellSubclass {
        public String category;
        public String text;
    }

    @RegisterForReflection
    public static class QuteSpellTarget {
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
            return String.join("\n- ", parts);
        }
    }

    @RegisterForReflection
    public static class QuteSpellAmp {
        public String text;
        public Map<String, String> ampEffects;
    }
}
