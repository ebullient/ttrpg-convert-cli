package dev.ebullient.json5e.tools5e;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.json5e.io.Json5eTui;
import dev.ebullient.json5e.qute.ImageRef;
import dev.ebullient.json5e.qute.QuteDeity;
import dev.ebullient.json5e.qute.QuteSource;

public class Json2QuteDeity extends Json2QuteCommon {

    Json2QuteDeity(JsonIndex index, IndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    public QuteSource build() {

        List<String> tags = new ArrayList<>(sources.getSourceTags());

        String pantheon = getTextOrDefault(node, "pantheon", null);
        if (pantheon != null) {
            tags.add("deity/" + slugify(pantheon));
        }

        List<String> domains = new ArrayList<>();
        if (node.has("domains")) {
            node.withArray("domains").forEach(d -> {
                String domain = d.asText();
                tags.add("domain/" + slugify(domain));
                domains.add(domain);
            });
        }

        return new QuteDeity(
                decoratedTypeName(getName(), getSources()),
                getSources().getSourceText(index.srdOnly()),
                findAndReplace(node, "altNames"),
                pantheon,
                dietyAlignment(),
                getTextOrDefault(node, "title", null),
                getTextOrDefault(node, "category", null),
                String.join(", ", domains),
                getTextOrDefault(node, "province", null),
                getTextOrDefault(node, "symbol", null),
                getSymbolImage(),
                getText("##"),
                tags);
    }

    String dietyAlignment() {
        ArrayNode a1 = node.withArray("alignment");
        if (a1.size() == 0) {
            return "Unaligned";
        }
        String choices = a1.toString().replaceAll("[^LCNEGAUXY]", "");
        return mapAlignmentToString(choices);
    }

    ImageRef getSymbolImage() {
        if (node.has("symbolImg")) {
            JsonNode symbolImg = node.get("symbolImg");
            try {
                JsonMediaHref mediaHref = Json5eTui.MAPPER.treeToValue(symbolImg, JsonMediaHref.class);
                if (mediaHref.href.path != null) {
                    Path sourcePath = Path.of("img", mediaHref.href.path);
                    Path target = Path.of("dieties", "img", slugify(sourcePath.getFileName().toString()));
                    String title = mediaHref.title == null ? "" : mediaHref.title;

                    return new ImageRef(
                            sourcePath,
                            index().compendiumPath().resolve(target),
                            String.format("![%s](%s#symbol)", title, index().compendiumRoot() + target.toString()));
                }
            } catch (JsonProcessingException | IllegalArgumentException e) {
                tui().errorf("Unable to read media reference from %s", symbolImg.toPrettyString());
            }
        }
        return null;
    }
}
