package dev.ebullient.convert.qute;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class TtrpgTemplateExtension {
    /** Return the value formatted with a bonus with a +/- prefix */
    static String asBonus(Integer value) {
        return String.format("%+d", value);
    }

    /** Return the string capitalized */
    static String capitalized(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
