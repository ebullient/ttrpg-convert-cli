package dev.ebullient.convert.tools.pf2e;

import com.fasterxml.jackson.databind.JsonNode;
import dev.ebullient.convert.tools.JsonNodeReader;

import java.util.List;

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
}
