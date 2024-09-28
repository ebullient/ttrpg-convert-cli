package dev.ebullient.convert.tools.pf2e.qute;

import java.util.List;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools Trait attributes ({@code trait2md.txt})
 *
 * Extension of {@link dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase Pf2eQuteBase}
 */
@TemplateData
public class QuteTrait extends Pf2eQuteBase {

    /** Aliases for this note */
    public final List<String> aliases;
    /** List of categories to which this trait belongs */
    public final List<String> categories;

    public QuteTrait(Pf2eSources sources, List<String> text, Tags tags,
            List<String> aliases, List<String> categories) {
        super(sources, text, tags);

        this.aliases = aliases;
        this.categories = categories;
    }

    @Override
    public boolean createIndex() {
        return false;
    }
}
