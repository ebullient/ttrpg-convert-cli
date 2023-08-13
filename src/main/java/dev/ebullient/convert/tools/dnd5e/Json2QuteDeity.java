package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.QuteDeity;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QuteDeity extends Json2QuteCommon {

    Json2QuteDeity(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {
        Tags tags = new Tags(getSources());

        String pantheon = getTextOrDefault(rootNode, "pantheon", null);
        if (pantheon != null) {
            tags.add("deity", slugify(pantheon));
        }

        List<String> domains = new ArrayList<>();
        if (rootNode.has("domains")) {
            rootNode.withArray("domains").forEach(d -> {
                String domain = d.asText();
                tags.add("domain", slugify(domain));
                domains.add(domain);
            });
        }

        return new QuteDeity(sources,
                getName(),
                getSourceText(getSources()),
                findAndReplace(rootNode, "altNames"),
                pantheon,
                dietyAlignment(),
                replaceText(getTextOrEmpty(rootNode, "title")),
                replaceText(getTextOrEmpty(rootNode, "category")),
                String.join(", ", domains),
                replaceText(getTextOrEmpty(rootNode, "province")),
                replaceText(getTextOrEmpty(rootNode, "symbol")),
                getSymbolImage(),
                getText("##"),
                tags);
    }

    String dietyAlignment() {
        ArrayNode a1 = rootNode.withArray("alignment");
        if (a1.size() == 0) {
            return "Unaligned";
        }
        String choices = a1.toString().replaceAll("[^LCNEGAUXY]", "");
        return mapAlignmentToString(choices);
    }

    ImageRef getSymbolImage() {
        if (rootNode.has("symbolImg")) {
            JsonNode symbolImg = rootNode.get("symbolImg");
            try {
                JsonMediaHref mediaHref = mapper().treeToValue(symbolImg, JsonMediaHref.class);
                return buildImageRef(mediaHref, getImagePath());
            } catch (JsonProcessingException | IllegalArgumentException e) {
                tui().errorf("Unable to read media reference from %s", symbolImg.toPrettyString());
            }
        }
        return null;
    }
}
