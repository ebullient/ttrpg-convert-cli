package dev.ebullient.convert.qute;

import static dev.ebullient.convert.StringUtil.pluralize;
import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.Collection;

import dev.ebullient.convert.StringUtil;
import dev.ebullient.convert.io.JavadocVerbatim;
import dev.ebullient.convert.config.TtrpgConfig;
import io.quarkus.qute.TemplateExtension;

/**
 * Qute template extensions for TTRPG data.
 *
 * Use these functions to help render TTRPG data in Qute templates.
 */
@TemplateExtension
public class TtrpgTemplateExtension {

    /** Return the value formatted with a bonus with a +/- prefix. Example: `{perception.asBonus}` */
    @JavadocVerbatim
    static String asBonus(Integer value) {
        return String.format("%+d", value);
    }

    /** Return the string capitalized. Example: `{resource.name.capitalized}` */
    @JavadocVerbatim
    static String capitalized(String s) {
        return toTitleCase(s);
    }

    /**
     * Return the string pluralized based on the size of the collection.
     * Example: `{resource.name.pluralized(resource.components)}`
     */
    @JavadocVerbatim
    static String pluralizeLabel(Collection<?> collection, String s) {
        return pluralize(s, collection.size(), true);
    }

    /**
     * Return the given object as a string, with a space prepended if it's non-empty and non-null.
     * Example: `{resource.name.prefixSpace}`
     */
    @JavadocVerbatim
    static String prefixSpace(Object obj) {
        if (obj == null) {
            return "";
        }
        String s = obj.toString();
        return s.isEmpty() ? "" : (" " + s);
    }

    /**
     * Return the given collection converted into a string and joined using the specified joiner.
     * Example: `{resource.components.join(", ")}`
     */
    @JavadocVerbatim
    static String join(Collection<?> collection, String joiner) {
        return StringUtil.join(joiner, collection);
    }

    /**
     * Return the given list joined into a single string, using a different delimiter for the last element.
     * Example: `{resource.components.joinConjunct(", ", " or ")}`
     */
    @JavadocVerbatim
    static String joinConjunct(Collection<?> collection, String joiner, String lastjoiner) {
        return StringUtil.joinConjunct(joiner, lastjoiner, collection.stream().map(o -> o.toString()).toList());
    }

    /** Indent each line of the given string with the given indent. */
    static String indent(String lines, String indent) {
        return lines.replaceAll("\n", "\n" + indent);
    }

    /**
     * Double all newlines in the text (eg replace every newline with two newlines). For use with embedded YAML, where the
     * {@code >} folding operator will ignore newlines that aren't 'doubled'. This only replaces single newlines, not newlines
     * that are already doubled.
     */
    static String unfoldNewlines(String text) {
        return text.replaceAll("([^\n])\n([^\n])", "$1\n\n$2");
    }

    /**
     * Quote the input according to YAML property rules. Only quote if necessary for YAML to interpret it as a string. Escape
     * quotes in the input string if necessary.
     */
    static String quoted(Object obj) {
        if (obj == null) {
            return "";
        }
        String text = obj.toString();
        if (text == null || text.isBlank()) {
            return "";
        }
        if (text.contains("\n")) {
            TtrpgConfig.getConfig().tui().errorf("Asked to quote a multiline string: %s", text);
        }
        if (!text.startsWith("[") && !text.startsWith("*") && !text.contains(":") && !text.startsWith("\"")) {
            // No quoting required
            return text;
        }
        // Escape any quotes in the text before we quote it
        return "\"%s\"".formatted(text.replaceAll("\"", "\\\\\""));
    }
}
