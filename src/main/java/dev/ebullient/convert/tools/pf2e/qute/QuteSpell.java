package dev.ebullient.convert.tools.pf2e.qute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.qute.QuteUtil;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools Spell attributes ({@code spell2md.txt})
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase Pf2eQuteBase}
 * </p>
 */
@TemplateData
public class QuteSpell extends Pf2eQuteBase {

    /** A spell’s overall power, from 1 to 10. */
    public final String level;
    /** Type: spell, cantrip, or focus */
    public final String spellType;
    /** Collection of traits (decorated links) */
    public final Collection<String> traits;
    /** Aliases for this note */
    public final List<String> aliases;
    /**
     * Spell casting attributes (trigger, duration, etc.) as
     * {@link dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellCasting QuteSpellCasting}
     */
    public final QuteSpellCasting casting;
    /** Spell target attributes as {@link dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellTarget QuteSpellTarget} */
    public final QuteSpellTarget targeting;
    /**
     * Spell save and duration attributes as {@link dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellSaveDuration
     * QuteSpellSaveDuration}
     */
    public final QuteSpellSaveDuration saveDuration;
    /** Psi amp behavior as {@link dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellAmp QuteSpellAmp} */
    public final QuteSpellAmp amp;
    /** List of spell domains (links) */
    public final List<String> domains;
    /** List of spell traditions (trait links) */
    public final List<String> traditions;
    /** Spell lists containing this spell */
    public final List<String> spellLists;

    /**
     * List of category (Bloodline or Mystery) to Subclass (Sorcerer or Oracle). Link to class (if present)
     * as a list of {@link dev.ebullient.convert.qute.NamedText NamedText}.
     */
    public final Collection<NamedText> subclass;
    /** Heightened spell effects as a list of {@link dev.ebullient.convert.qute.NamedText NamedText} */
    public final Collection<NamedText> heightened;

    public QuteSpell(Pf2eSources sources, List<String> text, Tags tags,
            String level, String spellType, Collection<String> traits, List<String> aliases,
            QuteSpellCasting casting, QuteSpellTarget targeting, QuteSpellSaveDuration saveDuration,
            List<String> domains, List<String> traditions, List<String> spellLists,
            Collection<NamedText> subclass, Collection<NamedText> heightened, QuteSpellAmp amp) {
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

    /**
     * True if the spell text has sections or Amp text
     */
    @Override
    public boolean getHasSections() {
        return super.getHasSections() || amp != null;
    }

    /**
     * Pf2eTools spell casting attributes.
     * <p>
     * This attribute will render itself as a list of labeled elements
     * if you reference it directly: `{resource.casting}`.
     * </p>
     */
    @TemplateData
    public static class QuteSpellCasting implements QuteUtil {
        /** Formatted action icon/link. Casting action */
        public String cast;
        /** Comma-separated list of required spell components (material, somatic, verbal, focus) */
        public List<String> components;
        /** Formatted string. Material cost of the spell */
        public String cost;
        /** Formatted string. Spell activation trigger. */
        public String trigger;
        /** Formatted string. Spell cast requirements */
        public String requirements;

        public String toString() {
            List<String> parts = new ArrayList<>();

            parts.add(cast + (components != null && !components.isEmpty()
                    ? ""
                    : " " + String.join(", ", components)));

            if (isPresent(cost)) {
                parts.add("**Cost**: " + cost);
            }
            if (isPresent(trigger)) {
                parts.add("**Trigger**: " + trigger);
            }
            if (isPresent(requirements)) {
                parts.add("**Requirements**: " + requirements);
            }

            return String.join("\n- ", parts);
        }
    }

    /**
     * Pf2eTools spell save attributes.
     * <p>
     * This attribute will render itself as labeled elements
     * if you reference it directly: `{resource.saveDuration}`.
     * </p>
     */
    @TemplateData
    public static class QuteSpellSaveDuration implements QuteUtil {
        /** Boolean. True if this is a basic saving throw */
        public boolean basic;
        /** Formatted string. Saving throw */
        public String savingThrow;
        /** Formatted string. Duration. */
        public String duration;

        public String toString() {
            List<String> parts = new ArrayList<>();
            if (isPresent(savingThrow)) {
                parts.add(String.format("**Saving Throw**: %s%s",
                        basic ? " basic " : "",
                        savingThrow));
            }
            if (isPresent(duration)) {
                parts.add("**Duration**: " + duration);
            }
            return String.join("\n- ", parts);
        }
    }

    /**
     * Pf2eTools spell target attributes.
     * <p>
     * This attribute will render itself as labeled elements
     * if you reference it directly: `{resource.targeting}`.
     * </p>
     */
    @TemplateData
    public static class QuteSpellTarget implements QuteUtil {
        /** Formatted string describing the spell range */
        public String range;
        /** Formatted string describing the spell area of effect */
        public String area;
        /** Formatted string describing the spell target(s) */
        public String targets;

        public String toString() {
            List<String> parts = new ArrayList<>();
            if (isPresent(range)) {
                parts.add("**Range**: " + range);
            }
            if (isPresent(area)) {
                parts.add("**Area**: " + area);
            }
            if (isPresent(targets)) {
                parts.add("**Targets**: " + targets);
            }
            return String.join("\n- ", parts);
        }
    }

    /**
     * Pf2eTools spell Amp attributes
     * <p>
     * This attribute will render itself as labeled elements
     * if you reference it directly: `{resource.amp}`.
     * </p>
     */
    @TemplateData
    public static class QuteSpellAmp implements QuteUtil {
        /** Formatted text describing amp effects */
        public String text;
        /** Heightened amp effects as a list of {@link dev.ebullient.convert.qute.NamedText Traits} */
        public Collection<NamedText> ampEffects;

        public String toString() {
            List<String> parts = new ArrayList<>();
            if (isPresent(text)) {
                parts.add(text);
            }
            if (isPresent(ampEffects)) {
                for (NamedText entry : ampEffects) {
                    parts.add("**" + entry.name + "** " + entry.desc);
                }
            }

            return String.join("\n\n", parts);
        }
    }
}
