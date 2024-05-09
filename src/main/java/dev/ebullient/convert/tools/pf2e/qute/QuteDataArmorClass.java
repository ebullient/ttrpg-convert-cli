package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.ebullient.convert.tools.pf2e.Pf2eTypeReader.Pf2eStat;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools armor class attributes
 *
 * @param value The AC value
 * @param alternateValues Alternate AC values as a map of (AC, condition)
 * @param notes Any notes associated with the AC e.g. "with mage armor"
 * @param abilities Any AC related abilities
 */
@TemplateData
public record QuteDataArmorClass(
        Integer value,
        Map<String, Integer> alternateValues,
        List<String> notes,
        List<String> abilities
    ) implements Pf2eStat {

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
        return alternateValues.entrySet().stream()
            .map(e -> String.format(asBonus ? "(%+d%s)" : "(%d%s)", e.getValue(), e.getKey().isEmpty() ? "" : " " + e.getKey()))
            .collect(Collectors.joining(" "));
    }

    @Override
    public String bonus() {
        String alternates = formattedAlternates(true);
        return Pf2eStat.super.bonus() + (alternates.isEmpty() ? "" : " " + alternates);
    }

    @Override
    public String toString() {
        return Stream.of(List.of("**AC**", value, formattedAlternates(false)), notes, abilities)
            .flatMap(Collection::stream)
            .map(Objects::toString)
            .collect(Collectors.joining(" "));
    }
}
