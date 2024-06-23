package dev.ebullient.convert.tools;

import com.fasterxml.jackson.databind.JsonNode;

public interface IndexType {

    String name();

    String templateName();

    String defaultSourceString();

    /** Return the key for the given node in the index. */
    String createKey(JsonNode node);
}
