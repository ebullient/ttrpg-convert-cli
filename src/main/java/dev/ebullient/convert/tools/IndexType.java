package dev.ebullient.convert.tools;

import com.fasterxml.jackson.databind.JsonNode;

public interface IndexType {

    String name();

    String templateName();

    String defaultSourceString();

    /** Return a key which can be used to look up the given node of this type from the index. */
    String createKey(JsonNode key);

    /** As {@link #createKey(JsonNode)}, but use the given name and source instead of retrieving it from the node. */
    String createKey(String name, String source);
}
