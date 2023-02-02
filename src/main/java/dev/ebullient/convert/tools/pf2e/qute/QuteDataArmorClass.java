package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.qute.TemplateData;

@TemplateData
public class QuteDataArmorClass {
    public Map<String, String> armorClass;
    public String note;
    public String abilities;

    public String toString() {
        return armorClass.entrySet().stream()
                .map(e -> "**" + e.getKey() + "** " + e.getValue())
                .collect(Collectors.joining("; "))
                + (note == null ? "" : " " + note.trim())
                + (abilities == null ? "" : "; " + abilities);
    }
}
