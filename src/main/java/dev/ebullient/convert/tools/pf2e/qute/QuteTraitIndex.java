package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import dev.ebullient.convert.qute.QuteNote;
import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;

public class QuteTraitIndex extends QuteNote {

    public final Map<String, Collection<String>> categoryToTraits;

    public QuteTraitIndex(Pf2eSources sources, Map<String, Collection<String>> categoryToTraits) {
        super(sources, "Trait Index", null, "", List.of());
        this.categoryToTraits = categoryToTraits;
    }

    @Override
    public String targetFile() {
        return "index";
    }

    @Override
    public String targetPath() {
        return Pf2eIndexType.trait.relativePath();
    }

    @Override
    public String template() {
        return "indexTrait.txt";
    }
}
