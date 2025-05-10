package dev.ebullient.convert.qute;

import static dev.ebullient.convert.StringUtil.pluralize;
import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.Collection;

import dev.ebullient.convert.StringUtil;
import dev.ebullient.convert.io.JavadocVerbatim;
import dev.ebullient.convert.io.Tui;
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
     *
     * Example: `{resource.name.pluralized(resource.components)}`
     */
    @JavadocVerbatim
    static String pluralizeLabel(Collection<?> collection, String s) {
        return pluralize(s, collection.size(), true);
    }

    /**
     * Return the given object as a string, with a space prepended if it's non-empty and non-null.
     *
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
     *
     * Example: `{resource.components.join(", ")}`
     */
    @JavadocVerbatim
    static String join(Collection<?> collection, String joiner) {
        return StringUtil.join(joiner, collection);
    }

    /**
     * Return the given list joined into a single string, using a different delimiter for the last element.
     *
     * Example: `{resource.components.joinConjunct(", ", " or ")}`
     */
    @JavadocVerbatim
    static String joinConjunct(Collection<?> collection, String joiner, String lastjoiner) {
        return StringUtil.joinConjunct(joiner, lastjoiner, collection.stream().map(o -> o.toString()).toList());
    }

    /**
     * Return the object as a JSON string
     *
     * Example: `{resource.components.getJsonString(resource)}`
     */
    @JavadocVerbatim
    static String jsonString(Object o) {
        return Tui.jsonStringify(o);
    }
}
