package dev.ebullient.convert.tools.pf2e;

import com.fasterxml.jackson.databind.JsonNode;
import dev.ebullient.convert.tools.JsonNodeReader;
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
}
