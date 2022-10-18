package dev.ebullient.json5e.qute;

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
        return String.format("%2s (%s%s)", value,
                modifier >= 0 ? "+" : "",
                modifier);
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

    public String getStr() {
        return toAbilityString(strength);
    }

    public int getStrStat() {
        return strength;
    }

    public int getStrMod() {
        return getModifier(strength);
    }

    public String getDex() {
        return toAbilityString(dexterity);
    }

    public int getDexStat() {
        return dexterity;
    }

    public int getDexMod() {
        return getModifier(dexterity);
    }

    public String getCon() {
        return toAbilityString(constitution);
    }

    public int getConStat() {
        return constitution;
    }

    public int getConMod() {
        return getModifier(constitution);
    }

    public String getInt() {
        return toAbilityString(intelligence);
    }

    public int getIntStat() {
        return intelligence;
    }

    public int getIntMod() {
        return getModifier(intelligence);
    }

    public String getWis() {
        return toAbilityString(wisdom);
    }

    public int getWisStat() {
        return wisdom;
    }

    public int getWisMod() {
        return getModifier(wisdom);
    }

    public String getCha() {
        return toAbilityString(charisma);
    }

    public int getChaStat() {
        return charisma;
    }

    public int getChaMod() {
        return getModifier(charisma);
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
