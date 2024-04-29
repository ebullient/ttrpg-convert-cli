package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.stream.Collectors;

import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.qute.QuteUtil;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools armor class attributes
 */
@TemplateData
public class QuteDataArmorClass implements QuteUtil {
    public Collection<NamedText> armorClass;
    public String abilities;

    /** Notes associated with the armor class, e.g. "with mage armor". */
    public Collection<String> notes;

    /**
     * Any notes associated with the armor class. This contains the same data as
     * {@link dev.ebullient.convert.tools.pf2e.qute.QuteDataArmorClass#notes notes}, but as a single
     * semicolon-delimited string.
     */
    public String note;

    public String toString() {
        return armorClass.stream()
                .map(NamedText::toString)
                .collect(Collectors.joining("; "))
                + (isPresent(note) ? " " + note.trim() : "")
                + (isPresent(abilities) ? "; " + abilities : "");
    }
}
