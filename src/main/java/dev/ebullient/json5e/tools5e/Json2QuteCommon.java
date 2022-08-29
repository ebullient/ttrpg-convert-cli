package dev.ebullient.json5e.tools5e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.json5e.qute.QuteSource;

public abstract class Json2QuteCommon implements JsonSource {
    protected final JsonIndex index;
    protected final CompendiumSources sources;
    protected final JsonNode node;

    Json2QuteCommon(JsonIndex index, IndexType type, JsonNode jsonNode) {
        this.index = index;
        this.node = jsonNode;
        this.sources = type == IndexType.sourceless ? null : index.constructSources(type, jsonNode);
    }

    String getName() {
        return this.sources.getName();
    }

    @Override
    public CompendiumSources getSources() {
        return sources;
    }

    @Override
    public JsonIndex index() {
        return index;
    }

    public String getText(String heading) {
        List<String> text = new ArrayList<>();
        appendEntryToText(text, node, heading);
        return text.isEmpty() ? null : String.join("\n", text);
    }

    public String getFluffDescription(IndexType fluffType, String heading) {
        List<String> text = getFluff(fluffType, heading);
        return text.isEmpty() ? null : String.join("\n", text);
    }

    public List<String> getFluff(IndexType fluffType, String heading) {
        List<String> text = new ArrayList<>();
        if (booleanOrDefault(node, "hasFluff", false)) {
            JsonNode fluffNode = index.getNode(fluffType, node);
            if (fluffNode != null) {
                fluffNode = index.handleCopy(fluffType, fluffNode);
                if (fluffNode.has("entries")) {
                    appendEntryToText(text, fluffNode.get("entries"), heading);
                }
            }
        }
        return text;
    }

    public abstract QuteSource build();
}
