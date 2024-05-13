package dev.ebullient.convert.tools.pf2e.qute;

import dev.ebullient.convert.StringUtil;
import dev.ebullient.convert.qute.QuteUtil;

import java.util.List;

import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.joiningNonEmpty;

/** A generic container for a PF2e stat value which may have an attached note. */
public interface QuteDataGenericStat extends QuteUtil {
    /** Returns the value of the stat. */
    Integer value();

    /** Returns any notes associated with this value. */
    List<String> notes();

    /** Return the value formatted with a leading +/-. */
    default String bonus() {
        return "%+d".formatted(value());
    }

    /** Return notes formatted as space-delimited parenthesized strings. */
    default String formattedNotes() {
        return notes().stream().map(StringUtil::parenthesize).collect(joiningNonEmpty(" "));
    }

    /**
     * A basic {@link QuteDataGenericStat} which provides only a value and possibly a note. Default representation:
     * <blockquote>
     * 10 (some note) (some other note)
     * </blockquote>
     */
    record SimpleStat(Integer value, List<String> notes) implements QuteDataGenericStat {
        public SimpleStat(Integer value) {
            this(value, List.of());
        }

        public SimpleStat(Integer value, String note) {
            this(value, note == null || note.isBlank() ? List.of() : List.of(note));
        }

        @Override
        public String toString() {
            return join(" ", value.toString(), formattedNotes());
        }
    }
}
