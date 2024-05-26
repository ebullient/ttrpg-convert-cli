package dev.ebullient.convert.tools.pf2e;

import com.fasterxml.jackson.databind.JsonNode;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.JsonTextConverter;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataFrequency;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataSpeed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** A utility class which extends {@link JsonNodeReader} with PF2e-specific functionality. */
public interface Pf2eJsonNodeReader extends JsonNodeReader {

    /**
     * Return alignments as a list of formatted strings from this field in the given node.
     * Returns an empty list if we couldn't get alignments.
     */
    default List<String> getAlignmentsFrom(JsonNode alignNode, JsonSource convert) {
        return streamFrom(alignNode)
                .map(JsonNode::asText)
                .map(a -> a.length() > 2 ? a : convert.linkifyTrait(a.toUpperCase()))
                .toList();
    }

    /** Return a {@link QuteDataSpeed} read from this field of the {@code source} node, or null. */
    default QuteDataSpeed getSpeedFrom(JsonNode source, JsonSource convert) {
        return getObjectFrom(source).map(n -> Pf2eSpeed.read(n, convert)).orElse(null);
    }

    /**
     * A {@link Pf2eJsonNodeReader} which reads JSON like the following:
     *
     * <pre>
     *     {
     *         "walk": 10,
     *         "fly": 20,
     *         "speedNote": "(with fly spell)",
     *         "abilities": "air walk"
     *     }
     * </pre>
     */
    enum Pf2eSpeed implements Pf2eJsonNodeReader {
        walk,
        speedNote,
        abilities;

        /** Read a {@link QuteDataSpeed} from the {@code source} node. */
        private static QuteDataSpeed read(JsonNode source, JsonSource convert) {
            return new QuteDataSpeed(
                    walk.getIntFrom(source).orElse(null),
                    convert.streamPropsExcluding(source, speedNote, abilities)
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().asInt())),
                    speedNote.getTextFrom(source)
                            .map(convert::replaceText)
                            // Remove parens around the note
                            .map(s -> s.replaceFirst("^\\((%s)\\)$", "\1"))
                            .map(List::of).orElse(List.of()),
                    // Specifically make this mutable because we later need to add additional abilities for deities
                    new ArrayList<>(abilities.replaceTextFromList(source, convert)));
        }
    }

    /** Return a {@link QuteDataFrequency} read from {@code source}, or null. */
    default QuteDataFrequency getFrequencyFrom(JsonNode source, JsonSource convert) {
        return getObjectFrom(source).map(n -> Pf2eFrequency.read(n, convert)).orElse(null);
    }

    /**
     * A {@link Pf2eJsonNodeReader} which reads JSON like the following:
     *
     * <pre>
     *     "unit": "round",
     *     "number": 1,
     *     "recurs": true,
     *     "overcharge": true,
     *     "interval": 2,
     * </pre>
     *
     * <p>Or, with a custom unit:</p>
     * <pre>
     *     "customUnit": "gem",
     *     "number": 1,
     *     "recurs": true,
     *     "overcharge": true,
     *     "interval": 2,
     * </pre>
     *
     * <p>Or, for a special frequency with a custom string:</p>
     * <pre>
     *     "special": "once per day, and recharges when the great cyclops uses Ferocity"
     * </pre>
     */
    enum Pf2eFrequency implements Pf2eJsonNodeReader {
        special,
        number,
        recurs,
        overcharge,
        interval,
        unit,
        customUnit;

        /** Return a {@link QuteDataFrequency} read from {@code node}. */
        private static QuteDataFrequency read(JsonNode node, JsonTextConverter<?> convert) {
            if (special.getTextFrom(node).isPresent()) {
                return new QuteDataFrequency(special.replaceTextFrom(node, convert));
            }
            return new QuteDataFrequency(
                    // This should usually be an integer, but some entries deviate from the schema and use a word
                    number.getIntFrom(node).orElseGet(() -> {
                        // Try to coerce the word back into a number, and otherwise log an error and give 0
                        String freqString = number.getTextOrThrow(node).trim();
                        if (freqString.equalsIgnoreCase("once")) {
                            return 1;
                        }
                        convert.tui().errorf("Got unexpected frequency value \"%s\"", freqString);
                        return 0;
                    }),
                    interval.getIntFrom(node).orElse(null),
                    unit.getTextFrom(node).orElseGet(() -> customUnit.getTextOrThrow(node)),
                    recurs.booleanOrDefault(node, false),
                    overcharge.booleanOrDefault(node, false));
        }
    }
}
