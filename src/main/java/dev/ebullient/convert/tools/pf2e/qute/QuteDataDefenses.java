package dev.ebullient.convert.tools.pf2e.qute;

import static dev.ebullient.convert.StringUtil.flatJoin;
import static dev.ebullient.convert.StringUtil.formatMap;
import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.joinWithPrefix;
import static dev.ebullient.convert.StringUtil.joiningNonEmpty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.ebullient.convert.qute.QuteUtil;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools Armor class, Saving Throws, and other attributes describing defenses of a creature or hazard. Example:
 * <ul>
 * <li><b>AC</b> 23 (33 with mage armor); <b>Fort</b> +15, <b>Ref</b> +12, <b>Will</b> +10</li>
 * <li>
 * <b>Floor Hardness</b> 18, <b>Floor HP</b> 72 (BT 36);
 * <b>Channel Hardness</b> 12, <b>Channel HP</b> 48 (BT24 ) to destroy a channel gate;
 * <b>Immunities</b> critical hits;
 * <b>Resistances</b> precision damage;
 * <b>Weaknesses</b> bludgeoning damage
 * </li>
 * </ul>
 *
 * @param ac The armor class as a {@link QuteDataArmorClass}
 * @param savingThrows The saving throws, as {@link QuteSavingThrows}
 * @param hpHardnessBt HP, hardness, and broken threshold stored in a {@link QuteDataHpHardnessBt}
 * @param additionalHpHardnessBt Additional HP, hardness, or broken thresholds for other HP components as a map of
 *        names to {@link QuteDataHpHardnessBt}
 * @param immunities List of strings, optional
 * @param resistances Map of (name, {@link QuteDataGenericStat})
 * @param weaknesses Map of (name, {@link QuteDataGenericStat})
 */
@TemplateData
public record QuteDataDefenses(
        QuteDataArmorClass ac,
        QuteSavingThrows savingThrows,
        QuteDataHpHardnessBt hpHardnessBt,
        Map<String, QuteDataHpHardnessBt> additionalHpHardnessBt,
        List<String> immunities,
        Map<String, QuteDataGenericStat> resistances,
        Map<String, QuteDataGenericStat> weaknesses) implements QuteUtil {

    @Override
    public String toString() {
        return join("\n",
                // - **AC** 21; **Fort** +15, **Ref** +12, **Will** +10
                joinWithPrefix("; ", "- ", ac, savingThrows),
                // - **Hardness** 18, **HP (BT)** 10; **Immunities** critical hits; **Resistances** fire 5
                joinWithPrefix("; ", "- ",
                        hpHardnessBt,
                        join("; ", formatMap(additionalHpHardnessBt, (k, v) -> v.toStringWithName(k))),
                        joinWithPrefix(", ", "**Immunities** ", immunities),
                        formatMap(resistances, (k, v) -> join(" ", k, v)).stream().sorted()
                                .collect(joiningNonEmpty(", ", "**Resistances** ")),
                        formatMap(weaknesses, (k, v) -> join(" ", k, v)).stream().sorted()
                                .collect(joiningNonEmpty(", ", "**Weaknesses** "))));
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
            return flatJoin(
                    hasThrowAbilities ? "; " : ", ",
                    formatMap(savingThrows, "**%s** %s"::formatted),
                    // Passing null into List.of causes an exception at runtime
                    List.of(abilities == null ? "" : abilities));
        }
    }
}
