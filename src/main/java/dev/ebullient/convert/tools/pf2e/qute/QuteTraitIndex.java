package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

@TemplateData
public class QuteTraitIndex extends Pf2eQuteNote {

    public final Map<String, Collection<String>> categoryToTraits;

    public QuteTraitIndex(Pf2eSources sources, Map<String, Collection<String>> categoryToTraits) {
        super(Pf2eIndexType.syntheticGroup, sources, "Trait Index");
        this.categoryToTraits = new TreeMap<>();
        for (Map.Entry<String, Collection<String>> entry : categoryToTraits.entrySet()) {
            List<String> sorted = entry.getValue().stream()
                    .filter(x -> x.matches("\\[.+?\\]\\(.+?\\)"))
                    .sorted()
                    .collect(Collectors.toList());
            if (!sorted.isEmpty()) {
                this.categoryToTraits.put(entry.getKey(), sorted);
            }
        }
    }

    @Override
    public String targetFile() {
        return "traits";
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
