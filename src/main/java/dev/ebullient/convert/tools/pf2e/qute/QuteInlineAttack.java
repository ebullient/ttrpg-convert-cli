package dev.ebullient.convert.tools.pf2e.qute;

import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.parenthesize;
import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.qute.QuteUtil;
import dev.ebullient.convert.tools.JsonTextConverter;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools Attack attributes (inline/embedded, {@code inline-attack2md.txt})
 *
 * <p>
 * When used directly, renders according to {@code inline-attack2md.txt}
 * </p>
 */
@TemplateData
public final class QuteInlineAttack implements QuteDataGenericStat, QuteUtil.Renderable {

    /** The name of the attack e.g. "fist" (string) */
    public final String name;

    /** Number/type of action cost ({@link QuteDataActivity QuteDataActivity}) */
    public final QuteDataActivity activity;

    /**
     * The range of the attack ({@link QuteInlineAttack.AttackRangeType AttackRangeType} enum)
     */
    public final AttackRangeType rangeType;

    /** The to-hit bonus for the attack (integer) */
    public final Integer attackBonus;

    /**
     * Damage if the attack hits (formatted string), e.g. "1d8 bludgeoning plus grab". This will include
     * damage types and non-multiline effects.
     */
    public final String damage;

    /**
     * The damage types caused by the attack. Will be included in either
     * {@link QuteInlineAttack#damage damage} or in
     * {@link QuteInlineAttack#multilineEffect multilineEffect}.
     */
    public final Collection<String> damageTypes;

    /** Any traits associated with the attack (collection of decorated links) */
    public final Collection<String> traits;

    /**
     * Any additional effects associated with the attack e.g. grab (list of strings). Effects listed here
     * may be repeated in {@link QuteInlineAttack#damage damage}.
     */
    public final List<String> effects;

    /** A multi-line effect. Formatted string, will be null if there is no multiline effect. */
    public final String multilineEffect;

    /** Any notes associated with the attack e.g. "no multiple attack penalty" (list of strings) */
    public final List<String> notes;

    // Internal use only
    private final JsonTextConverter<?> _converter;

    public QuteInlineAttack(
            String name, QuteDataActivity activity, AttackRangeType rangeType, Integer attackBonus, String damage,
            Collection<String> damageTypes, Collection<String> traits, List<String> effects, String multilineEffect,
            List<String> notes, JsonTextConverter<?> converter) {
        this.name = name;
        this.activity = activity;
        this.rangeType = rangeType;
        this.attackBonus = attackBonus;
        this.damage = damage;
        this.damageTypes = damageTypes;
        this.traits = traits;
        this.effects = effects;
        this.multilineEffect = multilineEffect;
        this.notes = notes;
        this._converter = converter;
    }

    public QuteInlineAttack(
            String name, QuteDataActivity activity, AttackRangeType rangeType, String damage,
            Collection<String> damageTypes, Collection<String> traits, String note,
            JsonTextConverter<?> converter) {
        this(
                name, activity, rangeType, null,
                damage, damageTypes, traits,
                List.of(), null, note == null ? List.of() : List.of(note), converter);
    }

    @Override
    public List<String> notes() {
        return notes;
    }

    @Override
    public Integer value() {
        return attackBonus;
    }

    @Override
    public String template() {
        return "inline-attack2md.txt";
    }

    @Override
    public String render() {
        return _converter.renderInlineTemplate(this, null);
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
