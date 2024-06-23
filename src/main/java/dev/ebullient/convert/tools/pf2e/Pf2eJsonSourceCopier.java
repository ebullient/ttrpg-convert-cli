package dev.ebullient.convert.tools.pf2e;

import com.fasterxml.jackson.databind.JsonNode;
import dev.ebullient.convert.tools.JsonSourceCopier;

public class Pf2eJsonSourceCopier extends JsonSourceCopier<Pf2eIndexType> implements JsonSource {
    final Pf2eIndex index;

    Pf2eJsonSourceCopier(Pf2eIndex index) {
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

    @Override
    public JsonNode handleCopy(Pf2eIndexType type, JsonNode jsonSource) {

        return jsonSource;
    }
}
