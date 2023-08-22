package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.ArrayList;
import java.util.List;

import dev.ebullient.convert.qute.QuteUtil;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools vulnerabilities, resistances, immunities, and condition immunities
 * <p>
 * This data object provides a default mechanism for creating
 * a marked up string based on the attributes that are present.
 * To use it, reference it directly.
 * </p>
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

    public ImmuneResist() {
    }

    public ImmuneResist(String vulnerable, String resist, String immune, String conditionImmune) {
        this.vulnerable = vulnerable;
        this.resist = resist;
        this.immune = immune;
        this.conditionImmune = conditionImmune;
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
