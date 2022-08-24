package dev.ebullient.json5e.tools5e;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.json5e.io.Json5eTui;
import dev.ebullient.json5e.qute.QuteSource;

public abstract class Json2QuteCommon implements JsonSource {
    protected final JsonIndex index;
    protected final CompendiumSources sources;
    protected final JsonNode node;

    Json2QuteCommon(JsonIndex index, IndexType type, JsonNode jsonNode) {
        this.index = index;
        this.node = handleCopy(type, jsonNode);
        this.sources = index.constructSources(type, jsonNode);
    }

    String getName() {
        return this.sources.getName();
    }

    @Override
    public CompendiumSources getSources() {
        return sources;
    }

    @Override
    public Json5eTui tui() {
        return index.tui();
    }

    @Override
    public JsonIndex index() {
        return index;
    }

    public abstract QuteSource build();
}
