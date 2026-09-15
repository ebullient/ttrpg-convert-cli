package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.ArrayList;
import java.util.List;

import dev.ebullient.convert.qute.QuteUtil;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools vulnerabilities, resistances, immunities, and condition immunities
 *
 * This data object provides a default mechanism for creating
 * a marked up string based on the attributes that are present.
 * The {@code conditionImmune} property contains the linkified string; use the
 * condition-immunity list accessor when plain, ordered values are needed.
 */
@TemplateData
public class ImmuneResist implements QuteUtil {
    /** Comma-separated string of creature damage vulnerabilities (if present). */
    public String vulnerable;
    /** Comma-separated string of creature damage resistances (if present). */
    public String resist;
    /** Comma-separated string of creature damage immunities (if present). */
    public String immune;
    /** Comma-separated string of creature condition immunities (if present). */
    public String conditionImmune;
    /** Ordered plain condition-immunity values, including non-standard values such as {@code special}. */
    public List<String> conditionImmuneList;

    public ImmuneResist() {
        this(null, null, null, null, List.of());
    }

    public ImmuneResist(String vulnerable, String resist, String immune, String conditionImmune) {
        this(vulnerable, resist, immune, conditionImmune, List.of());
    }

    public ImmuneResist(String vulnerable, String resist, String immune, String conditionImmune,
            List<String> conditionImmuneList) {
        this.vulnerable = vulnerable;
        this.resist = resist;
        this.immune = immune;
        this.conditionImmune = conditionImmune;
        this.conditionImmuneList = conditionImmuneList == null ? List.of() : conditionImmuneList;
    }

    /** True if immunities or resistances are present (otherwise false) */
    public boolean isPresent() {
        return isPresent(vulnerable)
                || isPresent(resist)
                || isPresent(immune)
                || isPresent(conditionImmune);
    }

    @Override
    public String toString() {
        List<String> parts = new ArrayList<>();
        if (isPresent(vulnerable)) {
            parts.add("- **Damage Vulnerabilities** " + vulnerable);
        }
        if (isPresent(resist)) {
            parts.add("- **Damage Resistances** " + resist);
        }
        if (isPresent(immune)) {
            parts.add("- **Damage Immunities** " + immune);
        }
        if (isPresent(conditionImmune)) {
            parts.add("- **Condition Immunities** " + conditionImmune);
        }
        return String.join("\n", parts);
    }
}
