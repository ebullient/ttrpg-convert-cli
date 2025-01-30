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

import dev.ebullient.convert.config.ReprintBehavior;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.io.Tui;
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

    public static Iterable<String> findDeities(List<Tuple> allDeities) {
        var config = TtrpgConfig.getConfig();
        if (config.reprintBehavior() == ReprintBehavior.all) {
            return allDeities.stream()
                    .filter(t -> Tools5eSources.includedByConfig(t.key))
                    .peek(t -> Tui.instance().logf(Msg.DEITY, " ----  %s", t.key))
                    .map(t -> t.key)
                    .toList();
        }

        final Comparator<String> byDate = Comparator
                .comparing(k -> TtrpgConfig.sourcePublicationDate(k));

        Function<Tuple, String> deityKey = n -> {
            String name = DeityField.reprintAlias.getTextOrDefault(n.node, SourceField.name.getTextOrEmpty(n.node));
            String pantheon = DeityField.pantheon.getTextOrEmpty(n.node);
            return (name + "-" + pantheon).toLowerCase();
        };

        // Group by source
        Map<String, List<Tuple>> deityBySource = allDeities.stream()
                .collect(Collectors.groupingBy(t -> SourceField.source.getTextOrEmpty(t.node)));

        // Sort the sources by date, descending
        List<String> sourcesByDate = deityBySource.keySet().stream()
                .sorted(byDate.reversed())
                .toList();

        Map<String, Tuple> keepers = new HashMap<>();
        // Iterate over groups of deities in order of publication.
        // Keep the first deity of each name, add others to the remove pile.
        for (String book : sourcesByDate) {
            List<Tuple> deities = deityBySource.remove(book);

            if (keepers.isEmpty()) { // most recent bucket. Keep all.
                deities.forEach(tuple -> {
                    String key = deityKey.apply(tuple);
                    if (Tools5eSources.includedByConfig(tuple.key)) {
                        Tui.instance().logf(Msg.DEITY, " ----  %60s :: %s", tuple.key, key);
                        keepers.put(key, tuple);
                    } else {
                        Tui.instance().logf(Msg.DEITY, "(drop) %s", tuple.key);
                    }
                });
                continue;
            }
            for (Tuple tuple : deities) {
                String key = deityKey.apply(tuple);
                if (Tools5eSources.includedByConfig(tuple.key)) {
                    if (keepers.containsKey(key)) {
                        Tui.instance().logf(Msg.DEITY, "(drop | superseded) %47s => %s", tuple.key, key);
                    } else {
                        keepers.put(key, tuple);
                        Tui.instance().logf(Msg.DEITY, " ----  %60s :: %s", tuple.key, key);
                    }
                } else {
                    Tui.instance().logf(Msg.DEITY, "(drop) %s", tuple.key);
                }
            }
        }
        return keepers.entrySet().stream().map(e -> e.getValue().key).toList();
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
