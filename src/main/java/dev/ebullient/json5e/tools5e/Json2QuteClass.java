package dev.ebullient.json5e.tools5e;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.json5e.qute.QuteSource;

public class Json2QuteClass extends Json2QuteCommon {

    Json2QuteClass(JsonIndex index, IndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    public QuteSource build() {

        //return new QuteClass(name, source, tags);
        return null;
    }

}
