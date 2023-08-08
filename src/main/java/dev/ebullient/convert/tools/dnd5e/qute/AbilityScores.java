package dev.ebullient.convert.tools.dnd5e.qute;

import io.quarkus.qute.TemplateData;

/**
 * 5eTools Ability Score attributes.
 * <p>
 * Used by {@link dev.ebullient.convert.tools.dnd5e.qute.QuteMonster QuteMonster} to
 * describe a monster's statistics.
 * </p>
 * <p>
 * If referenced as a unit (ignoring inner attributes), it will render ability scores as
 * a `|` separated list of values, in `STR,DEX,CON,INT,WIS,CHA` order, for example:<br />
 * `10 (+0)|10 (+0)|10 (+0)|10 (+0)|10 (+0)|10 (+0)`.
 * </p>
 */
@TemplateData
public class AbilityScores {
    final int strength;
    final int dexterity;
    final int constitution;
    final int intelligence;
    final int wisdom;
    final int charisma;

    public AbilityScores(int strength, int dexterity, int constitution, int intelligence, int wisdom, int charisma) {
        this.strength = strength;
        this.dexterity = dexterity;
        this.constitution = constitution;
        this.intelligence = intelligence;
        this.wisdom = wisdom;
        this.charisma = charisma;
    }

    private int getModifier(int score) {
        int mod = score - 10;
        if (mod % 2 != 0) {
            mod -= 1; // round down
        }
        return mod / 2;
    }

    private String toAbilityString(int value) {
        int modifier = getModifier(value);
        return String.format("%2s (%s)", value, padded(modifier));
    }

    public int[] toArray() {
        return new int[] {
                strength,
                dexterity,
                constitution,
                intelligence,
                wisdom,
                charisma
        };
    }

    /** Strength as an ability string: `10 (+0)` */
    public String getStr() {
        return toAbilityString(strength);
    }

    /** Strength score as a number: 10 */
    public int getStrStat() {
        return strength;
    }

    /** Strength modifier: +1 or -2 */
    public String getStrMod() {
        return padded(getModifier(strength));
    }

    /** Dexterity as an ability string: `10 (+0)` */
    public String getDex() {
        return toAbilityString(dexterity);
    }

    /** Dexterity score as a number: 10 */
    public int getDexStat() {
        return dexterity;
    }

    /** Dexterity modifier: +1 or -2 */
    public String getDexMod() {
        return padded(getModifier(dexterity));
    }

    /** Constitution as an ability string: `10 (+0)` */
    public String getCon() {
        return toAbilityString(constitution);
    }

    /** Constitution score as a number: 10 */
    public int getConStat() {
        return constitution;
    }

    /** Constitution modifier: +1 or -2 */
    public String getConMod() {
        return padded(getModifier(constitution));
    }

    /** Intelligence as an ability string: `10 (+0)` */
    public String getInt() {
        return toAbilityString(intelligence);
    }

    /** Intelligence score as a number: 10 */
    public int getIntStat() {
        return intelligence;
    }

    /** Intelligence modifier: +1 or -2 */
    public String getIntMod() {
        return padded(getModifier(intelligence));
    }

    /** Wisdom as an ability string: `10 (+0)` */
    public String getWis() {
        return toAbilityString(wisdom);
    }

    /** Wisdom score as a number: 10 */
    public int getWisStat() {
        return wisdom;
    }

    /** Wisdom modifier: +1 or -2 */
    public String getWisMod() {
        return padded(getModifier(wisdom));
    }

    /** Charisma as an ability string: `10 (+0)` */
    public String getCha() {
        return toAbilityString(charisma);
    }

    /** Charisma stat as a number: 10 */
    public int getChaStat() {
        return charisma;
    }

    /** Charisma modifier: +1 or -2 */
    public String getChaMod() {
        return padded(getModifier(charisma));
    }

    private String padded(int value) {
        return String.format("%s%s",
                value >= 0 ? "+" : "",
                value);
    }

    @Override
    public String toString() {
        return toAbilityString(strength)
                + "|" + toAbilityString(dexterity)
                + "|" + toAbilityString(constitution)
                + "|" + toAbilityString(intelligence)
                + "|" + toAbilityString(wisdom)
                + "|" + toAbilityString(charisma);
    }

    public static class Builder {
        int strength;
        int dexterity;
        int constitution;
        int intelligence;
        int wisdom;
        int charisma;

        public Builder() {
        }

        public Builder setStrength(int strength) {
            this.strength = strength;
            return this;
        }

        public Builder setDexterity(int dexterity) {
            this.dexterity = dexterity;
            return this;
        }

        public Builder setConstitution(int constitution) {
            this.constitution = constitution;
            return this;
        }

        public Builder setIntelligence(int intelligence) {
            this.intelligence = intelligence;
            return this;
        }

        public Builder setWisdom(int wisdom) {
            this.wisdom = wisdom;
            return this;
        }

        public Builder setCharisma(int charisma) {
            this.charisma = charisma;
            return this;
        }

        public AbilityScores build() {
            return new AbilityScores(strength, dexterity, constitution, intelligence, wisdom, charisma);
        }
    }
}
