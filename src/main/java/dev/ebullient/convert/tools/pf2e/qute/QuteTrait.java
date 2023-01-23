package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.tools.pf2e.Pf2eSources;

public class QuteTrait extends Pf2eQuteBase {

    public final List<String> aliases;
    public final List<String> categories;

    public QuteTrait(Pf2eSources sources, List<String> text, Collection<String> tags,
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
