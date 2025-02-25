package dev.ebullient.convert.tools.pf2e;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.StringUtil;
import dev.ebullient.convert.tools.pf2e.qute.QuteAffliction;

public class Json2QuteAffliction extends Json2QuteBase {

    private final boolean isEmbedded;

    public Json2QuteAffliction(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode, boolean isEmbedded) {
        super(index, type, rootNode, isEmbedded ? null : Pf2eSources.findOrTemporary(type, rootNode));
        this.isEmbedded = isEmbedded;
    }

    @Override
    protected QuteAffliction buildQuteNote() {
        return Pf2eAffliction.createAffliction(this);
    }

    public enum Pf2eAffliction implements Pf2eJsonNodeReader {
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
        private static QuteAffliction createAffliction(Json2QuteAffliction convert) {
            JsonNode node = convert.rootNode;

            // Sometimes the affliction data is nested as an entry within the parent node.
            Optional<JsonNode> nestedAfflictionNode = Optional.ofNullable(getNestedAffliction(node));
            if (!convert.isEmbedded && nestedAfflictionNode.isEmpty()) {
                // For standalone notes, we should always have a nested affliction node.
                convert.tui().errorf("Unable to extract affliction entry from %s", node.toPrettyString());
                return null;
            }
            JsonNode dataNode = nestedAfflictionNode.orElse(node);

            Optional<String> afflictionLevel = level.intFrom(node).map(Objects::toString);
            afflictionLevel.ifPresent(lv -> convert.tags.add("affliction", "level", lv));

            String temptedCurseText = temptedCurse.transformTextFrom(node, "\n", convert);
            Optional<String> afflictionType = type.getTextFrom(node)
                    .filter(s -> !s.equalsIgnoreCase("affliction"))
                    .filter(StringUtil::isPresent);
            afflictionType.ifPresent(type -> {
                if (isPresent(temptedCurseText)) {
                    convert.tags.add("affliction", type, "tempted");
                } else {
                    convert.tags.add("affliction", type);
                }
            });

            Optional<String> afflictionName = name.getTextFrom(node);

            return new QuteAffliction(
                    convert.sources,
                    // Standalone notes must have a valid affliction name so that we can name the file
                    convert.isEmbedded ? afflictionName.orElse("") : afflictionName.orElseThrow(),
                    // Any entries which were alongside the nested affliction block
                    nestedAfflictionNode.isEmpty()
                            ? List.of()
                            : entries.streamFrom(node)
                                    .filter(Predicate.not(AppendTypeValue.affliction::isBlockTypeOf))
                                    .collect(
                                            ArrayList<String>::new,
                                            (acc, n) -> convert.appendToText(acc, n, "##"),
                                            ArrayList::addAll),
                    convert.tags,
                    convert.traits,
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
                            .or(() -> DC.intFrom(dataNode).map(Objects::toString))
                            .map(StringUtil::isPresent)
                            .map(unused -> new QuteAffliction.QuteAfflictionSave(
                                    DC.intOrNull(dataNode),
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
                    convert.isEmbedded,
                    convert);
        }

        /** Try to extract the affliction node from the entries. Returns null if we couldn't extract one. */
        private static JsonNode getNestedAffliction(JsonNode node) {
            if (!entries.isArrayIn(node)) {
                return null;
            }
            List<JsonNode> topLevelAfflictions = Pf2eAffliction.entries.streamFrom(node)
                    .filter(AppendTypeValue.affliction::isBlockTypeOf).toList();
            if (topLevelAfflictions.size() != 1) {
                return null;
            }
            return topLevelAfflictions.get(0);
        }
    }
}
