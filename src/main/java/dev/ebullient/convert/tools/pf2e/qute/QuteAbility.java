package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;
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
public class QuteAbility extends Pf2eQuteNote {

    /**
     * Collection of trait links. Use `{#for}` or `{#each}` to iterate over the collection.
     * See <a href="#traitList">traitList</a> or <a href="#bareTraitList">bareTraitList</a>.
     */
    public final Collection<String> traits;
    /** Formatted string. Components required to activate this ability (embedded/inline only) */
    public final String components;
    /** Formatted string. Trigger to activate this ability */
    public final String trigger;
    /** Formatted string. Requirements for activating this ability */
    public final String requirements;
    /** How often this ability can be used/activated */
    public final String frequency;
    /** The cost of using this ability */
    public final String cost;
    /** Caveats related to using this ability (embedded/inline only) */
    public final String note;
    /** Special characteristics of this ability (embedded/inline only) */
    public final String special;
    /**
     * True if this ability is embedded in another note (admonition block).
     * When this is true, the {@code inline-ability} template is used.
     */
    public final boolean embedded;
    /** Ability ({@link dev.ebullient.convert.tools.pf2e.qute.QuteDataActivity activity/activation details}) */
    public final QuteDataActivity activity;

    public QuteAbility(String name, List<String> text, Tags tags,
            Collection<String> traits, QuteDataActivity activity,
            String components, String requirements,
            String cost, String trigger, String frequency, String special, String note,
            boolean embedded) {
        super(Pf2eIndexType.ability, name, null, text, tags);

        this.traits = traits;
        this.activity = activity;
        this.components = components;
        this.requirements = requirements;
        this.cost = cost;
        this.trigger = trigger;
        this.frequency = frequency;
        this.special = special;
        this.note = note;
        this.embedded = embedded;
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
     * True if getHasAttributes is true or special is present.
     * In other words, this is true if there is more than just a name and text.
     *
     * Use this to test to choose between a detailed or simple rendering.
     */
    public boolean getHasDetails() {
        return getHasAttributes() || isPresent(special);
    }

    @Deprecated
    public boolean getHasBullets() {
        return getHasAttributes();
    }

    /** True if frequency, trigger, and requirements are present. In other words, this is true if the ability has an effect. */
    public boolean getHasEffect() {
        return isPresent(frequency) || isPresent(trigger) || isPresent(requirements);
    }

    /** Return a comma-separated list of trait links */
    public String getTraitList() {
        if (traits == null || traits.isEmpty()) {
            return "";
        }
        return traits.stream()
                .collect(Collectors.joining(", "));
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
}
