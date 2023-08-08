package dev.ebullient.convert.qute;

import java.util.Collection;
import java.util.Map;

public interface QuteUtil {
    default boolean isPresent(Collection<?> s) {
        return s != null && !s.isEmpty();
    }

    default boolean isPresent(Map<?, ?> s) {
        return s != null && !s.isEmpty();
    }

    default boolean isPresent(String s) {
        return s != null && !s.isBlank();
    }
}
