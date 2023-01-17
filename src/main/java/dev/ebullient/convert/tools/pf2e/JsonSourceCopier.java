package dev.ebullient.convert.tools.pf2e;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonSourceCopier implements JsonSource {
    final Pf2eIndex index;

    JsonSourceCopier(Pf2eIndex index) {
        this.index = index;
    }

    @Override
    public Pf2eIndex index() {
        return index;
    }

    @Override
    public Pf2eSources getSources() {
        throw new IllegalStateException("Should not call getSources while copying source");
    }

    JsonNode handleCopy(Pf2eIndexType type, JsonNode jsonSource) {

        return jsonSource;
    }
}
