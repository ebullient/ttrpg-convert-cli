package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.StringUtil;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.QuteAffliction;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.toTitleCase;

public class Json2QuteAffliction extends Json2QuteBase {

    public Json2QuteAffliction(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode) {
        super(index, type, rootNode);
    }

    @Override
    protected QuteAffliction buildQuteNote() {
        return Pf2eAffliction.createAffliction(rootNode, this, getSources());
    }

    public enum Pf2eAffliction implements JsonNodeReader {
        name,
        DC,
        duration,
        level,
        maxDuration,
        note,
        onset,
        savingThrow,
        stage,
        stages,
        temptedCurse,
        type,
        entries,
        entry;

        public static boolean isAfflictionBlock(JsonNode node) {
            return type.getTextFrom(node).map(s -> s.equals("affliction")).orElse(false);
        }

        /**
         * Example JSON input, with an embedded affliction that does not have nested affliction data:
         *
         * <pre>
         *     "type": "affliction",
         *     "name": "Goblin Pox",
         *     "traits": ["disease"],
         *     "level": 1,
         *     "DC": 22,
         *     "onset": "1d4 days"
         *     "savingThrow": "Will",
         *     "note": "Goblins and dogs are immune",
         *     "stages": [
         *       {"stage": 1, "entry": "sickened 1", "duration": "1 round"},
         *       {"stage": 2, "entry": "sickened 1 and slowed 1", "duration": "1 round"}
         *     ],
         *     "entries": [
         *       "Whenever you gain the frightened condition while in the Baffled Lowlands, increase the value by 1"
         *     ],
         * </pre>
         * <p>
         * Example JSON input, with a standalone affliction that does have nested affliction data:
         * </p>
         *
         * <pre>
         *     "name": "Blightburn Sickness",
         *     "source": "TV",
         *     "page": 45,
         *     "type": "Disease",
         *     "traits": ["disease"],
         *     "level": "Level Varies",
         *     "entries": [
         *       "Caused by exposure to blightburn crystal, blightburn sickness burns and dissolves from within.",
         *       {
         *         "type": "affliction",
         *         "onset": "1d4 days",
         *         "DC": 22,
         *         "savingThrow": "Will",
         *         "note": "Goblins and dogs are immune",
         *         "stages": [
         *           {"stage": 1, "entry": "sickened 1", "duration": "1 round"},
         *           {"stage": 2, "entry": "sickened 1 and slowed 1", "duration": "1 round"}
         *         ],
         *         "entries": [
         *           "Whenever you gain the frightened condition while in the Baffled Lowlands, increase the value by 1"
         *         ],
         *       }
         *     ],
         * </pre>
         */
        private static QuteAffliction createAffliction(
                JsonNode node, JsonSource convert, Pf2eSources sources) {
            boolean isEmbedded = sources == null;

            // Sometimes the affliction data is nested as an entry within the parent node.
            Optional<JsonNode> nestedAfflictionNode = Optional.ofNullable(getNestedAffliction(node));
            if (!isEmbedded && nestedAfflictionNode.isEmpty()) {
                // For standalone notes, we should always have a nested affliction node.
                convert.tui().errorf("Unable to extract affliction entry from %s", node.toPrettyString());
                return null;
            }
            JsonNode dataNode = nestedAfflictionNode.orElse(node);

            Tags tags = new Tags(sources);
            Collection<String> traits = convert.collectTraitsFrom(node, tags);

            Optional<String> afflictionLevel = level.getIntFrom(node).map(Objects::toString);
            afflictionLevel.ifPresent(lv -> tags.add("affliction", "level", lv));

            String temptedCurseText = temptedCurse.transformTextFrom(node, "\n", convert);
            Optional<String> afflictionType = type.getTextFrom(node)
                    .filter(s -> !s.equalsIgnoreCase("affliction"))
                    .filter(StringUtil::isPresent);
            afflictionType.ifPresent(type -> {
                if (isPresent(temptedCurseText)) {
                    tags.add("affliction", type, "tempted");
                } else {
                    tags.add("affliction", type);
                }
            });

            Optional<String> afflictionName = name.getTextFrom(node);

            return new QuteAffliction(
                    sources,
                    // Standalone notes must have a valid affliction name so that we can name the file
                    isEmbedded ? afflictionName.orElse("") : afflictionName.orElseThrow(),
                    // Any entries which were alongside the nested affliction block
                    nestedAfflictionNode.isEmpty()
                            ? List.of()
                            : entries.streamFrom(node)
                                    .filter(Predicate.not(Pf2eAffliction::isAfflictionBlock))
                                    .collect(
                                            ArrayList<String>::new,
                                            (acc, n) -> convert.appendToText(acc, n, "##"),
                                            ArrayList::addAll),
                    tags,
                    traits,
                    Field.alias.replaceTextFromList(dataNode, convert),
                    // Level may be e.g. "varies"
                    afflictionLevel.or(() -> level.getTextFrom(node))
                            // Fix some data irregularities
                            .map(s -> s.equalsIgnoreCase(", level varies") ? "Level Varies" : s)
                            .orElse(null),
                    afflictionType.orElse(null),
                    maxDuration.replaceTextFrom(dataNode, convert),
                    onset.replaceTextFrom(dataNode, convert),
                    // DC can be either an int or a custom string. Only populate the object if any of the contained
                    // fields are present
                    savingThrow.getTextFrom(dataNode)
                            .or(() -> DC.getTextFrom(dataNode))
                            .or(() -> DC.getIntFrom(dataNode).map(Objects::toString))
                            .map(StringUtil::isPresent)
                            .map(unused -> new QuteAffliction.QuteAfflictionSave(
                                    DC.getIntFrom(dataNode).orElse(null),
                                    savingThrow.getTextFrom(dataNode)
                                            .map(s -> s.contains(" ") ? s : toTitleCase(s)).orElse(null),
                                    DC.getTextFrom(dataNode).map(convert::replaceText).orElse(null)))
                            .orElse(null),
                    entries.transformTextFrom(dataNode, "\n", convert),
                    temptedCurseText,
                    note.getTextFrom(dataNode).map(convert::replaceText).map(List::of).orElse(List.of()),
                    stages.streamFrom(dataNode)
                            .map(n -> Map.entry("Stage %s".formatted(stage.getTextOrDefault(n, "1")), n))
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> new QuteAffliction.QuteAfflictionStage(
                                            duration.replaceTextFrom(e.getValue(), convert),
                                            entry.transformTextFrom(e.getValue(), "\n", convert, e.getKey())),
                                    (x, y) -> y,
                                    LinkedHashMap::new)),
                    isEmbedded,
                    convert);
        }

        static QuteAffliction createInlineAffliction(JsonNode node, JsonSource convert) {
            return createAffliction(node, convert, null);
        }

        /** Try to extract the affliction node from the entries. Returns null if we couldn't extract one. */
        private static JsonNode getNestedAffliction(JsonNode node) {
            if (!entries.isArrayIn(node)) {
                return null;
            }
            List<JsonNode> topLevelAfflictions = Pf2eAffliction.entries.streamFrom(node)
                    .filter(Pf2eAffliction::isAfflictionBlock).toList();
            if (topLevelAfflictions.size() != 1) {
                return null;
            }
            return topLevelAfflictions.get(0);
        }
    }
}
