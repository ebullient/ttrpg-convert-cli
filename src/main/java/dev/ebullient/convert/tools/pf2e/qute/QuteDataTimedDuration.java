package dev.ebullient.convert.tools.pf2e.qute;

import java.util.List;

import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.pluralize;

/**
 * A duration of time, represented by a numerical value and a unit. Sometimes this includes a custom display string,
 * for durations which cannot be represented using the normal structure. Examples:
 * <ul>
 * <li>A duration of 3 minutes: <blockquote>3 minutes</blockquote></li>
 * <li>A duration of 1 turn: <blockquote>until the end of your next turn</blockquote></li>
 * <li>An unlimited duration: <blockquote>unlimited</blockquote></li>
 * </ul>
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
