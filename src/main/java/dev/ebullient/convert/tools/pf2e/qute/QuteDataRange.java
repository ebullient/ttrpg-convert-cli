package dev.ebullient.convert.tools.pf2e.qute;

import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.pluralize;

import java.util.List;

/**
 * A range with a given value and unit of measurement for that value.
 *
 * @param value An integer value for the range
 * @param unit What unit of measurement the {@code value} is given in, as a {@link QuteDataRange.RangeUnit}
 * @param notes Any associated notes, or an alternate rendering when the range can't be represented using just
 *        a unit and value.
 */
public record QuteDataRange(Integer value, RangeUnit unit, List<String> notes) implements QuteDataGenericStat {

    public QuteDataRange(Integer value, RangeUnit unit, String note) {
        this(value, unit, note == null || note.isBlank() ? List.of() : List.of(note));
    }

    @Override
    public String formattedNotes() {
        return join(", ", notes);
    }

    @Override
    public String toString() {
        if (unit == null || unit == RangeUnit.UNKNOWN) {
            return formattedNotes();
        }
        return switch (unit) {
            case TOUCH, PLANETARY, INTERPLANAR, UNLIMITED -> unit.toString();
            default -> join(" ", value, pluralize(unit.toString(), value), formattedNotes());
        };
    }

    public enum RangeUnit {
        TOUCH,
        FOOT,
        MILE,
        PLANETARY,
        INTERPLANAR,
        UNLIMITED,
        UNKNOWN;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }
}
