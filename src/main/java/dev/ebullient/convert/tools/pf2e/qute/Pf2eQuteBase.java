package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;

import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;

public class Pf2eQuteBase extends QuteBase {

    protected Pf2eIndexType type;

    public Pf2eQuteBase(Pf2eSources sources, String name, String source, String text, Collection<String> tags) {
        super(sources, name, source, text, tags);
        this.type = sources.getType();
    }

    @Override
    public String targetPath() {
        return type.compendiumPath();
    }
}
