package dev.ebullient.convert.tools.pf2e.qute;

import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.numberAsWords;
import static dev.ebullient.convert.StringUtil.pluralize;

import java.util.List;

/**
 * A description of a frequency e.g. "once", which may include an interval that this is repeated for.
 *
 * Examples:
 *
 * - once per day
 * - once per hour
 * - 3 times per day
 * - {@code recurs=true}: once every day
 * - {@code overcharge=true}: once per day, plus overcharge
 * - {@code interval=2}: once per 2 days
 *
 * @param value The number represented by the frequency, integer
 * @param unit The unit the frequency is in, string. Required.
 * @param recurs Whether the unit recurs. In the default representation, this makes it render "every" instead of "per"
 * @param overcharge Whether there's an overcharge involved. Used for wands mostly. In the default representation, this
 *        adds ", plus overcharge".
 * @param interval The interval that the frequency is repeated for
 * @param notes Any notes associated with the frequency. May include a custom string, for frequencies which cannot be
 *        represented using the normal parts. If this is present, then the other parameters will be null.
 */
public record QuteDataFrequency(
        Integer value, Integer interval, String unit, boolean recurs, boolean overcharge,
        List<String> notes) implements QuteDataGenericStat {

    public QuteDataFrequency(String special) {
        this(null, null, null, false, false, List.of(special));
    }

    public QuteDataFrequency(
            Integer value, Integer interval, String unit, boolean recurs, boolean overcharge) {
        this(value, interval, unit, recurs, overcharge, List.of());
    }

    @Override
    public String formattedNotes() {
        return String.join(", ", notes);
    }

    @Override
    public String toString() {
        if (!notes.isEmpty()) {
            return formattedNotes();
        }
        return join(" ",
                switch (value) {
                    case 1 -> "once";
                    case 2 -> "twice";
                    default -> "%s times".formatted(numberAsWords(value));
                },
                recurs ? "every" : "per",
                interval,
                pluralize(unit, interval == null ? 1 : interval, true)) + (overcharge ? ", plus overcharge" : "");
    }
}
