package dev.ebullient.convert.tools.pf2e.qute;

import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.parenthesize;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools Hazard attributes ({@code hazard2md.txt})
 *
 * Hazards are rendered both standalone and inline (as an admonition block).
 * The default template can render both.
 * It uses special syntax to handle the inline case.
 *
 * Use `%%--` to mark the end of the preamble (frontmatter and
 * other leading content only appropriate to the standalone case).
 *
 * Extension of {@link dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase Pf2eQuteBase}
 */
@TemplateData
public class QuteHazard extends Pf2eQuteBase {

    /** Collection of traits (decorated links) */
    public final Collection<String> traits;

    public final String level;
    public final String disable;
    public final String reset;
    public final String routine;
    public final QuteDataDefenses defenses;

    /**
     * The attacks available to the hazard, as a list of
     * {@link dev.ebullient.convert.tools.pf2e.qute.QuteInlineAttack QuteInlineAttack}
     */
    public final List<QuteInlineAttack> attacks;

    /**
     * The hazard's abilities, as a list of
     * {@link dev.ebullient.convert.tools.pf2e.qute.QuteAbility QuteAbility}
     */
    public final List<QuteAbility> abilities;

    /**
     * The hazard's actions, as a list of
     * {@link dev.ebullient.convert.tools.pf2e.qute.QuteAbilityOrAffliction QuteAbilityOrAffliction}.
     *
     * Using the elements directly will give a default rendering, but if you want more
     * control you can use {@code isAffliction} and {@code isAbility} to check whether it's an affliction or an
     * ability. Example:
     *
     *
     * ```md
     * {#each resource.actions}
     * {#if it.isAffliction}
     *
     * **Affliction** {it}
     * {#else if it.isAbility}
     *
     * **Ability** {it}
     * {/if}
     * {/each}
     * ```
     */
    public final List<QuteAbilityOrAffliction> actions;

    /**
     * The hazard's stealth, as a
     * {@link dev.ebullient.convert.tools.pf2e.qute.QuteHazard.QuteHazardStealth QuteHazardAttributes}
     */
    public final QuteHazardStealth stealth;

    /**
     * The hazard's perception, as a
     * {@link dev.ebullient.convert.tools.pf2e.qute.QuteDataGenericStat QuteDataGenericStat}
     */
    public final QuteDataGenericStat perception;

    public QuteHazard(Pf2eSources sources, List<String> text, Tags tags,
            Collection<String> traits, String level, String disable,
            String reset, String routine, QuteDataDefenses defenses,
            List<QuteInlineAttack> attacks, List<QuteAbility> abilities, List<QuteAbilityOrAffliction> actions,
            QuteHazardStealth stealth, QuteDataGenericStat perception) {
        super(sources, text, tags);
        this.traits = traits;
        this.level = level;
        this.reset = reset;
        this.routine = routine;
        this.disable = disable;
        this.attacks = attacks;
        this.abilities = abilities;
        this.actions = actions;
        this.defenses = defenses;
        this.stealth = stealth;
        this.perception = perception;
    }

    public String getComplexity() {
        if (traits == null || traits.stream().noneMatch(t -> t.contains("complex"))) {
            return "Simple";
        }
        return "Complex";
    }

    public String getRoutineAdmonition() {
        return convertToEmbed(routine, "Routine", "pf2-summary");
    }

    public String convertToEmbed(String content, String title, String admonition) {
        int embedDepth = Arrays.stream(content.split("\n"))
                .filter(s -> s.matches("^`+$"))
                .map(String::length)
                .max(Integer::compare).orElse(2);
        char[] ticks = new char[embedDepth + 1];
        Arrays.fill(ticks, '`');
        String backticks = new String(ticks);

        return backticks + "ad-" + admonition + "\n"
                + "title: " + title + "\n\n"
                + content + "\n"
                + backticks;
    }

    /**
     * Pf2eTools hazard attributes.
     *
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     *
     * @param value The hazard's Stealth bonus
     * @param minProf The minimum Perception proficiency required to be able to roll against the hazard's Stealth
     * @param notes Any notes associated with the hazard's Stealth. Sometimes this includes other stats which may
     *        be rolled against the hazard's Stealth.
     * @param dc The DC which must be passed to see the hazard
     */
    @TemplateData
    public record QuteHazardStealth(
            Integer value, Integer dc, String minProf, List<String> notes) implements QuteDataGenericStat {

        public QuteHazardStealth(Integer value, Integer dc, String minProf, String note) {
            this(value, dc, minProf, note == null || note.isEmpty() ? List.of() : List.of(note));
        }

        @Override
        public String formattedNotes() {
            return join(" ", parenthesize(minProf), join(", ", notes));
        }

        @Override
        public String toString() {
            return join(" ", bonus(), dc != null ? "DC " + dc : "", formattedNotes());
        }
    }
}
