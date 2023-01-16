package dev.ebullient.convert.tools.pf2e;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonSourceCopier implements JsonSource {
    final ToolsPf2eIndex index;

    JsonSourceCopier(ToolsPf2eIndex index) {
        this.index = index;
    }

    @Override
    public ToolsPf2eIndex index() {
        return index;
    }

    @Override
    public ToolsPf2eSources getSources() {
        throw new IllegalStateException("Should not call getSources while copying source");
    }

    JsonNode handleCopy(ToolsPf2eIndexType type, JsonNode jsonSource) {

        return jsonSource;
    }
}
