package dev.ebullient.convert.tools.pf2e.qute;

import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.pluralize;

import java.util.List;

import dev.ebullient.convert.io.JavadocVerbatim;

/**
 * A duration of time, represented by a numerical value and a unit. Sometimes this includes a custom display string,
 * for durations which cannot be represented using the normal structure.
 *
 * Examples:
 *
 * - A duration of 3 minutes: `3 minutes`
 * - A duration of 1 turn: `until the end of your next turn`
 * - An unlimited duration: `unlimited`
 *
 * @param value The quantity of time
 * @param unit The unit that the quantity is measured in, as a {@link QuteDataTimedDuration.DurationUnit}
 */
public record QuteDataTimedDuration(Integer value, DurationUnit unit,
        List<String> notes) implements QuteDataGenericStat, QuteDataDuration {

    public QuteDataTimedDuration(Integer value, DurationUnit unit, String note) {
        this(value, unit, note == null || note.isBlank() ? List.of() : List.of(note));
    }

    /** The custom display used for this duration. */
    public String getCustomDisplay() {
        return formattedNotes();
    }

    /** Returns true if we use a custom display string to show this instead of the value and unit. */
    public boolean hasCustomDisplay() {
        return !notes.isEmpty();
    }

    /** Returns a comma delimited string containing all notes. */
    @JavadocVerbatim
    @Override
    public String formattedNotes() {
        return join(", ", notes);
    }

    @Override
    public String toString() {
        if (hasCustomDisplay()) {
            return getCustomDisplay();
        }
        if (unit == DurationUnit.UNLIMITED) {
            return "unlimited";
        }
        if (value != null && value == 1 && unit == DurationUnit.TURN) {
            return "until the end of your next turn";
        }
        return join(" ", value, pluralize(unit == null ? null : unit.toString(), value));
    }

    /** Represents different units that a duration might be in. */
    public enum DurationUnit {
        REACTION,
        ACTION,
        TURN,
        ROUND,
        MINUTE,
        HOUR,
        DAY,
        MONTH,
        UNLIMITED,
        SPECIAL;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }
}
