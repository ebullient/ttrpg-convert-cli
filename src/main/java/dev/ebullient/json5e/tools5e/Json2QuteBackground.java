package dev.ebullient.json5e.tools5e;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.json5e.qute.QuteBackground;
import dev.ebullient.json5e.qute.QuteSource;

public class Json2QuteBackground extends Json2QuteCommon {

    Json2QuteBackground(JsonIndex index, IndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    public QuteSource build() {
        String backgroundName = decoratedBackgroundName(sources.getName());

        return new QuteBackground(
                decoratedTypeName(backgroundName, sources),
                sources.getSourceText(),
                getText("##"),
                List.of("background/" + tui().slugify(sources.primarySource())));
    }

    String decoratedBackgroundName(String name) {
        if (name.startsWith("Variant")) {
            name = name.replace("Variant ", "") + " (Variant)";
        }
        return name;
    }

}
