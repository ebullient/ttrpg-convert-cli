package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import dev.ebullient.convert.qute.QuteUtil;
import dev.ebullient.convert.tools.JsonTextConverter;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.toTitleCase;

/**
 * Pf2eTools Affliction attributes (inline/embedded, {@code inline-affliction2md.txt})
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteNote Pf2eQuteNote}
 * </p>
 */
@TemplateData
public class QuteAffliction extends Pf2eQuteNote implements QuteUtil.Renderable {

    /** Collection of traits (decorated links) */
    public final Collection<String> traits;
    /** Integer from 1 to 10. Level of the affliction. */
    public final String level;
    /** Aliases for this note. Only populated if not embedded. */
    public final List<String> aliases;
    /** Category of affliction (Curse or Disease). Usually shown alongside the level. */
    public final String category;
    /** A description of the tempted version of the curse */
    public final String temptedCurse;
    /** Formatted text. Maximum duration of the infliction. */
    public final String maxDuration;
    /** Formatted text. Maximum duration of the infliction. */
    public final String onset;
    /**
     * The saving throw required to not contract or advance the affliction as
     * {@link dev.ebullient.convert.tools.pf2e.qute.QuteAffliction.QuteAfflictionSave QuteAfflictionSave}
     */
    public final QuteAfflictionSave savingThrow;
    /** Formatted text. Affliction effect, may be multiple lines. */
    public final String effect;
    /**
     * Affliction stages: map of name to stage data as
     * {@link dev.ebullient.convert.tools.pf2e.qute.QuteAffliction.QuteAfflictionStage QuteAfflictionStage}
     */
    public final Map<String, QuteAfflictionStage> stages;
    /** If true, then this affliction is embedded into a larger note. */
    public final boolean isEmbedded;
    /** Any additional notes associated with the affliction. */
    public final List<String> notes;

    // Internal only
    private final JsonTextConverter<?> _converter;

    public QuteAffliction(
            Pf2eSources sources, String name, List<String> text, Tags tags,
            Collection<String> traits, List<String> aliases, String level,
            String category, String maxDuration, String onset, QuteAfflictionSave savingThrow,
            String effect, String temptedCurse, List<String> notes, Map<String, QuteAfflictionStage> stages,
            boolean isEmbedded, JsonTextConverter<?> _converter) {
        super(Pf2eIndexType.affliction, sources, name, text, tags);

        this.level = level;
        this.traits = traits;
        this.maxDuration = maxDuration;
        this.onset = onset;
        this.savingThrow = savingThrow;
        this.effect = effect;
        this.stages = stages;
        this.temptedCurse = temptedCurse;
        this.aliases = aliases;
        this.category = category;
        this.isEmbedded = isEmbedded;
        this.notes = notes;
        this._converter = _converter;
    }

    /** The category and level as a formatted string, e.g. "Disease 9" */
    public String formattedLevel() {
        return join(" ",
                // Use "Level" as the default category, but only if we don't already have "Level" in the level text
                category == null && level != null && !level.toLowerCase().startsWith("level ")
                        ? "Level"
                        : toTitleCase(category),
                level);
    }

    @Override
    public String template() {
        return isEmbedded ? "inline-affliction2md.txt" : "affliction2md.txt";
    }

    @Override
    public String render() {
        return _converter.renderInlineTemplate(this, null);
    }

    @Override
    public String toString() {
        return render();
    }

    /**
     * True if the affliction specifies a tempting curse or has stages. If you use a section header
     * for curse information, use this test to add a section header before other text.
     */
    @Override
    public boolean getHasSections() {
        return isPresent(temptedCurse) || !stages.isEmpty();
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

    /**
     * Affliction saving throw
     *
     * @param value The DC of the saving throw
     * @param notes Any notes relating to the saving throw
     * @param save The type of save associated with the throw e.g. Fortitude
     */
    @TemplateData
    public record QuteAfflictionSave(Integer value, String save, List<String> notes) implements QuteDataGenericStat {
        public QuteAfflictionSave(Integer value, String save, String note) {
            this(value, save, note != null && !note.isBlank() ? List.of(note) : List.of());
        }

        @Override
        public String formattedNotes() {
            return String.join(", ", notes);
        }

        @Override
        public String toString() {
            return join(" ",
                    save, isPresent(value) || !notes.isEmpty() ? "DC" : "", value, formattedNotes());
        }
    }
}
