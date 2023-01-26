package dev.ebullient.convert.tools;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.pf2e.JsonSource.Field;
import dev.ebullient.convert.tools.pf2e.JsonTextReplacement;
import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;

public interface NodeReader {

    String name();

    default String nodeName() {
        return this.name();
    }

    default boolean existsIn(JsonNode source) {
        return source.has(this.nodeName());
    }

    default JsonNode getFrom(JsonNode source) {
        if (source == null) {
            return null;
        }
        return source.get(this.nodeName());
    }

    default JsonNode getFieldFrom(JsonNode source, NodeReader field) {
        JsonNode targetNode = getFrom(source);
        if (targetNode == null) {
            return null;
        }
        return targetNode.get(field.nodeName());
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
        JsonNode target = getFrom(source);
        if (target == null) {
            return List.of();
        } else if (target.isTextual()) {
            return List.of(target.asText());
        }
        List<String> list = fieldFromTo(source, Tui.LIST_STRING, tui);
        return list == null ? List.of() : list;
    }

    default String replaceTextFrom(JsonNode node, JsonTextReplacement replacer) {
        return replacer.replaceText(getTextOrEmpty(node));
    }

    default String transformTextFrom(JsonNode node, String join, Tui tui, JsonTextReplacement replacer) {
        List<String> list = getListOfStrings(node, tui);
        if (list.isEmpty()) {
            return null;
        }
        return list.stream().map(s -> replacer.replaceText(s)).collect(Collectors.joining(join));
    }

    default List<String> transformListFrom(JsonNode node, Tui tui, JsonTextReplacement replacer) {
        List<String> list = getListOfStrings(node, tui);
        return list.stream().map(s -> replacer.replaceText(s)).collect(Collectors.toList());
    }

    default List<String> linkifyListFrom(JsonNode node, Pf2eIndexType type, Tui tui, JsonTextReplacement replacer) {
        List<String> list = getListOfStrings(node, tui);
        return list.stream().map(s -> replacer.linkify(type, s)).collect(Collectors.toList());
    }

    default boolean booleanOrDefault(JsonNode source, boolean value) {
        JsonNode result = getFrom(source);
        return result == null ? value : result.asBoolean(value);
    }

    default int intOrDefault(JsonNode source, int value) {
        JsonNode result = getFrom(source);
        return result == null ? value : result.asInt();
    }

    default Optional<Integer> getIntFrom(JsonNode source) {
        JsonNode result = getFrom(source);
        return result == null ? Optional.empty() : Optional.of(result.asInt());
    }

    default ArrayNode withArrayFrom(JsonNode source) {
        return source.withArray(this.nodeName());
    }

    default Stream<JsonNode> streamOf(JsonNode source) {
        JsonNode result = getFrom(source);
        if (result == null) {
            return Stream.of();
        } else if (result.isObject()) {
            return Stream.of(result);
        }
        return StreamSupport.stream(result.spliterator(), false);
    }

    default <T> T fieldFromTo(JsonNode source, TypeReference<T> targetRef, Tui tui) {
        JsonNode node = source.get(this.nodeName());
        if (node != null) {
            try {
                return Tui.MAPPER.readValue(node.toString(), targetRef);
            } catch (JsonProcessingException e) {
                tui.errorf(e, "Unable to convert field %s from %s", this.nodeName(), node.toString());
            }
        }
        return null;
    }

    default <T> T fieldFromTo(JsonNode source, Class<T> classTarget, Tui tui) {
        JsonNode node = source.get(this.nodeName());
        if (node != null) {
            try {
                return Tui.MAPPER.readValue(node.toString(), classTarget);
            } catch (JsonProcessingException e) {
                tui.errorf(e, "Unable to convert field %s from %s", this.nodeName(), node.toString());
            }
        }
        return null;
    }

    interface FieldValue {
        String value();

        default boolean isValueOfField(JsonNode source, Field field) {
            return matches(field.getTextOrEmpty(source));
        }

        default boolean matches(String value) {
            return this.value().equalsIgnoreCase(value);
        }
    }
}
