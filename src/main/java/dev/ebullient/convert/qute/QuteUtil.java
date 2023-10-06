package dev.ebullient.convert.qute;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        map.forEach((k, v) -> result.put(k, Integer.parseInt(v)));
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

    default String makePlural(String s, int howMany) {
        return s + (howMany == 1 ? "" : "s");
    }

    default String makePlural(String s, String howMany) {
        return s + (howMany.equals("1") ? "" : "s");
    }
}
