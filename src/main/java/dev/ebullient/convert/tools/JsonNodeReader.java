package dev.ebullient.convert.tools;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.join;

import java.util.ArrayList;
import java.util.Arrays;
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

import dev.ebullient.convert.io.Tui;

public interface JsonNodeReader {

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

    /**
     * Return the boolean value of the field in the node:
     * - if the field is a boolean, return the value
     * - if the field is a string, return true if the string is present and not 'false'
     *
     * @param source
     * @param value
     * @return
     */
    default boolean coerceBooleanOrDefault(JsonNode source, boolean value) {
        JsonNode result = getFrom(source);
        if (result == null) {
            return value;
        }
        if (result.isBoolean()) {
            return result.asBoolean();
        }
        if (result.isTextual()) {
            return !result.asText().equalsIgnoreCase("false");
        }
        return value;
    }

    default boolean booleanOrDefault(JsonNode source, boolean value) {
        JsonNode result = getFrom(source);
        return result == null ? value : result.asBoolean(value);
    }

    default Double doubleOrDefault(JsonNode source, double value) {
        JsonNode result = getFrom(source);
        return result == null ? value : result.asDouble();
    }

    default Double doubleOrNull(JsonNode source) {
        JsonNode result = getFrom(source);
        return result == null ? null : result.asDouble();
    }

    default boolean existsIn(JsonNode source) {
        if (source == null || source.isNull()) {
            return false;
        }
        return source.has(this.nodeName());
    }

    default boolean nestedExistsIn(JsonNodeReader field, JsonNode source) {
        if (source == null || source.isNull()) {
            return false;
        }
        JsonNode parent = field.getFrom(source);
        return this.existsIn(parent);
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

    default JsonNode getFrom(JsonNode source) {
        if (source == null) {
            return null;
        }
        return source.get(this.nodeName());
    }

    /**
     * Return an optional of the object at this key in the node, or an empty optional if the key does not exist or is not an
     * object.
     */
    default Optional<JsonNode> getObjectFrom(JsonNode source) {
        return this.isObjectIn(source) ? Optional.of(this.getFrom(source)) : Optional.empty();
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

    default List<String> getListOfStrings(JsonNode source, Tui tui) {
        JsonNode target = getFrom(source);
        if (target == null || target.isNull()) {
            return List.of();
        }
        if (target.isTextual()) {
            return List.of(target.asText());
        }
        if (target.isObject()) {
            throw new IllegalArgumentException(
                    "Unexpected object when creating list of strings: %s".formatted(
                            source));
        }
        List<String> list = fieldFromTo(source, Tui.LIST_STRING, tui);
        return list == null ? List.of() : list;
    }

    default Map<String, String> getMapOfStrings(JsonNode source, Tui tui) {
        JsonNode target = getFrom(source);
        if (target == null || target.isNull()) {
            return Map.of();
        }
        if (target.isTextual() || target.isArray()) {
            throw new IllegalArgumentException(
                    "Unexpected text or array when creating map of strings: %s".formatted(
                            source));
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

    default Optional<String> getTextFrom(JsonNode x) {
        if (x != null && existsIn(x) && getFrom(x).isTextual()) {
            return Optional.of(getFrom(x).asText());
        }
        return Optional.empty();
    }

    default Optional<Integer> intFrom(JsonNode source) {
        JsonNode result = getFrom(source);
        return result == null || !result.isInt() ? Optional.empty() : Optional.of(result.asInt());
    }

    default int intOrDefault(JsonNode source, int value) {
        JsonNode result = getFrom(source);
        return result == null || result.isNull() ? value : result.asInt();
    }

    default Integer intOrNull(JsonNode source) {
        JsonNode result = getFrom(source);
        return result == null || result.isNull() ? null : result.asInt();
    }

    default int intOrThrow(JsonNode x) {
        JsonNode result = getFrom(x);
        if (result == null) {
            throw new IllegalArgumentException("Missing int from " + this.nodeName());
        }
        return result.asInt();
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

    /** Wrapper around {@link #streamPropsExcluding(JsonNode, JsonNodeReader...)} to make the naming less confusing */
    default Stream<Entry<String, JsonNode>> streamProps(JsonNode source) {
        return streamPropsExcluding(source);
    }

    /** Returns a stream of entries of (key, node) from the given node, excluding the given keys. */
    default Stream<Entry<String, JsonNode>> streamPropsExcluding(JsonNode source, JsonNodeReader... excludingKeys) {
        JsonNode result = getFrom(source);
        if (result == null || !result.isObject()) {
            return Stream.of();
        }
        return result.properties().stream()
                .filter(e -> Arrays.stream(excludingKeys).noneMatch(s -> e.getKey().equalsIgnoreCase(s.name())));
    }

    /**
     * {@link #transformTextFrom(JsonNode, String, JsonTextConverter, String)} with a null heading.
     *
     * @see #transformTextFrom(JsonNode, String, JsonTextConverter)
     */
    default String transformTextFrom(JsonNode source, String join, JsonTextConverter<?> replacer) {
        return transformTextFrom(source, join, replacer, null);
    }

    /**
     * Read the field in from the source as a potentially-nested array of entries. This calls
     * {@link JsonTextConverter#appendToText(List, JsonNode, String)} on the input node and returns
     * the parsed result joined according to the given delimiter.
     *
     * @param source The node to read from
     * @param delimiter The delimiter to use when joining the entries into a single string
     * @param replacer The {@link JsonTextConverter} to use for parsing entries.
     * @param heading The heading to pass to {@link JsonTextConverter#appendToText(List, JsonNode, String)}
     */
    default String transformTextFrom(JsonNode source, String delimiter, JsonTextConverter<?> replacer, String heading) {
        JsonNode target = getFrom(source);
        if (target == null) {
            return "";
        }
        List<String> inner = new ArrayList<>();
        replacer.appendToText(inner, target, heading);
        return join(delimiter, inner);
    }

    /**
     * Parse this field from {@code source} as potentially-nested array of entries, and return a list of strings. This
     * calls {@link JsonTextConverter#appendToText(List, JsonNode, String)} to recursively parse the input.
     */
    default List<String> transformListFrom(JsonNode source, JsonTextConverter<?> convert) {
        if (!isArrayIn(source)) {
            return List.of();
        }
        List<String> inner = new ArrayList<>();
        convert.appendToText(inner, getFrom(source), null);
        return inner;
    }

    /** Returns the enum value of {@code enumClass} that this field in {@code source} contains, or null. */
    default <E extends Enum<E>> E getEnumValueFrom(JsonNode source, Class<E> enumClass) {
        String value = getTextOrNull(source);
        if (!isPresent(value)) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value.toLowerCase());
        } catch (IllegalArgumentException ignored) {
        }
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /** An enum which implements this interface should have values which represent different JSON field values. */
    interface FieldValue {
        String name();

        default String value() {
            return name();
        }

        default boolean isValueOfField(JsonNode source, JsonNodeReader field) {
            return matches(field.getTextOrEmpty(source));
        }

        default boolean matches(String value) {
            return this.value().equalsIgnoreCase(value) || this.name().equalsIgnoreCase(value);
        }

        static <E extends Enum<E> & FieldValue> E valueFrom(String value, Class<E> enumClass) {
            if (!isPresent(value)) {
                return null;
            }
            return Arrays.stream(enumClass.getEnumConstants()).filter(e -> e.matches(value)).findAny().orElse(null);
        }
    }

    default boolean valueEquals(JsonNode previous, JsonNode next) {
        if (previous == null || next == null) {
            return true;
        }
        JsonNode prevValue = previous.get(this.nodeName());
        JsonNode nextValue = next.get(this.nodeName());
        return (prevValue == null && nextValue == null)
                || (prevValue != null && prevValue.equals(nextValue));
    }

    /**
     * Will always return an array (no null checks)
     * Does not create an array attribute if the element is not present
     */
    default ArrayNode readArrayFrom(JsonNode source) {
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
            return () -> source.get(this.nodeName()).properties().iterator();
        }
        return List.of();
    }

    /**
     * Will always return an array (no null checks)
     * Will create an array attribute if the element is not present
     */
    default ArrayNode ensureArrayIn(JsonNode target) {
        if (target == null) {
            return Tui.MAPPER.createArrayNode();
        }
        return target.withArrayProperty(this.nodeName());
    }

    default JsonNode copyFrom(JsonNode source) {
        return getFrom(source).deepCopy();
    }

    /** Destructive! */
    default JsonNode removeFrom(JsonNode target) {
        if (target == null) {
            return null;
        }
        return ((ObjectNode) target).remove(this.nodeName());
    }

    /** Destructive! */
    default void setIn(JsonNode target, JsonNode value) {
        ((ObjectNode) target).set(this.nodeName(), value);
    }

    /** Destructive! */
    default void setIn(JsonNode target, String value) {
        ((ObjectNode) target).put(this.nodeName(), value);
    }

    /** Destructive! */
    default void setIn(JsonNode target, boolean b) {
        ((ObjectNode) target).put(this.nodeName(), b);
    }

    /** Destructive! */
    default void copy(JsonNode source, JsonNode target) {
        if (source == null || target == null) {
            return;
        }
        if (!source.has(this.nodeName())) {
            return;
        }
        ((ObjectNode) target).set(this.nodeName(), getFrom(source).deepCopy());
    }

    /** Destructive! */
    default void link(JsonNode source, JsonNode target) {
        if (source == null || target == null) {
            return;
        }
        if (!source.has(this.nodeName())) {
            return;
        }
        ((ObjectNode) target).set(this.nodeName(), getFrom(source));
    }

    /** Destructive! */
    default void moveFrom(JsonNode source, JsonNode target) {
        if (source == null || target == null) {
            return;
        }
        JsonNode value = removeFrom(source);
        if (value != null) {
            ((ObjectNode) target).set(this.nodeName(), value);
        }
    }

    /** Destructive! */
    default void appendToArray(JsonNode target, String value) {
        if (target == null) {
            return;
        }
        ArrayNode array = ensureArrayIn(target).add(value);
        setIn(target, array);
    }

    /** Destructive! */
    default void appendToArray(JsonNode target, JsonNode value) {
        if (target == null || value == null) {
            return;
        }
        ArrayNode array = ensureArrayIn(target);
        if (value.isArray()) {
            array.addAll((ArrayNode) value);
        } else {
            array.add(value);
        }
        setIn(target, array);
    }
}
