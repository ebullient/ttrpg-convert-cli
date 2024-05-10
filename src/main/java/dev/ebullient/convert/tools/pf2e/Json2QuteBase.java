package dev.ebullient.convert.tools.pf2e;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteNote;

public abstract class Json2QuteBase implements Pf2eTypeReader {
    protected final Pf2eIndex index;
    protected final Pf2eIndexType type;
    protected final JsonNode rootNode;
    protected final Pf2eSources sources;

    public Json2QuteBase(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode) {
        this(index, type, rootNode, Pf2eSources.findOrTemporary(type, rootNode));
    }

    public Json2QuteBase(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode, Pf2eSources sources) {
        this.index = index;
        this.type = type;
        this.rootNode = rootNode;
        this.sources = sources;
    }

    @Override
    public Pf2eIndex index() {
        return index;
    }

    @Override
    public Pf2eSources getSources() {
        return sources;
    }

    public Pf2eQuteBase build() {
        boolean pushed = parseState().push(getSources(), rootNode);
        try {
            return buildQuteResource();
        } finally {
            parseState().pop(pushed);
        }
    }

    public Pf2eQuteNote buildNote() {
        boolean pushed = parseState().push(getSources(), rootNode);
        try {
            return buildQuteNote();
        } finally {
            parseState().pop(pushed);
        }
    }

    protected Pf2eQuteBase buildQuteResource() {
        tui().warnf("The default buildQuteResource method was called for %s. Was this intended?", sources.toString());
        return null;
    }

    protected Pf2eQuteNote buildQuteNote() {
        tui().warnf("The default buildQuteNote method was called for %s. Was this intended?", sources.toString());
        return null;
    }
}
