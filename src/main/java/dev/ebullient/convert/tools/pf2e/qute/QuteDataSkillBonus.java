package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.ebullient.convert.qute.QuteUtil;
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
 * @param standardBonus The standard bonus associated with this skill
 * @param otherBonuses Any additional bonuses, as a map of descriptions to bonuses. Iterate over all map entries to
 *        display the values: {@code {#each resource.skills.otherBonuses}{it.key}: {it.value}{/each}}
 * @param note Any note associated with this skill bonus
 */
@TemplateData
public record QuteDataSkillBonus(
        String name,
        Integer standardBonus,
        Map<String, Integer> otherBonuses,
        String note) implements QuteUtil {

    public QuteDataSkillBonus(String name, Integer standardBonus) {
        this(name, standardBonus, null, null);
    }

    @Override
    public String toString() {
        return Stream.of(
                List.of(String.format("%s %+d", name, standardBonus)),
                otherBonuses.entrySet().stream().map(e -> String.format("(%+d %s)", e.getValue(), e.getKey())).toList(),
                note == null ? List.<String> of() : List.of("(" + note + ")"))
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }
}
