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
    public String note;
    public String abilities;

    public String toString() {
        return armorClass.stream()
                .map(e -> e.toString())
                .collect(Collectors.joining("; "))
                + (isPresent(note) ? " " + note.trim() : "")
                + (isPresent(abilities) ? "; " + abilities : "");
    }
}
