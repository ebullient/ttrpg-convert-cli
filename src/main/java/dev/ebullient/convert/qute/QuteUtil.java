package dev.ebullient.convert.qute;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;

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

    /** Remove leading '+' */
    default Map<String, Integer> mapOfNumbers(Map<String, String> map) {
        Map<String, Integer> result = new HashMap<>();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            int intVal = 0;
            try {
                intVal = Integer.parseInt(entry.getValue());
            } catch (NumberFormatException e) {
                // ignore
            }
            result.put(entry.getKey(), intVal);
        }
        return result;
    }

    default void addUnlessEmpty(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    default void addUnlessEmpty(Map<String, Object> map, String key, Collection<NamedText> value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
    }

    default void addUnlessEmpty(Map<String, Object> map, String key, List<?> value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
    }

    default String template() {
        throw new UnsupportedOperationException("Tried to call template() on a class which does not have a template defined");
    }

    default IndexType indexType() {
        return Pf2eIndexType.syntheticGroup;
    }

    interface Renderable {
        /** Return this object rendered using its template. */
        String render();
    }
}
