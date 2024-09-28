package dev.ebullient.convert.qute;

import io.quarkus.qute.TemplateData;

/**
 * A simple record to hold the name and source of a reprinted item.
 *
 * @param name Name of the reprinted item
 * @param source Primary source of the reprinted item
 */
@TemplateData
public record Reprinted(String name, String source) {
}
