package dev.ebullient.convert.tools.pf2e.qute;

import static dev.ebullient.convert.StringUtil.flatJoin;
import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.formatMap;

import java.util.List;
import java.util.Map;

import io.quarkus.qute.TemplateData;

/**
 * A Pathfinder 2e skill and associated bonuses.
 *
 * <p>
 * Using this directly provides a default representation, e.g.
 * {@code Stealth +36 (+42 in forests) (some other note)}
 * </p>
 *
 * @param name The name of the skill
 * @param value The standard bonus associated with this skill
 * @param otherBonuses Any additional bonuses, as a map of descriptions to bonuses. Iterate over all map entries to
 *        display the values: {@code {#each resource.skills.otherBonuses}{it.key}: {it.value}{/each}}
 * @param notes Any notes associated with this skill bonus
 */
@TemplateData
public record QuteDataSkillBonus(
        String name, Integer value, Map<String, Integer> otherBonuses,
        List<String> notes) implements QuteDataGenericStat {

    public QuteDataSkillBonus(String name, Integer standardBonus) {
        this(name, standardBonus, Map.of(), List.of());
    }

    /** Return the standard bonus and any other conditional bonuses. */
    @Override
    public String bonus() {
        return flatJoin(" ",
                List.of(QuteDataGenericStat.super.bonus()),
                formatMap(otherBonuses, (k, v) -> "(%+d %s)".formatted(v, k)));
    }

    @Override
    public String toString() {
        return join(" ", name, bonus(), formattedNotes());
    }
}
