package dev.ebullient.convert.tools.pf2e.qute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.qute.QuteUtil;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.joinConjunct;

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
     * The time it takes to cast the spell, as a {@link QuteDataDuration} which is either a {@link QuteDataActivity}
     * or a {@link QuteDataTimedDuration}.
     */
    public final QuteDataDuration castDuration;
    /**
     * The required spell components as a list of formatted strings (maybe empty). Use
     * {@link dev.ebullient.convert.tools.pf2e.qute.QuteSpell#formattedComponents()}
     * to get a pre-formatted representation.
     */
    public final List<String> components;
    /** The material cost of the spell as a formatted string (optional) */
    public final String cost;
    /** The activation trigger for the spell as a formatted string (optional) */
    public final String trigger;
    /** The requirements to cast the spell (optional) */
    public final String requirements;
    /** Spell target attributes as {@link dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellTarget QuteSpellTarget} */
    public final QuteSpellTarget targeting;
    /** Spell save, as {@link dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellSave} */
    public final QuteSpellSave save;
    /** Spell duration, as {@link QuteDataTimedDuration} */
    public final QuteSpellDuration duration;
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
            QuteDataDuration castDuration, List<String> components, String cost, String trigger, String requirements,
            QuteSpellTarget targeting, QuteSpellSave save, QuteSpellDuration duration,
            List<String> domains, List<String> traditions, List<String> spellLists,
            Collection<NamedText> subclass, Collection<NamedText> heightened, QuteSpellAmp amp) {
        super(sources, text, tags);

        this.level = level;
        this.spellType = spellType;
        this.traits = traits;
        this.aliases = aliases;
        this.castDuration = castDuration;
        this.components = components;
        this.cost = cost;
        this.trigger = trigger;
        this.requirements = requirements;
        this.targeting = targeting;
        this.save = save;
        this.duration = duration;
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
     * The components required for the spell, as a formatted string. Example:
     * <blockquote>
     * <a href="#">somatic</a>, <a href="#">verbal</a>
     * </blockquote>
     */
    public String formattedComponents() {
        return join(", ", components);
    }

    /**
     * Details about the saving throw for a spell. Example default representations:
     * <blockquote>basic Reflex or Fortitude</blockquote>
     * <blockquote>basic Reflex, Fortitude, or Willpower</blockquote>
     *
     * @param saves The saving throws that can be used for this spell (list of strings)
     * @param basic True if this is a basic save (boolean)
     * @param hidden Whether this save should be hidden. This is sometimes true when it's a special save that is
     *        described in the text of the spell.
     */
    @TemplateData
    public record QuteSpellSave(List<String> saves, boolean basic, boolean hidden) implements QuteUtil {

        @Override
        public String toString() {
            return join(" ", basic ? "basic" : "", joinConjunct(" or ", saves));
        }
    }

    /**
     * Details about the duration of the spell. Example default representations:
     * <blockquote>1 minute</blockquote>
     * <blockquote>sustained up to 1 minute</blockquote>
     *
     * @param sustained Whether this is a sustained spell, boolean
     * @param dismissable Whether this spell can be dismissed, boolean. Not included in the default representation.
     * @param duration The duration of this spell, as a {@link QuteDataTimedDuration}.
     */
    @TemplateData
    public record QuteSpellDuration(
            QuteDataTimedDuration duration, boolean sustained, boolean dismissable) implements QuteUtil {
        @Override
        public String toString() {
            if (duration != null && duration.hasCustomDisplay()) {
                return duration.toString();
            }
            if (sustained && (duration == null || duration.unit() == QuteDataTimedDuration.DurationUnit.UNLIMITED)) {
                return "sustained";
            }
            if (duration == null) {
                return "";
            }
            return (sustained ? "sustained up to " : "") + duration;
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
        /** The spell's range, as a {@link QuteDataRange}. */
        public QuteDataRange range;
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
