package dev.ebullient.convert.tools.pf2e;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase;

public class Json2QuteBackground extends Json2QuteBase {

    public Json2QuteBackground(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode) {
        super(index, type, rootNode);
    }

    @Override
    protected Pf2eQuteBase buildQuteResource() {

        return super.buildQuteResource();
    }
}
