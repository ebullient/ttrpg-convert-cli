package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.Map;

import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

@TemplateData
public class QuteTraitIndex extends Pf2eQuteNote {

    public final Map<String, Collection<String>> categoryToTraits;

    public QuteTraitIndex(Pf2eSources sources, Map<String, Collection<String>> categoryToTraits) {
        super(Pf2eIndexType.syntheticGroup, sources, "Trait Index");
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
