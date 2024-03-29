package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.ArrayList;
import java.util.List;

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

    /** Hit dice formula string: 7d10 + 14 (for creatures) */
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
     * Hit points as a dice roller formula:
     * \`dice: 1d20+7|text(37)\` (\`1d20+7\`)
     */
    public String getHpDiceRoller() {
        return hitDice == null
                ? getHp()
                : "`dice: " + hitDice + "|text(" + hp + ")` (`" + hitDice + "`)";
    }

    /**
     * Hit points (number or —)
     */
    public String getHp() {
        return hp == null ? hpText : hp.toString();
    }

    public String toString() {
        List<String> out = new ArrayList<>();
        if (isPresent(ac)) {
            List<String> acOut = new ArrayList<>();
            acOut.add("**Armor Class**");
            if (isPresent(ac)) {
                acOut.add(ac.toString());
            }
            if (isPresent(acText)) {
                acOut.add("(" + acText + ")");
            }
            out.add("- " + String.join(" ", acOut));
        }
        if (isPresent(hp)) {
            List<String> hpOut = new ArrayList<>();
            hpOut.add("**Hit Points**");
            if (isPresent(hp)) {
                hpOut.add(hp.toString());
                if (isPresent(hitDice)) {
                    hpOut.add("(`" + hitDice + "`)");
                }
                if (isPresent(hpText)) {
                    hpOut.add("(" + hpText + ")");
                }
            } else {
                hpOut.add(isPresent(hpText) ? hpText : "—");
            }

            out.add("- " + String.join(" ", hpOut));
        }
        return String.join("\n", out);
    }
}
