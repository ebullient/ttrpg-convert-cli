package dev.ebullient.convert.tools.pf2e.qute;

import java.util.List;
import java.util.StringJoiner;

import dev.ebullient.convert.qute.QuteUtil;
import dev.ebullient.convert.tools.pf2e.Pf2eTypeReader.Pf2eStat;
import io.quarkus.qute.TemplateData;

/**
 * Hit Points, Hardness, and a broken threshold for hazards and shields. Used for creatures, hazards, and shields.
 * <p>
 * <b>Hardness</b> 10, <b>HP (BT)</b> 30 (15) to destroy a channel gate (some ability)
 * </p>
 *
 * @param hp The HP as a {@link dev.ebullient.convert.tools.pf2e.qute.QuteDataHpHardnessBt.HpStat HpStat} (optional)
 * @param hardness Hardness as a {@link dev.ebullient.convert.tools.pf2e.Pf2eTypeReader.Pf2eStat Pf2eStat} (optional)
 * @param brokenThreshold Broken threshold as an integer (optional, not populated for creatures)
 */
@TemplateData
public record QuteDataHpHardnessBt(HpStat hp, Pf2eStat hardness, Integer brokenThreshold) implements QuteUtil {

    @Override
    public String toString() {
        return toStringWithName("");
    }

    /** Return a representation of these stats with the given name used to label each component. */
    public String toStringWithName(String name) {
        name = name.isEmpty() ? "" : name + " ";
        StringJoiner parts = new StringJoiner(", ");
        if (hardness != null) {
            parts.add(String.format("**%sHardness** %s", name, hardness));
        }
        if (hp != null && hp.value != null) {
            if (isPresent(brokenThreshold)) {
                parts.add(String.format(
                        name.isEmpty() ? "**%sHP (BT)** %d (%d) %s" : "**%sHP** %d (BT %d) %s",
                        name, hp.value, brokenThreshold, hp.formattedNotes()));
            } else {
                parts.add(String.format("**%sHP** %s", name, hp));
            }
        }
        return parts.toString();
    }

    /**
     * HP value and associated notes. Referencing this directly provides a default representation, e.g.
     * <p>
     * 15 to destroy a head (head regrowth)
     * </p>
     *
     * @param value The HP value itself
     * @param abilities Any abilities associated with the HP
     * @param notes Any notes associated with the HP.
     */
    @TemplateData
    public record HpStat(Integer value, List<String> notes, List<String> abilities) implements Pf2eStat {
        public HpStat(Integer value) {
            this(value, null);
        }

        public HpStat(Integer value, String note) {
            this(value, note == null || note.isBlank() ? List.of() : List.of(note), List.of());
        }

        @Override
        public String toString() {
            String formattedNotes = formattedNotes();
            return value + (formattedNotes.isEmpty() ? "" : " " + formattedNotes);
        }

        /** Returns any notes and abilities formatted as a string. */
        @Override
        public String formattedNotes() {
            StringJoiner formatted = new StringJoiner(" ");
            if (isPresent(notes)) {
                formatted.add(String.join(", ", notes));
            }
            if (isPresent(abilities)) {
                abilities.stream().map(s -> String.format("(%s)", s)).forEachOrdered(formatted::add);
            }
            return formatted.toString();
        }
    }
}
