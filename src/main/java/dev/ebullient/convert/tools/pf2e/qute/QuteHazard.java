package dev.ebullient.convert.tools.pf2e.qute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
public class QuteHazard extends Pf2eQuteBase {

    public final Collection<String> traits;

    public final String level;
    public final String disable;
    public final String reset;
    public final String routine;
    public final QuteDataDefenses defenses;

    public final List<String> abilities;
    public final List<String> actions;
    public final QuteHazardAttributes stealth;
    public final QuteHazardAttributes perception;

    public QuteHazard(Pf2eSources sources, List<String> text, Collection<String> tags,
            Collection<String> traits, String level, String disable,
            String reset, String routine, QuteDataDefenses defenses,
            List<String> abilities, List<String> actions,
            QuteHazardAttributes stealth, QuteHazardAttributes perception) {
        super(sources, text, tags);
        this.traits = traits;
        this.level = level;
        this.reset = reset;
        this.routine = routine;
        this.disable = disable;
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
        return convertToEmbed(routine, "Routine", "summary");
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
                + "title: " + title + "\n"
                + content + "\n"
                + backticks;
    }

    @TemplateData
    @RegisterForReflection
    public static class QuteHazardAttributes {
        public Integer dc;
        public Integer bonus;
        public String minProf;
        public String notes;

        public String toString() {
            List<String> pieces = new ArrayList<>();
            if (dc != null) {
                pieces.add("DC " + dc);
            } else if (bonus != null) {
                pieces.add((bonus >= 0 ? "+" : "") + bonus);
            }
            if (minProf != null) {
                pieces.add(minProf);
            }
            if (notes != null) {
                pieces.add(notes);
            }
            return String.join(" ", pieces);
        }
    }
}
