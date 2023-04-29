package dev.ebullient.convert.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.io.Tui;

public interface NodeReader {

    interface Converter<T extends IndexType> {

        void appendEntryToText(List<String> inner, JsonNode target, String join);

        default Iterable<JsonNode> iterableElements(JsonNode source) {
            return () -> source.elements();
        }

        default String join(String joiner, Collection<String> list) {
            if (list == null || list.isEmpty()) {
                return "";
            }
            return String.join(joiner, list).trim();
        }

        default String joinConjunct(String lastJoiner, List<String> list) {
            return joinConjunct(list, ", ", lastJoiner, false);
        }

        default String joinConjunct(List<String> list, String joiner, String lastJoiner, boolean nonOxford) {
            if (list == null || list.isEmpty()) {
                return "";
            }
            if (list.size() == 1) {
                return list.get(0);
            }
            if (list.size() == 2) {
                return String.join(lastJoiner, list);
            }

            int pause = list.size() - 2;
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < list.size(); ++i) {
                out.append(list.get(i));

                if (i < pause) {
                    out.append(joiner);
                } else if (i == pause) {
                    if (!nonOxford) {
                        out.append(joiner.trim());
                    }
                    out.append(lastJoiner);
                }
            }
            return out.toString();
        }

        String linkify(T type, String s);

        /**
         * Maybe add a blank line to the list containing parsed text.
         * Imperfect, but only add a blank line if the previous line is
         * not already blank.
         *
         * @param text Text to analyze and maybe add a blank line to
         */
        default void maybeAddBlankLine(List<String> text) {
            if (text.size() > 0 && !text.get(text.size() - 1).isBlank()) {
                text.add("");
            }
        }

        String replaceText(String s);

        default String slugify(String s) {
            return Tui.slugify(s);
        }

        default Stream<JsonNode> streamOf(ArrayNode array) {
            return StreamSupport.stream(array.spliterator(), false);
        }

        default Stream<JsonNode> streamOfElements(JsonNode source) {
            if (source == null) {
                return Stream.of();
            }
            return StreamSupport.stream(iterableElements(source).spliterator(), false);
        }

        default List<String> toListOfStrings(JsonNode source) {
            if (source == null) {
                return List.of();
            } else if (source.isTextual()) {
                return List.of(source.asText());
            }
            List<String> list = tui().readJsonValue(source, Tui.LIST_STRING);
            return list == null ? List.of() : list;
        }

        Tui tui();
    }

    interface FieldValue {
        String value();

        default boolean isValueOfField(JsonNode source, NodeReader field) {
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

    default String bonusOrNull(JsonNode x) {
        JsonNode value = getFrom(x);
        if (value == null) {
            return null;
        }
        if (!value.isNumber()) {
            throw new IllegalArgumentException("Can only work with numbers: " + value);
        }
        int n = value.asInt();
        return (n >= 0 ? "+" : "") + n;
    }

    default boolean booleanOrDefault(JsonNode source, boolean value) {
        JsonNode result = getFrom(source);
        return result == null ? value : result.asBoolean(value);
    }

    default boolean existsIn(JsonNode source) {
        return source.has(this.nodeName());
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

    default JsonNode getFromOrEmptyObjectNode(JsonNode source) {
        if (source == null) {
            return Tui.MAPPER.createObjectNode();
        }
        JsonNode result = source.get(this.nodeName());
        return result == null
                ? Tui.MAPPER.createObjectNode()
                : result;
    }

    default JsonNode getFieldFrom(JsonNode source, NodeReader field) {
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
        return text == null ? null : text.asText();
    }

    default int intOrDefault(JsonNode source, int value) {
        JsonNode result = getFrom(source);
        return result == null ? value : result.asInt();
    }

    default <T extends IndexType> List<String> linkifyListFrom(JsonNode node, T type, Converter<T> convert) {
        List<String> list = getListOfStrings(node, convert.tui());
        return list.stream().map(s -> convert.linkify(type, s)).collect(Collectors.toList());
    }

    default String replaceTextFrom(JsonNode node, Converter<?> replacer) {
        return replacer.replaceText(getTextOrEmpty(node));
    }

    default List<String> replaceTextFromList(JsonNode node, Converter<?> convert) {
        List<String> list = getListOfStrings(node, convert.tui());
        return list.stream().map(s -> convert.replaceText(s)).collect(Collectors.toList());
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

    default String transformTextFrom(JsonNode source, String join, Converter<?> replacer) {
        JsonNode target = getFrom(source);
        if (target == null) {
            return null;
        }
        List<String> inner = new ArrayList<>();
        replacer.appendEntryToText(inner, target, join);
        return replacer.join(join, inner);
    }

    default boolean valueEquals(JsonNode previous, JsonNode next) {
        JsonNode prevValue = previous.get(this.nodeName());
        JsonNode nextValue = next.get(this.nodeName());
        return (prevValue == null && nextValue == null)
                || (prevValue != null && nextValue != null && prevValue.equals(nextValue));
    }

    default ArrayNode withArrayFrom(JsonNode source) {
        return source.withArray(this.nodeName());
    }
}
