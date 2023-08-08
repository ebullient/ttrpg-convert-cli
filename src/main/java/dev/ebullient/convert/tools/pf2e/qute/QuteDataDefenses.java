package dev.ebullient.convert.tools.pf2e.qute;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.ebullient.convert.qute.QuteUtil;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools Armor class, Saving Throws, and other attributes describing defenses
 */
@TemplateData
public class QuteDataDefenses implements QuteUtil {

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
        List<String> first = new ArrayList<>();
        if (ac != null) {
            first.add(ac.toString());
        }
        if (savingThrows != null) {
            first.add(savingThrows.toString());
        }
        if (!first.isEmpty()) {
            lines.add("- " + String.join(", ", first));
        }
        if (hpHardness != null) {
            lines.add("- " + hpHardness.stream()
                    .map(hp -> hp.toString())
                    .collect(Collectors.joining("; ")));
        }
        if (isPresent(immunities)) {
            lines.add("- **Immunities** " + String.join("; ", immunities));
        }
        if (isPresent(resistances)) {
            lines.add("- **Resistances** " + String.join("; ", resistances));
        }
        if (isPresent(weaknesses)) {
            lines.add("- **Weaknesses** " + String.join("; ", weaknesses));
        }
        return String.join("\n", lines);
    }

    /**
     * Pf2eTools saving throw attributes.
     *
     * <p>
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * To use it, reference it directly: `{resource.defenses.savingThrows}`.
     * </p>
     */
    @TemplateData
    public static class QuteSavingThrows implements QuteUtil {
        /** Map of score (Wisdom, Charisma) to saving throw modifier as a string (+3) */
        public Map<String, String> savingThrows = new LinkedHashMap<>();
        /** Saving throw abilities as a string (Fortitude, Reflex, Will) */
        public String abilities;

        public boolean hasThrowAbilities;

        public String toString() {
            String join = hasThrowAbilities ? "; " : ", ";
            return savingThrows.entrySet().stream()
                    .map(e -> "**" + e.getKey() + "** " + e.getValue())
                    .collect(Collectors.joining(join))
                    + (isPresent(abilities) ? (join + abilities) : "");
        }
    }
}
