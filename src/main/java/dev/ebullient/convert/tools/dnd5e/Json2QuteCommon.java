package dev.ebullient.convert.tools.dnd5e;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.tools.dnd5e.qute.QuteSource;

public abstract class Json2QuteCommon implements JsonSource {
    protected final Tools5eIndex index;
    protected final Tools5eSources sources;
    protected final JsonNode node;

    Json2QuteCommon(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        this.index = index;
        this.node = jsonNode;
        this.sources = type == Tools5eIndexType.sourceless ? null : index.constructSources(type, jsonNode);
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
        return text.isEmpty() ? null : String.join("\n", text);
    }

    public String getFluffDescription(Tools5eIndexType fluffType, String heading, List<ImageRef> imageRef) {
        List<String> text = getFluff(fluffType, heading, imageRef);
        return text.isEmpty() ? null : String.join("\n", text);
    }

    public List<String> getFluff(Tools5eIndexType fluffType, String heading, List<ImageRef> imageRef) {
        List<String> text = new ArrayList<>();
        if (booleanOrDefault(node, "hasFluff", false) || booleanOrDefault(node, "hasFluffImages", false)) {
            JsonNode fluffNode = index.getNode(fluffType, node);
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
            JsonNode fluffNode = index.getNode(fluffType, node);
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

    ImageRef readImageRef(JsonNode imageNode) {
        try {
            JsonMediaHref mediaHref = mapper().treeToValue(imageNode, JsonMediaHref.class);
            if (mediaHref.href.path != null) {
                String title = mediaHref.title == null ? "" : mediaHref.title;

                Path sourcePath = Path.of("img", mediaHref.href.path);
                String fileName = sourcePath.getFileName().toString();
                int x = fileName.lastIndexOf('.');

                Path target = Path.of(getImagePath(), "img",
                        slugify(fileName.substring(0, x)) + fileName.substring(x));

                return new ImageRef.Builder()
                        .setSourcePath(sourcePath)
                        .setTargetPath(index().compendiumPath(), target)
                        .setMarkdownAttributes(title, index().compendiumRoot())
                        .build();
            } else {
                throw new IllegalArgumentException("We have an ImageRef with no path");
            }
        } catch (JsonProcessingException | IllegalArgumentException e) {
            tui().errorf(e, "Unable to read media reference from %s", imageNode.toPrettyString());
        }
        return null;
    }

    protected String getImagePath() {
        switch (sources.getType()) {
            case background:
                return QuteSource.BACKGROUND_PATH;
            case deity:
                return QuteSource.DEITIES_PATH;
            case feat:
                return QuteSource.FEATS_PATH;
            case item:
                return QuteSource.ITEMS_PATH;
            case race:
                return QuteSource.RACES_PATH;
            case spell:
                return QuteSource.SPELLS_PATH;
            // Note: Monster overrides this method
            default:
                throw new IllegalArgumentException("We need the fluff image path for: " + sources.getType());
        }
    }

    public abstract QuteBase build();
}
