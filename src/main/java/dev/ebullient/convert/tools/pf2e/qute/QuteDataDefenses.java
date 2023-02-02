package dev.ebullient.convert.tools.pf2e.qute;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.qute.TemplateData;

@TemplateData
public class QuteDataDefenses {

    public final QuteDataArmorClass ac;
    public final QuteSavingThrows savingThrows;
    public final List<QuteDataHpHardness> hpHardness;
    public final List<String> immunities;
    public final List<String> resistances;
    public final List<String> weaknesses;

    public QuteDataDefenses(List<String> text, QuteDataArmorClass ac, QuteSavingThrows savingThrows,
            List<QuteDataHpHardness> hpHardness, List<String> immunities,
            List<String> resistances, List<String> weaknesses) {
        this.ac = ac;
        this.savingThrows = savingThrows;
        this.hpHardness = hpHardness;
        this.immunities = immunities;
        this.resistances = resistances;
        this.weaknesses = weaknesses;
    }

    public String toString() {
        List<String> lines = new ArrayList<>();
        lines.add(String.format("- %s, %s", ac, savingThrows));
        if (hpHardness != null) {
            lines.add("- " + hpHardness.stream()
                    .map(hp -> hp.toString())
                    .collect(Collectors.joining("; ")));
        }
        if (immunities != null) {
            lines.add("- **Immunities** " + String.join("; ", immunities));
        }
        if (resistances != null) {
            lines.add("- **Resistances** " + String.join("; ", resistances));
        }
        if (weaknesses != null) {
            lines.add("- **Weaknesses** " + String.join("; ", weaknesses));
        }
        return String.join("\n", lines);
    }

    @TemplateData
    public static class QuteSavingThrows {
        public Map<String, String> savingThrows;
        public String abilities;

        public String toString() {
            return savingThrows.entrySet().stream()
                    .map(e -> "**" + e.getKey() + "** " + e.getValue())
                    .collect(Collectors.joining(", "))
                    + (abilities == null ? "" : ", " + abilities);
        }
    }
}
