package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.ArrayList;
import java.util.List;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.qute.QuteUtil;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools armor class and hit points attributes
 * <p>
 * This data object provides a default mechanism for creating
 * a marked up string based on the attributes that are present.
 * To use it, reference it directly.
 * </p>
 */
@TemplateData
public class AcHp implements QuteUtil {
    /** Armor class (number) */
    public Integer ac;
    /** Additional armor class text. May link to related items */
    public String acText;
    /** Hit points */
    public Integer hp;
    /**
     * Additional hit point text.
     * In the case of summoned creatures, this will contain notes for how hit points
     * should be calculated relative to the player's modifiers.
     */
    public String hpText;
    /** Hit dice formula as formatted string: `7d10 + 14` (creatures) */
    public String hitDice;

    public AcHp() {
    }

    public AcHp(Integer ac, String acText, Integer hp, String hpText, String hitDice) {
        this.ac = ac;
        this.acText = acText;
        this.hp = hp;
        this.hpText = hpText;
        this.hitDice = hitDice;
    }

    public AcHp(AcHp other) {
        this.ac = other.ac;
        this.acText = other.acText;
        this.hp = other.hp;
        this.hpText = other.hpText;
        this.hitDice = other.hitDice;
    }

    /**
     * Hit points. If using the dice roller plugin is enabled,
     * this will be a dice roll formula.
     */
    public String getHp() {
        if (TtrpgConfig.getConfig().alwaysUseDiceRoller() && hitDice != null) {
            return "`dice: " + hitDice + "|text(" + hp + ")`";
        }
        return "" + hp;
    }

    public String toString() {
        List<String> out = new ArrayList<>();
        if (isPresent(ac)) {
            out.add("- **Armor Class** " + (isPresent(ac) ? ac + " " : "") + (isPresent(acText) ? " (" + acText + ")" : ""));
        }
        if (isPresent(hp)) {
            out.add("- **Hit Points** " + (isPresent(hp) ? hp + " " : "")
                    + (isPresent(hitDice) ? "(`" + hitDice + "`)" : "")
                    + (hpText != null ? " (" + hpText + ")" : ""));
        }
        return String.join("\n", out);
    }
}
