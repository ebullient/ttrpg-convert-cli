package dev.ebullient.convert.tools;

import com.fasterxml.jackson.databind.JsonNode;

/** Performs copy operations on nodes as a pre-processing step before they're handled by the individual converters. */
public abstract class JsonSourceCopier<T extends IndexType> implements JsonTextConverter<T> {

    /** Handle any {@code _copy} fields which are present in the given node. */
    public abstract JsonNode handleCopy(T type, JsonNode copyTo);
}
