package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools Affliction attributes for standalone notes ({@code affliction2md.txt})
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase Pf2eQuteBase}
 * </p>
 */
@TemplateData
public class QuteAffliction extends Pf2eQuteBase {

    /** Collection of traits (decorated links) */
    public final Collection<String> traits;
    /** Aliases for this note */
    public final List<String> aliases;

    /** Affliction level */
    public final String level;
    /** Type of affliction. Usually shown alongside the level. */
    public final String affliction;
    /** A description of the tempted version of the curse */
    public final String temptedCurse;

    public QuteAffliction(Pf2eSources sources, List<String> text, Tags tags,
            Collection<String> traits, List<String> aliases,
            String level, String affliction, String temptedCurse) {
        super(sources, text, tags);
        this.traits = traits;
        this.aliases = aliases;
        this.level = level;
        this.affliction = affliction;
        this.temptedCurse = temptedCurse;
    }

    /**
     * True if the affliction specifies a tempting curse. If you use a section header
     * for curse information, use this test to add a section header before other text.
     */
    @Override
    public boolean getHasSections() {
        return (temptedCurse != null && !temptedCurse.isEmpty());
    }

    public String title() {
        return String.format("%s, _%s %s_", getName(), affliction, level);
    }
}
