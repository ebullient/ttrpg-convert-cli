package dev.ebullient.convert.qute;

import java.util.List;

public interface QuteAltNames {

    /** Alternate names. (optional) */
    default List<String> getAltNames() {
        return List.of();
    }
}
