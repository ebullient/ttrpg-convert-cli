package dev.ebullient.convert.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import dev.ebullient.convert.io.Tui;

public interface JsonNodeReader {

    interface FieldValue {
        String value();

        default String toAnchorTag(String x) {
            return Tui.toAnchorTag(x);
        }

        default boolean isValueOfField(JsonNode source, JsonNodeReader field) {
            return matches(field.getTextOrEmpty(source));
        }

        default boolean matches(String value) {
            return this.value().equalsIgnoreCase(value);
        }
    }

    String name();

    default String nodeName() {
        return this.name();
    }

    default void debugIfExists(JsonNode node, Tui tui) {
        if (existsIn(node)) {
            tui.errorf(this.name() + " is defined in " + node.toPrettyString());
        }
    }

    default void appendUnlessEmptyFrom(JsonNode x, List<String> text) {
        String value = getTextOrEmpty(x);
        if (!value.isEmpty()) {
            text.add(value);
        }
    }

    default void appendUnlessEmptyFrom(JsonNode x, List<String> text, JsonTextConverter<?> replacer) {
        String value = replacer.replaceText(getTextOrEmpty(x));
        if (!value.isEmpty()) {
            text.add(value);
        }
    }

    default String bonusOrNull(JsonNode x) {
        JsonNode value = getFrom(x);
        if (value == null) {
            return null;
        }
        if (!value.isNumber()) {
            throw new IllegalArgumentException("bonusOrNull can only work with numbers: " + value);
        }
        int n = value.asInt();
        return (n >= 0 ? "+" : "") + n;
    }

    default boolean booleanOrDefault(JsonNode source, boolean value) {
        JsonNode result = getFrom(source);
        return result == null ? value : result.asBoolean(value);
    }

    default boolean existsIn(JsonNode source) {
        if (source == null || source.isNull()) {
            return false;
        }
        return source.has(this.nodeName());
    }

    default boolean isArrayIn(JsonNode source) {
        if (source == null || !source.has(this.nodeName())) {
            return false;
        }
        return source.get(this.nodeName()).isArray();
    }

    default boolean isObjectIn(JsonNode source) {
        if (source == null || !source.has(this.nodeName())) {
            return false;
        }
        return source.get(this.nodeName()).isObject();
    }

    default <T> T fieldFromTo(JsonNode source, Class<T> classTarget, Tui tui) {
        return tui.readJsonValue(source.get(this.nodeName()), classTarget);
    }

    default <T> T fieldFromTo(JsonNode source, TypeReference<T> targetRef, Tui tui) {
        return tui.readJsonValue(source.get(this.nodeName()), targetRef);
    }

    default ArrayNode arrayFrom(JsonNode source) {
        if (source == null) {
            return null;
        }
        return source.withArray(this.nodeName());
    }

    default JsonNode getFrom(JsonNode source) {
        if (source == null) {
            return null;
        }
        return source.get(this.nodeName());
    }

    default JsonNode getFromOrEmptyObjectNode(JsonNode source) {
        if (source == null) {
            return Tui.MAPPER.createObjectNode();
        }
        JsonNode result = source.get(this.nodeName());
        return result == null
                ? Tui.MAPPER.createObjectNode()
                : result;
    }

    /**
     * Find the first element in the array of this property.
     * Useful for elements that are ceremonial arrays (they
     * are always an array of one element)
     */
    default JsonNode getFirstFromArray(JsonNode source) {
        if (source == null) {
            return null;
        }
        JsonNode result = source.get(this.nodeName());
        if (result == null || !result.isArray()) {
            return null;
        }
        return result.get(0);
    }

    default JsonNode getFieldFrom(JsonNode source, JsonNodeReader field) {
        JsonNode targetNode = getFrom(source);
        if (targetNode == null) {
            return null;
        }
        return targetNode.get(field.nodeName());
    }

    default Optional<Integer> getIntFrom(JsonNode source) {
        JsonNode result = getFrom(source);
        return result == null ? Optional.empty() : Optional.of(result.asInt());
    }

    default int getIntOrThrow(JsonNode x) {
        JsonNode result = getFrom(x);
        if (result == null) {
            throw new IllegalArgumentException("Missing int from " + this.nodeName());
        }
        return result.asInt();
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

    default Map<String, String> getMapOfStrings(JsonNode source, Tui tui) {
        JsonNode target = getFrom(source);
        if (target == null) {
            return Map.of();
        }
        Map<String, String> map = fieldFromTo(source, Tui.MAP_STRING_STRING, tui);
        return map == null ? Map.of() : map;
    }

    default String getTextOrDefault(JsonNode x, String value) {
        String text = getTextOrNull(x);
        return text == null ? value : text;
    }

    default String getTextOrEmpty(JsonNode x) {
        String text = getTextOrNull(x);
        return text == null ? "" : text;
    }

    default String getTextOrNull(JsonNode x) {
        JsonNode text = getFrom(x);
        return text == null || text.isNull() ? null : text.asText();
    }

    default String getTextOrThrow(JsonNode x) {
        String text = getTextOrNull(x);
        if (text == null) {
            throw new IllegalArgumentException("Missing text from " + this.nodeName());
        }
        return text;
    }

    default int intOrDefault(JsonNode source, int value) {
        JsonNode result = getFrom(source);
        return result == null || result.isNull() ? value : result.asInt();
    }

    default String joinAndReplace(JsonNode source, JsonTextConverter<?> replacer) {
        return joinAndReplace(source, replacer, ", ");
    }

    default String joinAndReplace(JsonNode source, JsonTextConverter<?> replacer, String join) {
        JsonNode array = getFrom(source);
        if (array == null || array.isNull()) {
            return "";
        } else if (!array.isArray()) {
            throw new IllegalArgumentException("joinAndReplace can only work with arrays: " + array);
        }
        return StreamSupport.stream(array.spliterator(), false)
                .map(v -> replacer.replaceText(v.asText()))
                .collect(Collectors.joining(join));
    }

    default <T extends IndexType> List<String> linkifyListFrom(JsonNode node, T type, JsonTextConverter<T> convert) {
        List<String> list = getListOfStrings(node, convert.tui());
        return list.stream().map(s -> convert.linkify(type, s)).toList();
    }

    default String replaceTextFrom(JsonNode node, JsonTextConverter<?> replacer) {
        return replacer.replaceText(getTextOrEmpty(node));
    }

    default List<String> replaceTextFromList(JsonNode node, JsonTextConverter<?> convert) {
        List<String> list = getListOfStrings(node, convert.tui());
        return list.stream().map(convert::replaceText).toList();
    }

    default int size(JsonNode source) {
        JsonNode target = getFrom(source);
        if (target == null) {
            return 0;
        }
        return target.size();
    }

    default Stream<JsonNode> streamFrom(JsonNode source) {
        JsonNode result = getFrom(source);
        if (result == null) {
            return Stream.of();
        } else if (result.isObject()) {
            return Stream.of(result);
        }
        return StreamSupport.stream(result.spliterator(), false);
    }

    default String transformTextFrom(JsonNode source, String join, JsonTextConverter<?> replacer) {
        JsonNode target = getFrom(source);
        if (target == null) {
            return null;
        }
        List<String> inner = new ArrayList<>();
        replacer.appendToText(inner, target, null);
        return replacer.join(join, inner.stream().filter(x -> !x.isBlank()).toList());
    }

    default boolean valueEquals(JsonNode previous, JsonNode next) {
        JsonNode prevValue = previous.get(this.nodeName());
        JsonNode nextValue = next.get(this.nodeName());
        return (prevValue == null && nextValue == null)
                || (prevValue != null && prevValue.equals(nextValue));
    }

    default ArrayNode withArrayFrom(JsonNode source) {
        if (isArrayIn(source)) {
            return source.withArray(this.nodeName());
        }
        return Tui.MAPPER.createArrayNode();
    }

    default Iterable<JsonNode> iterateArrayFrom(JsonNode source) {
        if (isArrayIn(source)) {
            return () -> source.withArray(this.nodeName()).elements();
        }
        return List.of();
    }

    default Iterable<Entry<String, JsonNode>> iterateFieldsFrom(JsonNode source) {
        if (isObjectIn(source)) {
            return () -> source.get(this.nodeName()).fields();
        }
        return List.of();
    }

    /** Destructive! */
    default void removeFrom(JsonNode target) {
        if (target == null) {
            return;
        }
        ((ObjectNode) target).remove(this.nodeName());
    }

    /** Destructive! */
    default void setIn(JsonNode target, JsonNode value) {
        ((ObjectNode) target).set(this.nodeName(), value);
    }

    /** Destructive! */
    default void setIn(JsonNode target, String value) {
        ((ObjectNode) target).set(this.nodeName(), new TextNode(value));
    }
}
