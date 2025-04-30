package dev.ebullient.convert.qute;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import dev.ebullient.convert.io.JavadocIgnore;
import dev.ebullient.convert.tools.IndexType;

@JavadocIgnore
public interface QuteUtil {
    default boolean isPresent(Map<?, ?> s) {
        return s != null && !s.isEmpty();
    }

    default boolean isPresent(Collection<?> s) {
        return s != null && !s.isEmpty();
    }

    default boolean isPresent(String s) {
        return s != null && !s.isBlank();
    }

    default boolean isPresent(Object s) {
        return s != null;
    }

    default void addIntegerUnlessEmpty(Map<String, Object> map, String key, Integer value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    default void maybeAddBlankLine(List<String> content) {
        if (content.size() > 0 && !content.get(content.size() - 1).isBlank()) {
            content.add("");
        }
    }

    default void addUnlessEmpty(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    default void addUnlessEmpty(Map<String, Object> map, String key, Collection<?> value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
    }

    default String levelToString(String level) {
        switch (level) {
            case "1":
                return "1st";
            case "2":
                return "2nd";
            case "3":
                return "3rd";
            default:
                return level + "th";
        }
    }

    default String template() {
        throw new UnsupportedOperationException("Tried to call template() on a class which does not have a template defined");
    }

    default IndexType indexType() {
        throw new UnsupportedOperationException("Tried to call indexType() on a class which does not have a template defined");
    }

    @JavadocIgnore
    interface Renderable {
        /** Return this object rendered using its template. */
        String render();
    }
}
