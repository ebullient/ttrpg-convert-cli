package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndex.Tuple;
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
            tags.add("deity", pantheon);
        }

        List<String> domains = new ArrayList<>();
        if (rootNode.has("domains")) {
            rootNode.withArray("domains").forEach(d -> {
                String domain = d.asText();
                tags.add("domain", domain);
                domains.add(domain);
            });
        }

        return new QuteDeity(sources,
                getName(),
                getSourceText(getSources()),
                DeityField.altNames.replaceTextFromList(rootNode, this),
                pantheon,
                dietyAlignment(),
                replaceText(DeityField.title.getTextOrEmpty(rootNode)),
                replaceText(DeityField.category.getTextOrEmpty(rootNode)),
                String.join(", ", domains),
                replaceText(DeityField.province.getTextOrEmpty(rootNode)),
                replaceText(DeityField.symbol.getTextOrEmpty(rootNode)),
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

    public static Iterable<String> findDeitiesToRemove(List<Tuple> allDeities) {
        final Comparator<String> byDate = Comparator
                .comparing(k -> TtrpgConfig.sourcePublicationDate(k));

        Function<Tuple, String> deityKey = n -> {
            String reprintAlias = DeityField.reprintAlias.getTextOrNull(n.node);
            if (reprintAlias == null) {
                String pantheon = DeityField.pantheon.getTextOrEmpty(n.node);
                String name = SourceField.name.getTextOrEmpty(n.node);
                return name + "-" + pantheon;
            }
            return reprintAlias;
        };

        // Group by source
        Map<String, List<Tuple>> deityBySource = allDeities.stream()
                .collect(Collectors.groupingBy(t -> SourceField.source.getTextOrEmpty(t.node)));

        // Sort the sources by date, descending
        List<String> sourcesByDate = deityBySource.keySet().stream()
                .sorted(byDate.reversed())
                .toList();

        Map<String, Tuple> keepers = new HashMap<>();
        List<String> keysToRemove = new ArrayList<>();
        // Iterate over groups of deities in order of publication.
        // Keep the first deity of each name, add others to the remove pile.
        for (String book : sourcesByDate) {
            List<Tuple> deities = deityBySource.remove(book);

            if (keepers.isEmpty()) { // most recent bucket. Keep all.
                deities.forEach(t -> keepers.put(deityKey.apply(t), t));
                continue;
            }
            for (Tuple deity : deities) {
                String key = deityKey.apply(deity);
                if (keepers.containsKey(key)) {
                    keysToRemove.add(deity.key);
                } else {
                    keepers.put(key, deity);
                }
            }
        }
        return keysToRemove;
    }

    enum DeityField implements JsonNodeReader {
        altNames,
        category,
        pantheon,
        province,
        symbol,
        title,
        reprintAlias
    }
}
