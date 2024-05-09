package dev.ebullient.convert.tools.pf2e.qute;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        String first = Stream.of(ac, savingThrows)
                .filter(Objects::nonNull).map(Objects::toString).filter(s -> !s.isEmpty())
                .collect(Collectors.joining("; "));
        String second = Stream.of(
                hpHardness == null ? null
                        : hpHardness.stream()
                                .map(QuteDataHpHardness::toString)
                                .collect(Collectors.joining("; ")),
                isPresent(immunities) ? "**Immunities** " + String.join("; ", immunities) : null,
                isPresent(resistances) ? "**Resistances** " + String.join("; ", resistances) : null,
                isPresent(weaknesses) ? "**Weaknesses** " + String.join("; ", weaknesses) : null)
                .filter(Objects::nonNull).collect(Collectors.joining("; "));
        return Stream.of(first, second).map(s -> "- " + s).collect(Collectors.joining("\n"));
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
