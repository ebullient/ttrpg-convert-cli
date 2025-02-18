package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import dev.ebullient.convert.qute.QuteUtil;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.JsonSource;
import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools Ability attributes ({@code ability2md.txt} or {@code inline-ability2md.txt}).
 *
 * Abilities are rendered both standalone and inline (as an admonition block).
 * The default template can render both. It contains some special syntax to handle
 * the inline case.
 *
 * Use `%%--` to mark the end of the preamble (frontmatter and
 * other leading content only appropriate to the standalone case).
 *
 * Extension of {@link dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteNote Pf2eQuteNote}
 */
@TemplateData
public final class QuteAbility extends Pf2eQuteNote implements QuteUtil.Renderable, QuteAbilityOrAffliction {

    /** A formatted string which is a link to the base ability that this ability references. Embedded only. */
    public final String reference;
    /**
     * Collection of trait links as {@link QuteDataRef}. Use `{#for}` or `{#each}` to iterate over the collection.
     * See {@link QuteAbility#getBareTraitList()}.
     */
    public final Collection<QuteDataRef> traits;
    /** {@link QuteDataRange}. The targeting range for this ability. */
    public final QuteDataRange range;
    /** List of formatted strings. Activation components for this ability, e.g. command, envision */
    public final List<String> components;
    /** Formatted string. Trigger to activate this ability */
    public final String trigger;
    /** Formatted string. Requirements for activating this ability */
    public final String requirements;
    /** Formatted string. Prerequisites before this ability can be activated or taken. */
    public final String prerequisites;
    /**
     * {@link dev.ebullient.convert.tools.pf2e.qute.QuteDataFrequency QuteDataFrequency}.
     * How often this ability can be used/activated. Use directly to get a formatted string.
     */
    public final QuteDataFrequency frequency;
    /** The cost of using this ability */
    public final String cost;
    /** Any additional notes related to this ability that aren't included in the other fields. */
    public final String note;
    /** Special notes for this ability - usually requirements or caveats relating to its use. */
    public final String special;
    /**
     * True if this ability is embedded in another note (admonition block).
     * When this is true, the {@code inline-ability} template is used.
     */
    public final boolean embedded;
    /** Ability ({@link dev.ebullient.convert.tools.pf2e.qute.QuteDataActivity activity/activation details}) */
    public final QuteDataActivity activity;

    // Internal only.
    private final JsonSource _converter;

    public QuteAbility(Pf2eSources sources, String name, String reference, List<String> text, Tags tags,
            Collection<QuteDataRef> traits, QuteDataActivity activity, QuteDataRange range,
            List<String> components, String requirements, String prerequisites,
            String cost, String trigger, QuteDataFrequency frequency, String special, String note,
            boolean embedded, JsonSource converter) {
        super(Pf2eIndexType.ability, sources, name, text, tags);

        this.reference = reference;
        this.traits = traits;
        this.activity = activity;
        this.range = range;
        this.components = components;
        this.requirements = requirements;
        this.prerequisites = prerequisites;
        this.cost = cost;
        this.trigger = trigger;
        this.frequency = frequency;
        this.special = special;
        this.note = note;
        this.embedded = embedded;
        this._converter = converter;
    }

    /**
     * True if we have any details other than an activity, an effect, and components. e.g. if we have a cost, range,
     * requirements, prerequisites, trigger, frequency, or special.
     *
     * <p>Use this to test for the end of those attributes (e.g. to add whitespace or a special
     * character ahead of ability text)</p>
     */
    public boolean getHasAttributes() {
        return isPresent(range) || isPresent(requirements) || isPresent(prerequisites) || isPresent(cost)
            || isPresent(trigger) || isPresent(frequency) || isPresent(special) || isPresent(note);
    }

    /**
     * False if the ability is a short, one-line name and description.
     * Use this to test to choose between a detailed or simple rendering.
     */
    public boolean getHasDetails() {
        return getHasAttributes() || text.contains("\n") || text.split(" ").length > 5;
    }

    @Deprecated
    public boolean getHasBullets() {
        return getHasAttributes();
    }

    /** Return a comma-separated list of de-styled trait links (no title attributes) */
    public String getBareTraitList() {
        if (traits == null || traits.isEmpty()) {
            return "";
        }
        return traits.stream()
                .map(QuteDataRef::withoutTitle)
                .collect(Collectors.joining(", "));
    }

    @Override
    public String template() {
        return embedded ? "inline-ability2md.txt" : "ability2md.txt";
    }

    @Override
    public String toString() {
        return render();
    }

    @Override
    public String render(boolean asYamlStatblock) {
        return _converter.renderEmbeddedTemplate(this, null, asYamlStatblock);
    }
}
