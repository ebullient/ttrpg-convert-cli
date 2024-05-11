package dev.ebullient.convert.qute;

import io.quarkus.qute.TemplateExtension;

import static dev.ebullient.convert.StringUtil.toTitleCase;

@TemplateExtension
public class TtrpgTemplateExtension {
    /** Return the value formatted with a bonus with a +/- prefix */
    static String asBonus(Integer value) {
        return String.format("%+d", value);
    }

    /** Return the string capitalized */
    static String capitalized(String s) {
        return toTitleCase(s);
    }

    /** Return the given object as a string, with a space prepended if it's non-empty and non-null. */
    static String prefixSpace(Object obj) {
        if (obj == null) {
            return "";
        }
        String s = obj.toString();
        return s.isEmpty() ? "" : (" " + s);
    }
}
