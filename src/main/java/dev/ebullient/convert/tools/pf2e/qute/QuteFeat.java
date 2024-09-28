package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools Feat attributes ({@code feat2md.txt})
 *
 * Feats are rendered both standalone and inline (as an admonition block).
 * The default template can render both.
 * It uses special syntax to handle the inline case.
 *
 * Use `%%--` to mark the end of the preamble (frontmatter and
 * other leading content only appropriate to the standalone case).
 *
 * Extension of {@link dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase Pf2eQuteBase}
 */
@TemplateData
public class QuteFeat extends Pf2eQuteBase {

    /** Collection of traits (decorated links) */
    public final Collection<String> traits;
    /** Aliases for this note */
    public final List<String> aliases;

    public final String level;
    public final String access;
    /**
     * {@link dev.ebullient.convert.tools.pf2e.qute.QuteDataFrequency QuteDataFrequency}.
     * How often this feat can be used/activated. Use directly to get a formatted string.
     */
    public final QuteDataFrequency frequency;
    /** Activity/Activation cost (as {@link dev.ebullient.convert.tools.pf2e.qute.QuteDataActivity QuteDataActivity}) */
    public final QuteDataActivity activity;
    public final String trigger;
    public final String cost;
    public final String requirements;
    public final String prerequisites;
    public final String special;
    public final List<String> leadsTo;
    public final String note;
    /**
     * True if this ability is embedded in another note (admonition block).
     * The default template uses this flag to include a `title:` prefix for the admonition block:<br />
     * `{#if resource.embedded }title: {#else}# {/if}{resource.name}` *
     */
    public final boolean embedded;

    public QuteFeat(Pf2eSources sources, List<String> text, Tags tags,
            Collection<String> traits, List<String> aliases,
            String level, String access, QuteDataFrequency frequency, QuteDataActivity activity, String trigger,
            String cost, String requirements, String prerequisites, String special, String note,
            List<String> leadsTo, boolean embedded) {
        super(sources, text, tags);
        this.traits = traits;
        this.aliases = aliases;

        this.level = level;
        this.access = access;
        this.frequency = frequency;
        this.activity = activity;
        this.trigger = trigger;
        this.cost = cost;
        this.requirements = requirements;
        this.prerequisites = prerequisites;
        this.special = special;
        this.note = note;
        this.leadsTo = leadsTo;
        this.embedded = embedded;
    }

    @Override
    public boolean getHasSections() {
        return super.getHasSections() || (leadsTo != null && !leadsTo.isEmpty());
    }

    public String title() {
        return String.format("%s%s, *Feat %s*", getName(),
                activity == null
                        ? ""
                        : " " + activity,
                level);
    }
}
