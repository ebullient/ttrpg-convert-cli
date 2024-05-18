package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools Affliction attributes (inline/embedded, {@code inline-affliction2md.txt})
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteNote Pf2eQuteNote}
 * </p>
 */
@TemplateData
public class QuteInlineAffliction extends Pf2eQuteNote {

    /** Collection of traits (decorated links) */
    public final Collection<String> traits;
    /** Overall power, from 1 to 10. */
    public final String level;
    /** Formatted text. Maximum duration of the infliction. */
    public final String maxDuration;
    /** Formatted text. Maximum duration of the infliction. */
    public final String onset;
    /** Formatted text. Savint throws. */
    public final String savingThrow;
    /** Formatted text. Affliction effect */
    public final String effect;
    /**
     * Affliction stages: map of name to stage data as
     * {@link dev.ebullient.convert.tools.pf2e.qute.QuteInlineAffliction.QuteAfflictionStage QuteAfflictionStage}
     */
    public final Map<String, QuteAfflictionStage> stages;

    public QuteInlineAffliction(String name, List<String> text, Tags tags,
            Collection<String> traits, String level,
            String maxDuration, String onset, String savingThrow,
            String effect, Map<String, QuteAfflictionStage> stages) {
        super(Pf2eIndexType.affliction, name, null, text, tags);

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

    /**
     * Pf2eTools affliction stage attributes.
     *
     * @param text Formatted text. Affliction stage
     * @param duration Formatted text. Affliction duration
     */
    @TemplateData
    public record QuteAfflictionStage(String duration, String text) {
    }
}
