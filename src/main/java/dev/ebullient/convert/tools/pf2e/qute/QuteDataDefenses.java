package dev.ebullient.convert.tools.pf2e.qute;

import static dev.ebullient.convert.StringUtil.formatIfPresent;
import static dev.ebullient.convert.StringUtil.formatMap;
import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.joiningNonEmpty;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.ebullient.convert.qute.QuteUtil;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataGenericStat.QuteDataNamedBonus;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools Armor class, Saving Throws, and other attributes describing defenses of a creature or hazard.
 *
 * Example:
 *
 * ```md
 * **AC** 23 (33 with mage armor); **Fort** +15, **Ref** +12, **Will** +10
 * ```
 *
 * ```md
 * **Floor Hardness** 18, **Floor HP** 72 (BT 36);
 * **Channel Hardness** 12, **Channel HP** 48 (BT24 ) to destroy a channel gate;
 * **Immunities** critical hits;
 * **Resistances** precision damage;
 * **Weaknesses** bludgeoning damage
 * ```
 *
 * @param ac The armor class as a {@link QuteDataArmorClass}
 * @param savingThrows The saving throws, as {@link QuteDataDefenses.QuteSavingThrows}
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

    @SuppressWarnings("unused")  // for template use
    public String additionalHp() {
        return additionalHpHardnessBt.entrySet().stream()
            .filter(e -> e.getValue().hp() != null)
            .map(e -> "__%s HP__ %s".formatted(e.getKey(), e.getValue().hp()))
            .collect(Collectors.joining("; "));
    }

    @SuppressWarnings("unused")  // for template use
    public String additionalHardness() {
        return additionalHpHardnessBt.entrySet().stream()
            .filter(e -> e.getValue().hardness() != null)
            .map(e -> "__%s Hardness__ %s".formatted(e.getKey(), e.getValue().hardness()))
            .collect(Collectors.joining("; "));
    }

    @Override
    public String toString() {
        return join("\n",
                // - **AC** 21; **Fort** +15, **Ref** +12, **Will** +10
                formatIfPresent("- %s",
                    join("; ", formatIfPresent("**AC** %s", ac), savingThrows)),
                // - **Hardness** 18, **HP (BT)** 10; **Immunities** critical hits; **Resistances** fire 5
                formatIfPresent("- %s", join("; ",
                        hpHardnessBt,
                        join("; ", formatMap(additionalHpHardnessBt, (k, v) -> v.toStringWithName(k))),
                        formatIfPresent("**Immunities** %s", join(", ", immunities)),
                        formatMap(resistances, (k, v) -> join(" ", k, v)).stream().sorted()
                                .collect(joiningNonEmpty(", ", "**Resistances** ")),
                        formatMap(weaknesses, (k, v) -> join(" ", k, v)).stream().sorted()
                                .collect(joiningNonEmpty(", ", "**Weaknesses** ")))));
    }

    /**
     * Pathfinder 2e saving throws. Example default rendering:
     *
     * ```md
     * **Fort** +10 (+12 vs. poison), **Ref** +5 (+7 vs. traps), **Will** +4 (+6 vs. mental); +1 status to
     * all saves vs. magic
     * ```
     *
     * @param fort Fortitude saving throw bonus, as a {@link QuteDataGenericStat.QuteDataNamedBonus}
     * @param ref Reflex saving throw bonus, as a {@link QuteDataGenericStat.QuteDataNamedBonus}
     * @param will Will saving throw bonus, as a {@link QuteDataGenericStat.QuteDataNamedBonus}
     * @param abilities Any saving throw related abilities
     */
    @TemplateData
    public record QuteSavingThrows(
            QuteDataNamedBonus fort, QuteDataNamedBonus ref, QuteDataNamedBonus will,
            List<String> abilities) implements QuteUtil {
        /** Returns all abilities as a formatted, comma-separated string. */
        public String formattedAbilities() {
            return join(", ", abilities);
        }

        /** Returns all saving throws as a formatted string, not including any abilities. See class doc for example. */
        public String formattedBonuses() {
            return Stream.of(fort, ref, will)
                    .filter(Objects::nonNull)
                    .map(save -> "**%s** %s".formatted(save.name(), save.bonus()))
                    .collect(joiningNonEmpty(", "));
        }

        @Override
        public String toString() {
            return join("; ", formattedBonuses(), formattedAbilities());
        }
    }
}
