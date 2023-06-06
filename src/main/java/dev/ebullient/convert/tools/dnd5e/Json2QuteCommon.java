package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.qute.QuteNote;

public class Json2QuteCommon implements JsonSource {
    protected final Tools5eIndex index;
    protected final Tools5eSources sources;
    protected final Tools5eIndexType type;
    protected final JsonNode node;

    Json2QuteCommon(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        this.index = index;
        this.node = jsonNode;
        this.type = type;
        this.sources = type == Tools5eIndexType.syntheticGroup ? null : Tools5eSources.findOrTemporary(type, jsonNode);
    }

    String getName() {
        return this.sources.getName();
    }

    @Override
    public Tools5eSources getSources() {
        return sources;
    }

    @Override
    public Tools5eIndex index() {
        return index;
    }

    public String getText(String heading) {
        List<String> text = new ArrayList<>();
        appendEntryToText(text, node, heading);
        appendFootnotes(text, 0);
        return text.isEmpty() ? null : String.join("\n", text);
    }

    public String getFluffDescription(Tools5eIndexType fluffType, String heading, List<ImageRef> imageRef) {
        List<String> text = getFluff(fluffType, heading, imageRef);
        return text.isEmpty() ? null : String.join("\n", text);
    }

    public List<String> getFluff(Tools5eIndexType fluffType, String heading, List<ImageRef> imageRef) {
        List<String> text = new ArrayList<>();
        if (booleanOrDefault(node, "hasFluff", false) || booleanOrDefault(node, "hasFluffImages", false)) {
            String fluffKey = fluffType.createKey(node);
            JsonNode fluffNode = index.getNode(fluffKey);
            if (fluffNode != null) {
                JsonSourceCopier copier = new JsonSourceCopier(index);
                fluffNode = copier.handleCopy(fluffType, fluffNode);

                if (fluffNode.has("entries")) {
                    appendEntryToText(text, fluffNode.get("entries"), heading);
                }

                JsonNode images = fluffNode.get("images");
                if (images != null && images.isArray()) {
                    for (Iterator<JsonNode> i = images.elements(); i.hasNext();) {
                        ImageRef ir = readImageRef(i.next());
                        if (ir != null) {
                            imageRef.add(ir);
                        }
                    }
                }
            }
        }
        return text;
    }

    public List<ImageRef> getFluffImages(Tools5eIndexType fluffType) {
        List<ImageRef> imageRef = new ArrayList<>();
        if (booleanOrDefault(node, "hasFluffImages", false)) {
            String fluffKey = fluffType.createKey(node);
            JsonNode fluffNode = index.getNode(fluffKey);
            if (fluffNode != null) {
                JsonSourceCopier copier = new JsonSourceCopier(index);
                fluffNode = copier.handleCopy(fluffType, fluffNode);

                JsonNode images = fluffNode.get("images");
                if (images != null && images.isArray()) {
                    for (Iterator<JsonNode> i = images.elements(); i.hasNext();) {
                        ImageRef ir = readImageRef(i.next());
                        if (ir != null) {
                            imageRef.add(ir);
                        }
                    }
                }
            }
        }
        return imageRef;
    }

    public final QuteBase build() {
        boolean pushed = node == null ? parseState.push(getSources()) : parseState.push(node);
        try {
            return buildQuteResource();
        } finally {
            parseState.pop(pushed);
        }
    }

    public final QuteNote buildNote() {
        boolean pushed = node == null ? parseState.push(getSources()) : parseState.push(node);
        try {
            return buildQuteNote();
        } finally {
            parseState.pop(pushed);
        }
    }

    protected QuteBase buildQuteResource() {
        tui().warnf("The default buildQuteResource method was called for %s. Was this intended?", sources.toString());
        return null;
    }

    protected QuteNote buildQuteNote() {
        tui().warnf("The default buildQuteNote method was called for %s. Was this intended?", sources.toString());
        return null;
    }
}
