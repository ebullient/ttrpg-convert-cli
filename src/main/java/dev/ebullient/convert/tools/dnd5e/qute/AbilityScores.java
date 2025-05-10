package dev.ebullient.convert.tools.dnd5e.qute;

import static dev.ebullient.convert.StringUtil.asModifier;

import dev.ebullient.convert.qute.QuteUtil;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools Ability Score attributes.
 *
 * Used to describe a monster, object or vehicle's ability scores.
 *
 * If referenced as a unit (ignoring inner attributes), it will render ability scores as
 * a `|` separated list of values, in `STR,DEX,CON,INT,WIS,CHA` order.
 *
 * For example:
 * `10 (+0)|10 (+0)|10 (+0)|10 (+0)|10 (+0)|10 (+0)`.
 *
 * @param strength Strength score as {@link dev.ebullient.convert.tools.dnd5e.qute.AbilityScores.AbilityScore}
 * @param dexterity Dexterity score as {@link dev.ebullient.convert.tools.dnd5e.qute.AbilityScores.AbilityScore}
 * @param constitution Constitution score as {@link dev.ebullient.convert.tools.dnd5e.qute.AbilityScores.AbilityScore}
 * @param intelligence Intelligence score as {@link dev.ebullient.convert.tools.dnd5e.qute.AbilityScores.AbilityScore}
 * @param wisdom Wisdom score as {@link dev.ebullient.convert.tools.dnd5e.qute.AbilityScores.AbilityScore}
 * @param charisma Charisma score as {@link dev.ebullient.convert.tools.dnd5e.qute.AbilityScores.AbilityScore}
 */
@TemplateData
public record AbilityScores(
        AbilityScore strength,
        AbilityScore dexterity,
        AbilityScore constitution,
        AbilityScore intelligence,
        AbilityScore wisdom,
        AbilityScore charisma) implements QuteUtil {

    public static final AbilityScore TEN = new AbilityScore(10, null);
    public static final AbilityScores DEFAULT = new AbilityScores(TEN, TEN, TEN, TEN, TEN, TEN);

    public static int getModifier(AbilityScore score) {
        if (score.special != null) {
            return 0;
        }
        return scoreToModifier(score.score);
    }

    public static int scoreToModifier(int score) {
        int mod = score - 10;
        if (mod % 2 != 0) {
            mod -= 1; // round down
        }
        return mod / 2;
    }

    public int[] toArray() {
        return new int[] {
                strength.score(),
                dexterity.score(),
                constitution.score(),
                intelligence.score(),
                wisdom.score(),
                charisma.score()
        };
    }

    /** Strength as an ability string: `10 (+0)` */
    public String getStr() {
        return strength.toString();
    }

    /** Strength score as a number: 10 */
    public int getStrStat() {
        return strength.score();
    }

    /** Strength modifier: +1 or -2 */
    public String getStrMod() {
        return asModifier(getModifier(strength));
    }

    /** Dexterity as an ability string: `10 (+0)` */
    public String getDex() {
        return dexterity.toString();
    }

    /** Dexterity score as a number: 10 */
    public int getDexStat() {
        return dexterity.score();
    }

    /** Dexterity modifier: +1 or -2 */
    public String getDexMod() {
        return asModifier(getModifier(dexterity));
    }

    /** Constitution as an ability string: `10 (+0)` */
    public String getCon() {
        return constitution.toString();
    }

    /** Constitution score as a number: 10 */
    public int getConStat() {
        return constitution.score();
    }

    /** Constitution modifier: +1 or -2 */
    public String getConMod() {
        return asModifier(getModifier(constitution));
    }

    /** Intelligence as an ability string: `10 (+0)` */
    public String getInt() {
        return intelligence.toString();
    }

    /** Intelligence score as a number: 10 */
    public int getIntStat() {
        return intelligence.score();
    }

    /** Intelligence modifier: +1 or -2 */
    public String getIntMod() {
        return asModifier(getModifier(intelligence));
    }

    /** Wisdom as an ability string: `10 (+0)` */
    public String getWis() {
        return wisdom.toString();
    }

    /** Wisdom score as a number: 10 */
    public int getWisStat() {
        return wisdom.score();
    }

    /** Wisdom modifier: +1 or -2 */
    public String getWisMod() {
        return asModifier(getModifier(wisdom));
    }

    /** Charisma as an ability string: `10 (+0)` */
    public String getCha() {
        return charisma.toString();
    }

    /** Charisma stat as a number: 10 */
    public int getChaStat() {
        return charisma.score();
    }

    /** Charisma modifier: +1 or -2 */
    public String getChaMod() {
        return asModifier(getModifier(charisma));
    }

    public AbilityScore getScore(String name) {
        switch (name.toLowerCase()) {
            case "strength":
                return strength;
            case "dexterity":
                return dexterity;
            case "constitution":
                return constitution;
            case "intelligence":
                return intelligence;
            case "wisdom":
                return wisdom;
            case "charisma":
                return charisma;
            default:
                throw new IllegalArgumentException("Unknown ability score: " + name);
        }
    }

    @Override
    public String toString() {
        return strength.toString()
                + "|" + dexterity.toString()
                + "|" + constitution.toString()
                + "|" + intelligence.toString()
                + "|" + wisdom.toString()
                + "|" + charisma.toString();
    }

    /**
     * Ability score. Usually an integer, but can be a special value (string) instead.
     *
     * @param score The ability score (integer).
     * @param special The special value (string), or null if not applicable.
     */
    @TemplateData
    public record AbilityScore(int score, String special) {

        /** @return true if this score has a "special" value */
        public boolean isSpecial() {
            return special != null;
        }

        /** @return the modifier for this score as an integer */
        public int modifier() {
            return AbilityScores.getModifier(this);
        }

        @Override
        public String toString() {
            if (special != null) {
                return special;
            }
            return String.format("%2s (%s)", score, asModifier(modifier()));
        }
    }

    public static class Builder {
        AbilityScore strength;
        AbilityScore dexterity;
        AbilityScore constitution;
        AbilityScore intelligence;
        AbilityScore wisdom;
        AbilityScore charisma;

        public Builder() {
        }

        public Builder setStrength(int strength) {
            this.strength = new AbilityScore(strength, null);
            return this;
        }

        public Builder setDexterity(int dexterity) {
            this.dexterity = new AbilityScore(dexterity, null);
            return this;
        }

        public Builder setConstitution(int constitution) {
            this.constitution = new AbilityScore(constitution, null);
            return this;
        }

        public Builder setIntelligence(int intelligence) {
            this.intelligence = new AbilityScore(intelligence, null);
            return this;
        }

        public Builder setWisdom(int wisdom) {
            this.wisdom = new AbilityScore(wisdom, null);
            return this;
        }

        public Builder setCharisma(int charisma) {
            this.charisma = new AbilityScore(charisma, null);
            return this;
        }

        public AbilityScores build() {
            return new AbilityScores(strength, dexterity, constitution, intelligence, wisdom, charisma);
        }
    }
}
