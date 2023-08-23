package dev.ebullient.convert.tools.pf2e;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.pf2e.qute.QuteAbility;

public class Json2QuteAbility extends Json2QuteBase {

    public Json2QuteAbility(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode) {
        super(index, type, rootNode);
    }

    @Override
    protected QuteAbility buildQuteNote() {
        return Pf2eTypeAbility.createAbility(rootNode, this, false);
    }
}
