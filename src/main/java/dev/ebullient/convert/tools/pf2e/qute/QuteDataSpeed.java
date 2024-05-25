package dev.ebullient.convert.tools.pf2e.qute;

import static dev.ebullient.convert.StringUtil.flatJoin;
import static dev.ebullient.convert.StringUtil.formatMap;
import static dev.ebullient.convert.StringUtil.join;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *
 * @param value The land speed in feet
 * @param otherSpeeds Other speeds, as a map of (name, speed in feet)
 * @param notes Any speed-related notes
 * @param abilities Any speed-related abilities
 */
public record QuteDataSpeed(
        Integer value, Map<String, Integer> otherSpeeds, List<String> notes,
        List<String> abilities) implements QuteDataGenericStat {

    public void addAbility(String ability) {
        abilities.add(ability);
    }

    /** Return formatted notes and abilities. e.g. {@code (note) (another note); ability, another ability} */
    @Override
    public String formattedNotes() {
        return join("; ", QuteDataGenericStat.super.formattedNotes(), join(", ", abilities));
    }

    /** Return formatted speeds as a string, starting with land speed. e.g. {@code 10 feet, swim 20 feet} */
    public String formattedSpeeds() {
        return flatJoin(", ",
                List.of(Optional.ofNullable(value).map("%d feet"::formatted).orElse("no land speed")),
                formatMap(otherSpeeds, "%s %d feet"::formatted));
    }

    /**
     * Examples:
     * <blockquote>
     * 10 feet, swim 20 feet (some note); some ability
     * </blockquote>
     * <blockquote>
     * 10 feet, swim 20 feet, some ability
     * </blockquote>
     *
     */
    @Override
    public String toString() {
        return join(notes.isEmpty() ? ", " : " ", formattedSpeeds(), formattedNotes());
    }
}
