package dev.ebullient.convert.tools.pf2e.qute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.qute.QuteUtil;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellTarget;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools Ritual attributes ({@code ritual2md.txt})
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase Pf2eQuteBase}
 * </p>
 */
@TemplateData
public class QuteRitual extends Pf2eQuteBase {

    /** A spell’s overall power, from 1 to 10. */
    public final String level;
    /** Type: Ritual (usually) */
    public final String ritualType;
    /** Collection of traits (decorated links) */
    public final Collection<String> traits;
    /** Aliases for this note */
    public final List<String> aliases;

    /** Casting attributes as {@link dev.ebullient.convert.tools.pf2e.qute.QuteRitual.QuteRitualCasting QuteRitualCasting} */
    public final QuteRitualCasting casting;
    /** Casting attributes as {@link dev.ebullient.convert.tools.pf2e.qute.QuteRitual.QuteRitualChecks QuteRitualChecks} */
    public final QuteRitualChecks checks;
    /** Casting attributes as {@link dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellTarget QuteSpellTarget} */
    public final QuteSpellTarget targeting;

    /** Formatted text. Ritual requirements */
    public final String requirements;
    /** Formated text. Ritual duration */
    public final String duration;

    /** Heightened spell effects as a list of {@link dev.ebullient.convert.qute.NamedText Traits} */
    public final Collection<NamedText> heightened;

    public QuteRitual(Pf2eSources sources, List<String> text, Tags tags,
            String level, String ritualType, Collection<String> traits, List<String> aliases,
            QuteRitualCasting casting, QuteRitualChecks checks, QuteSpellTarget targeting,
            String requirements, String duration, Collection<NamedText> heightened) {
        super(sources, text, tags);

        this.level = level;
        this.ritualType = ritualType;
        this.traits = traits;
        this.aliases = aliases;
        this.casting = casting;
        this.checks = checks;
        this.targeting = targeting;
        this.requirements = requirements;
        this.duration = duration;
        this.heightened = heightened;
    }

    /**
     * Pf2eTools ritual casting attributes
     *
     * <p>
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * To use it, reference it directly: `{resource.casting}`.
     * </p>
     */
    @TemplateData
    public static class QuteRitualCasting implements QuteUtil {
        /** Formatted action icon/link. Casting action */
        public String cast;
        /** Formatted string. Material cost of the spell */
        public String cost;
        /** Minumum number of secondary casters required */
        public String secondaryCasters;

        public String toString() {
            List<String> parts = new ArrayList<>();

            parts.add(cast);

            if (isPresent(cost)) {
                parts.add(String.format("**Cost** %s", cost));
            }
            if (isPresent(secondaryCasters)) {
                parts.add(String.format("**Secondary Casters** %s", secondaryCasters));
            }

            return String.join("\n- ", parts);
        }
    }

    /**
     * Pf2eTools ritual check attributes
     *
     * <p>
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * To use it, reference it directly: `{resource.checks}`.
     * </p>
     */
    @TemplateData
    public static class QuteRitualChecks implements QuteUtil {
        /** Formatted string. Links to skills for primary checks */
        public String primaryChecks;
        /** Formatted string. Links to skills for secondary checks */
        public String secondaryChecks;

        public String toString() {
            List<String> parts = new ArrayList<>();
            parts.add(String.format("**Primary Checks** %s", primaryChecks));

            if (isPresent(secondaryChecks)) {
                parts.add(String.format("**Secondary Checks** %s", secondaryChecks));
            }

            return String.join("\n- ", parts);
        }
    }

}
