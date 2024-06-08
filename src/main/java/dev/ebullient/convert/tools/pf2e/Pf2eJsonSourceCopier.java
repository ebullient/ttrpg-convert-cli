package dev.ebullient.convert.tools.pf2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.ebullient.convert.tools.JsonSourceCopier;

import java.util.List;

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
    protected JsonNode getOriginNode(String key) {
        return index.getOrigin(key);
    }

    @Override
    protected String getExternalTemplateKey(JsonNode trait) {
        // Not used in Pf2eTools data
        return null;
    }

    @Override
    protected JsonNode resolveTemplateVariable(
            String originKey, JsonNode value, JsonNode target, TemplateVariable variableMode, List<String> params
    ) {
        // Not used in Pf2eTools data
        return null;
    }

    @Override
    protected boolean doModProp(
        ModFieldMode mode, String originKey, JsonNode modInfo, JsonNode copyFrom, ObjectNode target
    ) {
        // Not used in Pf2eTools data
        return false;
    }

    @Override
    public Pf2eSources getSources() {
        throw new IllegalStateException("Should not call getSources while copying source");
    }
}
