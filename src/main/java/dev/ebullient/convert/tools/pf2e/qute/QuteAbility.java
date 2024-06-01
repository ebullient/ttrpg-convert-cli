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
 * <p>
 * Abilities are rendered both standalone and inline (as an admonition block).
 * The default template can render both. It contains some special syntax to handle
 * the inline case.
 * </p>
 * <p>
 * Use `%%--` to mark the end of the preamble (frontmatter and
 * other leading content only appropriate to the standalone case).
 * </p>
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteNote Pf2eQuteNote}
 * </p>
 */
@TemplateData
public final class QuteAbility extends Pf2eQuteNote implements QuteUtil.Renderable, QuteAbilityOrAffliction {

    /** A formatted string which is a link to the base ability that this ability references. Embedded only. */
    public final String reference;
    /**
     * Collection of trait links. Use `{#for}` or `{#each}` to iterate over the collection.
     * See <a href="#traitlist">traitList</a> or <a href="#baretraitlist">bareTraitList</a>.
     */
    public final Collection<String> traits;
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

    public QuteAbility(Pf2eSources sources, String name, String reference, String text, Tags tags,
            Collection<String> traits, QuteDataActivity activity, QuteDataRange range,
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

    /** True if an activity (with text), components, or traits are present. */
    public boolean getHasActivity() {
        return activity != null || isPresent(components) || isPresent(traits);
    }

    /**
     * True if hasActivity is true, hasEffect is true or cost is present.
     * In other words, this is true if a list of attributes could have been rendered.
     *
     * Use this to test for the end of those attributes (add whitespace or a special
     * character ahead of ability text)
     */
    public boolean getHasAttributes() {
        return getHasActivity() || getHasEffect() || isPresent(cost);
    }

    /**
     * True if the ability is a short, one-line name and description.
     * Use this to test to choose between a detailed or simple rendering.
     */
    public boolean getHasDetails() {
        return getHasAttributes() || isPresent(special) || text.contains("\n") || text.split(" ").length > 5;
    }

    @Deprecated
    public boolean getHasBullets() {
        return getHasAttributes();
    }

    /** True if frequency, trigger, and requirements are present. In other words, this is true if the ability has an effect. */
    public boolean getHasEffect() {
        return isPresent(frequency) || isPresent(trigger) || isPresent(requirements);
    }

    /** Return a comma-separated list of de-styled trait links (no title attributes) */
    public String getBareTraitList() {
        if (traits == null || traits.isEmpty()) {
            return "";
        }
        return traits.stream()
                .map(x -> x.replaceAll(" \".*\"", ""))
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
    public String render() {
        return _converter.renderEmbeddedTemplate(this, null);
    }
}
