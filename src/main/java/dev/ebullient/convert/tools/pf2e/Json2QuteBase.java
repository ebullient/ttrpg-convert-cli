package dev.ebullient.convert.tools.pf2e;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.QuteBase;

public abstract class Json2QuteBase implements Pf2eTypeReader {
    protected final Pf2eIndex index;
    protected final Pf2eIndexType type;
    protected final JsonNode rootNode;
    protected final Pf2eSources sources;

    public Json2QuteBase(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode) {
        this(index, type, rootNode, Pf2eSources.findSources(rootNode));
    }

    public Json2QuteBase(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode, Pf2eSources sources) {
        this.index = index;
        this.type = type;
        this.rootNode = rootNode;
        this.sources = sources;
    }

    public List<String> collectTraits() {
        return Field.traits.getListOfStrings(rootNode, tui()).stream()
                .sorted()
                .map(s -> linkify(Pf2eIndexType.trait, s))
                .collect(Collectors.toList());
    }

    @Override
    public Pf2eIndex index() {
        return index;
    }

    @Override
    public Pf2eSources getSources() {
        return sources;
    }

    public abstract QuteBase build();
}
