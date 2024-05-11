package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import dev.ebullient.convert.qute.QuteUtil;
import io.quarkus.qute.TemplateData;

import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.parenthesize;
import static dev.ebullient.convert.StringUtil.toTitleCase;

/**
 * Pf2eTools Attack attributes (inline/embedded, {@code inline-attack2md.txt})
 *
 * <p>
 * When used directly, renders according to {@code inline-attack2md.txt}
 * </p>
 *
 * @param name The name of the attack e.g. "fist" (string)
 * @param traits Any traits associated with the attack (collection of decorated links)
 * @param rangeType {@link dev.ebullient.convert.tools.pf2e.qute.QuteInlineAttack.AttackRangeType AttackType} enum
 * @param activity Number/type of action cost
 *        ({@link dev.ebullient.convert.tools.pf2e.qute.QuteDataActivity QuteDataActivity})
 * @param attackBonus The to-hit bonus for the attack (integer)
 * @param damage Damage if the attack hits (formatted string), e.g. "1d8 bludgeoning plus grab". This will include
 *        effects and damage types, unless there are multiline effects which may include the damage type.
 * @param effects Any additional effects associated with the attack e.g. grab (list of strings). Effects listed here
 *        will be repeated in {@code damage}.
 * @param multilineEffect A multi-line effect. Formatted string.
 * @param damageTypes The damage types caused by the attack. Will be included in either {@code damage} or in a
 *        multiline {@code effect}.
 * @param notes Any notes associated with the attack e.g. "no multiple attack penalty" (list of strings)
 */
@TemplateData
public record QuteInlineAttack(
        String name, QuteDataActivity activity, AttackRangeType rangeType, Integer attackBonus, String damage,
        Collection<String> damageTypes, Collection<String> traits, List<String> effects, String multilineEffect,
        List<String> notes, Function<QuteUtil, String> _renderer) implements QuteDataGenericStat, QuteUtil.Renderable {

    @Override
    public Integer value() {
        return attackBonus;
    }

    @Override
    public String formattedNotes() {
        return QuteDataGenericStat.super.formattedNotes();
    }

    @Override
    public String template() {
        return "inline-attack2md.txt";
    }

    @Override
    public String toString() {
        return render();
    }

    /** Return traits formatted as a single string, e.g. {@code (agile, trip, finesse)} */
    public String formattedTraits() {
        return parenthesize(join(", ", traits));
    }

    public enum AttackRangeType {
        RANGED,
        MELEE;

        @Override
        public String toString() {
            return toTitleCase(name());
        }
    }
}
