package dev.ebullient.convert.tools.pf2e.qute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

public class QuteItem extends Pf2eQuteBase {

    public final List<String> traits;
    public final List<String> aliases;

    public final QuteItemActivate activate;
    public final String onset;
    public final String level;
    public final String duration;
    public final String category;
    public final String group;
    public final String hands;
    public final Map<String, String> usage;

    public QuteItem(Pf2eSources sources, List<String> text, Collection<String> tags,
            List<String> traits, List<String> aliases, QuteItemActivate activate,
            String level, String onset, String duration, String category, String group,
            String hands, Map<String, String> usage) {
        super(sources, text, tags);
        this.traits = traits;
        this.aliases = aliases;

        this.activate = activate;
        this.onset = onset;
        this.level = level;
        this.duration = duration;
        this.category = category;
        this.group = group;
        this.usage = usage;
        this.hands = hands;
    }

    @TemplateData
    public static class QuteItemActivate {
        public QuteActivityType activity;
        public String components;
        public String trigger;
        public String frequency;
        public String requirements;

        public String toString() {
            List<String> lines = new ArrayList<>();
            if (activity != null || components != null) {
                lines.add(String.join(" ", List.of(
                        activity == null ? "" : activity.toString(),
                        components == null ? "" : components)).trim());
            }
            if (frequency != null) {
                lines.add("**Frequency** " + frequency);
            }
            if (trigger != null) {
                lines.add("**Trigger** " + trigger);
            }
            if (requirements != null) {
                lines.add("**Requirements** " + requirements);
            }

            return String.join("; ", lines);
        }
    }
}
