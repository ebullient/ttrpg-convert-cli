package dev.ebullient.convert.tools.pf2e;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonSourceCopier implements JsonSource {
    final JsonIndex index;

    JsonSourceCopier(JsonIndex index) {
        this.index = index;
    }

    @Override
    public JsonIndex index() {
        return index;
    }

    @Override
    public ToolsPf2eSources getSources() {
        throw new IllegalStateException("Should not call getSources while copying source");
    }

    JsonNode handleCopy(IndexType type, JsonNode jsonSource) {

        return jsonSource;
    }

}
