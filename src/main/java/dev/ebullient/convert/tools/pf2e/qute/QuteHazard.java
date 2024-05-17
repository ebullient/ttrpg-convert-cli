package dev.ebullient.convert.tools.pf2e.qute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.qute.QuteUtil;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Pf2eTools Hazard attributes ({@code hazard2md.txt})
 * <p>
 * Hazards are rendered both standalone and inline (as an admonition block).
 * The default template can render both. It contains
 * some special syntax to handle the inline case.
 * </p>
 * <p>
 * Use `%%--` to mark the end of the preamble (frontmatter and
 * other leading content only appropriate to the standalone case).
 * </p>
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase Pf2eQuteBase}
 * </p>
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
    public final List<String> abilities;
    public final List<String> actions;
    public final QuteHazardAttributes stealth;
    public final QuteHazardAttributes perception;

    public QuteHazard(Pf2eSources sources, List<String> text, Tags tags,
            Collection<String> traits, String level, String disable,
            String reset, String routine, QuteDataDefenses defenses,
            List<QuteInlineAttack> attacks, List<String> abilities, List<String> actions,
            QuteHazardAttributes stealth, QuteHazardAttributes perception) {
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
     * <p>
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * </p>
     */
    @TemplateData
    @RegisterForReflection
    public static class QuteHazardAttributes implements QuteUtil {
        /** Number. Difficulty class */
        public Integer dc;
        /** Number. Bonus */
        public Integer bonus;
        /** String. Minimum proficiency */
        public String minProf;
        /** Formatted string. Notes */
        public String notes;

        public String toString() {
            List<String> pieces = new ArrayList<>();
            if (dc != null) {
                pieces.add("DC " + dc);
            } else if (bonus != null) {
                pieces.add((bonus >= 0 ? "+" : "") + bonus);
            }
            if (isPresent(minProf)) {
                pieces.add(minProf);
            }
            if (isPresent(notes)) {
                pieces.add(notes);
            }
            return String.join(" ", pieces);
        }
    }
}
