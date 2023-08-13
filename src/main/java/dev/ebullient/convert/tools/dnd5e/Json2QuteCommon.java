package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteNote;

public class Json2QuteCommon implements JsonSource {
    protected final Tools5eIndex index;
    protected final Tools5eSources sources;
    protected final Tools5eIndexType type;
    protected final JsonNode rootNode;
    protected String imagePath = null;

    Json2QuteCommon(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        this.index = index;
        this.rootNode = jsonNode;
        this.type = type;
        this.sources = type.multiNode() ? null : Tools5eSources.findOrTemporary(jsonNode);
    }

    public Json2QuteCommon withImagePath(String imagePath) {
        this.imagePath = imagePath;
        return this;
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

    @Override
    public String getImagePath() {
        if (imagePath != null) {
            return imagePath;
        }
        return JsonSource.super.getImagePath();
    }

    public String getText(String heading) {
        List<String> text = new ArrayList<>();
        appendToText(text, rootNode, heading);
        appendFootnotes(text, 0);
        return text.isEmpty() ? null : String.join("\n", text);
    }

    public String getFluffDescription(Tools5eIndexType fluffType, String heading, List<ImageRef> images) {
        List<String> text = getFluff(fluffType, heading, images);
        return text.isEmpty() ? null : String.join("\n", text);
    }

    public List<String> getFluff(Tools5eIndexType fluffType, String heading, List<ImageRef> images) {
        List<String> text = new ArrayList<>();
        JsonNode fluffNode = null;
        if (rootNode.has("fluff")) {
            fluffNode = rootNode.get("fluff");
            if (fluffNode.has("_monsterFluff")) {
                String fluffKey = fluffType.createKey(fluffNode.get(""));
                fluffNode = index.getNode(fluffKey);
            }
        } else if (booleanOrDefault(rootNode, "hasFluff", false) || booleanOrDefault(rootNode, "hasFluffImages", false)) {
            String fluffKey = fluffType.createKey(rootNode);
            fluffNode = index.getNode(fluffKey);
        }

        if (fluffNode != null) {
            JsonSourceCopier copier = new JsonSourceCopier(index);
            fluffNode = copier.handleCopy(fluffType, fluffNode);
            if (fluffNode.has("entries")) {
                appendToText(text, fluffNode.get("entries"), heading);
            }
            getImages(fluffNode.get("images"), images);
        }
        return text;
    }

    public List<ImageRef> getFluffImages(Tools5eIndexType fluffType) {
        List<ImageRef> images = new ArrayList<>();
        if (booleanOrDefault(rootNode, "hasFluffImages", false)) {
            String fluffKey = fluffType.createKey(rootNode);
            JsonNode fluffNode = index.getNode(fluffKey);
            if (fluffNode != null) {
                JsonSourceCopier copier = new JsonSourceCopier(index);
                fluffNode = copier.handleCopy(fluffType, fluffNode);
                getImages(fluffNode.get("images"), images);
            }
        }
        return images;
    }

    public void getImages(JsonNode imageNode, List<ImageRef> images) {
        if (imageNode != null && imageNode.isArray()) {
            for (Iterator<JsonNode> i = imageNode.elements(); i.hasNext();) {
                ImageRef ir = readImageRef(i.next());
                if (ir != null) {
                    images.add(ir);
                }
            }
        }
    }

    public final Tools5eQuteBase build() {
        boolean pushed = parseState().push(getSources(), rootNode);
        try {
            return buildQuteResource();
        } catch (Exception e) {
            tui().errorf(e, "build(): Error processing '%s': %s", getName(), e.toString());
            throw e;
        } finally {
            parseState().pop(pushed);
        }
    }

    public final Tools5eQuteNote buildNote() {
        boolean pushed = parseState().push(getSources(), rootNode);
        try {
            return buildQuteNote();
        } catch (Exception e) {
            tui().errorf(e, "buildNote(): Error processing '%s': %s", getName(), e.toString());
            throw e;
        } finally {
            parseState().pop(pushed);
        }
    }

    protected Tools5eQuteBase buildQuteResource() {
        tui().warnf("The default buildQuteResource method was called for %s. Was this intended?", sources.toString());
        return null;
    }

    protected Tools5eQuteNote buildQuteNote() {
        tui().warnf("The default buildQuteNote method was called for %s. Was this intended?", sources.toString());
        return null;
    }
}
