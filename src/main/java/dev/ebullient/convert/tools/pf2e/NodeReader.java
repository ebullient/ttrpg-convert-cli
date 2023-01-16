package dev.ebullient.convert.tools.pf2e;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.pf2e.JsonSource.Field;

public interface NodeReader {

    String name();

    default String nodeName() {
        return this.name();
    }

    default boolean existsIn(JsonNode node) {
        return node.has(this.nodeName());
    }

    default boolean isFieldValue(JsonNode source, Field field) {
        return this.nodeName().equals(field.getTextOrNull(source));
    }

    default JsonNode getFrom(JsonNode node) {
        return node.get(this.nodeName());
    }

    default <T> T fromTo(JsonNode node, TypeReference<T> target, Tui tui) {
        try {
            return Tui.MAPPER.readValue(node.toString(), target);
        } catch (JsonProcessingException e) {
            tui.errorf(e, "Unable to convert %s", node.toString());
        }
        return null;
    }

    default <T> T fromTo(JsonNode node, Class<T> target, Tui tui) {
        try {
            return Tui.MAPPER.readValue(node.toString(), target);
        } catch (JsonProcessingException e) {
            tui.errorf(e, "Unable to convert %s", node.toString());
        }
        return null;
    }

    default ArrayNode withArrayFrom(JsonNode node) {
        return node.withArray(this.nodeName());
    }

    default String getTextOrNull(JsonNode x) {
        if (x.has(this.nodeName())) {
            return x.get(this.nodeName()).asText();
        }
        return null;
    }

    default String getTextOrEmpty(JsonNode x) {
        if (x.has(this.nodeName())) {
            return x.get(this.nodeName()).asText();
        }
        return "";
    }

    default String getTextOrDefault(JsonNode x, String value) {
        if (x.has(this.nodeName())) {
            return x.get(this.nodeName()).asText();
        }
        return value;
    }

    default String getOrEmptyIfEqual(JsonNode x, String expected) {
        if (x.has(this.nodeName())) {
            String value = x.get(this.nodeName()).asText().trim();
            return value.equalsIgnoreCase(expected) ? "" : value;
        }
        return "";
    }

    default boolean booleanOrDefault(JsonNode source, boolean value) {
        JsonNode result = source.get(this.nodeName());
        return result == null ? value : result.asBoolean(value);
    }

    default int intOrDefault(JsonNode source, int value) {
        JsonNode result = source.get(this.nodeName());
        return result == null ? value : result.asInt();
    }
}
