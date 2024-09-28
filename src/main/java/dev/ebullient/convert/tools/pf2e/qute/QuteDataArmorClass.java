package dev.ebullient.convert.tools.pf2e.qute;

import static dev.ebullient.convert.StringUtil.flatJoin;
import static dev.ebullient.convert.StringUtil.formatMap;
import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.parenthesize;

import java.util.List;
import java.util.Map;

import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools armor class attributes.
 *
 * Default representation example:
 *
 * ```md
 * **AC** 15 (10 with mage armor) note ability
 * ```
 *
 * @param value The AC value
 * @param alternateValues Alternate AC values as a map of (condition, AC value)
 * @param notes Any notes associated with the AC e.g. "with mage armor"
 * @param abilities Any AC related abilities
 */
@TemplateData
public record QuteDataArmorClass(
        Integer value, Map<String, Integer> alternateValues, List<String> notes,
        List<String> abilities) implements QuteDataGenericStat {

    public QuteDataArmorClass(Integer value) {
        this(value, Map.of(), List.of(), List.of());
    }

    public QuteDataArmorClass(Integer value, Integer alternateValue) {
        this(value, alternateValue == null ? Map.of() : Map.of("", alternateValue), List.of(), List.of());
    }

    /**
     * @param asBonus If true, then prefix alternate AC values with a +/-
     * @return Alternate values formatted as e.g. {@code (30 with mage armor)}
     */
    private String formattedAlternates(boolean asBonus) {
        String valFormat = asBonus ? "%+d" : "%d";
        return join(" ",
                formatMap(alternateValues, (k, v) -> parenthesize(join(" ", valFormat.formatted(v), k))));
    }

    @Override
    public String bonus() {
        return join(" ", QuteDataGenericStat.super.bonus(), formattedAlternates(true));
    }

    @Override
    public String toString() {
        return flatJoin(" ", List.of("**AC**", value, formattedAlternates(false)), notes, abilities);
    }
}
