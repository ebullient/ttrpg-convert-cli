package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import dev.ebullient.convert.qute.QuteNote;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class QuteInlineAffliction extends QuteNote {

    public final List<String> traits;
    public final String level;
    public final String maxDuration;
    public final String onset;
    public final String savingThrow;
    public final String effect;
    public final Map<String, QuteAfflictionStage> stages;

    public QuteInlineAffliction(String name, List<String> text, Collection<String> tags,
            List<String> traits, String level,
            String maxDuration, String onset, String savingThrow,
            String effect, Map<String, QuteAfflictionStage> stages) {
        super(name, null, text, tags);

        this.level = level;
        this.traits = traits;
        this.maxDuration = maxDuration;
        this.onset = onset;
        this.savingThrow = savingThrow;
        this.effect = effect;
        this.stages = stages;
    }

    @Override
    public String template() {
        return "inline-affliction2md.txt";
    }

    @RegisterForReflection
    public static class QuteAfflictionStage {
        public String text;
        public String duration;
    }
}
