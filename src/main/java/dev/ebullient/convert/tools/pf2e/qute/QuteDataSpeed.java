package dev.ebullient.convert.tools.pf2e.qute;

import dev.ebullient.convert.tools.pf2e.Pf2eTypeReader;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @param value The land speed in feet
 * @param otherSpeeds Other speeds, as a map of (name, speed in feet)
 * @param notes Any speed-related notes
 * @param abilities Any speed-related abilities
 */
public record QuteDataSpeed(
        Integer value, Map<String, Integer> otherSpeeds, List<String> notes,
        List<String> abilities) implements Pf2eTypeReader.Pf2eStat {

    public void addAbility(String ability) {
        abilities.add(ability);
    }

    /** Return formatted notes and abilities. e.g. {@code (note) (another note); ability, another ability} */
    @Override
    public String formattedNotes() {
        return Stream.of(Pf2eTypeReader.Pf2eStat.super.formattedNotes(), String.join(", ", abilities))
                .filter(this::isPresent)
                .collect(Collectors.joining("; "));
    }

    /** Return formatted speeds as a string, starting with land speed. e.g. {@code 10 feet, swim 20 feet} */
    public String formattedSpeeds() {
        StringJoiner speeds = new StringJoiner(", ")
                .add(Optional.ofNullable(value).map(n -> String.format("%d feet", value)).orElse("no land speed"));
        otherSpeeds.entrySet().stream()
                .map(e -> String.format("%s %d feet", e.getKey(), e.getValue()))
                .forEach(speeds::add);
        return speeds.toString();
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
        return Stream.of(formattedSpeeds(), formattedNotes())
                .filter(this::isPresent).collect(Collectors.joining(notes.isEmpty() ? ", " : " "));
    }
}
