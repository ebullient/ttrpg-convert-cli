package dev.ebullient.convert.tools.pf2e;

import java.util.List;

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

    default boolean existsIn(JsonNode source) {
        return source.has(this.nodeName());
    }

    default JsonNode getFrom(JsonNode source) {
        return source.get(this.nodeName());
    }

    default String getTextOrNull(JsonNode x) {
        JsonNode text = getFrom(x);
        return text == null ? null : text.asText();
    }

    default String getTextOrEmpty(JsonNode x) {
        String text = getTextOrNull(x);
        return text == null ? "" : text;
    }

    default String getTextOrDefault(JsonNode x, String value) {
        String text = getTextOrNull(x);
        return text == null ? value : text;
    }

    default List<String> getListOfStrings(JsonNode source, Tui tui) {
        JsonNode result = source.get(this.nodeName());
        if (result == null) {
            return List.of();
        } else if (result.isTextual()) {
            return List.of(result.asText());
        } else {
            return fieldFromTo(source, Tui.LIST_STRING, tui);
        }
    }

    default boolean booleanOrDefault(JsonNode source, boolean value) {
        JsonNode result = source.get(this.nodeName());
        return result == null ? value : result.asBoolean(value);
    }

    default int intOrDefault(JsonNode source, int value) {
        JsonNode result = source.get(this.nodeName());
        return result == null ? value : result.asInt();
    }

    default ArrayNode withArrayFrom(JsonNode source) {
        return source.withArray(this.nodeName());
    }

    default <T> T fieldFromTo(JsonNode source, TypeReference<T> target, Tui tui) {
        JsonNode node = source.get(this.nodeName());
        if (node != null) {
            try {
                return Tui.MAPPER.readValue(node.toString(), target);
            } catch (JsonProcessingException e) {
                tui.errorf(e, "Unable to convert field %s from %s", this.nodeName(), node.toString());
            }
        }
        return null;
    }

    default <T> T fieldFromTo(JsonNode source, Class<T> target, Tui tui) {
        JsonNode node = source.get(this.nodeName());
        if (node != null) {
            try {
                return Tui.MAPPER.readValue(node.toString(), target);
            } catch (JsonProcessingException e) {
                tui.errorf(e, "Unable to convert field %s from %s", this.nodeName(), node.toString());
            }
        }
        return null;
    }

    interface FieldValue {
        String value();

        default boolean isValueOfField(JsonNode source, Field field) {
            return this.value().equals(field.getTextOrNull(source));
        }
    }
}
