package dev.ebullient.convert.tools.pf2e.qute;

import dev.ebullient.convert.qute.QuteUtil;
import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;

/**
 * A union type which is either a {@link dev.ebullient.convert.tools.pf2e.qute.QuteAbility QuteAbility}
 * or a {@link dev.ebullient.convert.tools.pf2e.qute.QuteAffliction QuteAffliction}.
 *
 * <p>
 * Use {@link dev.ebullient.convert.tools.pf2e.qute.QuteAbilityOrAffliction#isAbility() isAbility()}
 * and {@link dev.ebullient.convert.tools.pf2e.qute.QuteAbilityOrAffliction#isAffliction() isAffliction()}
 * to tell whether it's an ability or an affliction.
 * </p>
 */
public sealed interface QuteAbilityOrAffliction extends QuteUtil permits QuteAbility, QuteAffliction {
    /** Returns true if this object is a {@link dev.ebullient.convert.tools.pf2e.qute.QuteAbility QuteAbility} */
    default boolean isAbility() {
        return indexType() == Pf2eIndexType.ability;
    }

    /** Returns true if this object is a {@link dev.ebullient.convert.tools.pf2e.qute.QuteAffliction QuteAffliction} */
    default boolean isAffliction() {
        return switch ((Pf2eIndexType) indexType()) {
            case affliction, disease, curse -> true;
            default -> false;
        };
    }
}
