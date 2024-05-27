package dev.ebullient.convert.tools.pf2e.qute;

import static dev.ebullient.convert.StringUtil.flatJoin;
import static dev.ebullient.convert.StringUtil.formatMap;
import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.joiningNonEmpty;

import java.util.List;
import java.util.Map;

import dev.ebullient.convert.StringUtil;
import dev.ebullient.convert.qute.QuteUtil;
import io.quarkus.qute.TemplateData;

/** A generic container for a PF2e stat value which may have an attached note. */
public interface QuteDataGenericStat extends QuteUtil {
    /** Returns the value of the stat. */
    Integer value();

    /** Returns any notes associated with this value. */
    List<String> notes();

    /** Return the value formatted with a leading +/-. */
    default String bonus() {
        return value() == null ? "" : "%+d".formatted(value());
    }

    /** Return notes formatted as space-delimited parenthesized strings. */
    default String formattedNotes() {
        return notes().stream().map(StringUtil::parenthesize).collect(joiningNonEmpty(" "));
    }

    /**
     * A basic {@link dev.ebullient.convert.tools.pf2e.qute.QuteDataGenericStat QuteDataGenericStat} which provides
     * only a value and possibly a note. Default representation:
     *
     * <blockquote>
     * 10 (some note) (some other note)
     * </blockquote>
     */
    class SimpleStat implements QuteDataGenericStat {
        private final Integer value;
        private final List<String> notes;

        public SimpleStat(Integer value, List<String> notes) {
            this.value = value;
            this.notes = notes;
        }

        public SimpleStat(Integer value) {
            this(value, List.of());
        }

        public SimpleStat(Integer value, String note) {
            this(value, note == null || note.isBlank() ? List.of() : List.of(note));
        }

        @Override
        public String toString() {
            return join(" ", value, formattedNotes());
        }

        @Override
        public Integer value() {
            return value;
        }

        @Override
        public List<String> notes() {
            return notes;
        }
    }

    /**
     * A Pathfinder 2e named bonus, potentially with other conditional bonuses. Example default representation:
     * <blockquote>
     * Stealth +36 (+42 in forests) (ignores tremorsense)
     * </blockquote>
     *
     * @param name The name of the skill
     * @param value The standard bonus associated with this skill
     * @param otherBonuses Any additional bonuses, as a map of descriptions to bonuses. Iterate over all map entries to
     *        display the values, e.g.: {@code {#each resource.skills.otherBonuses}{it.key}: {it.value}{/each}}
     * @param notes Any notes associated with this skill bonus
     */
    @TemplateData
    record QuteDataNamedBonus(
            String name, Integer value, Map<String, Integer> otherBonuses,
            List<String> notes) implements QuteDataGenericStat {

        public QuteDataNamedBonus(String name, Integer standardBonus) {
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
}
