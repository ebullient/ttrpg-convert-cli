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

    private String toAbilityModifier(int value) {
        int mod = value - 10;
        if (mod % 2 != 0) {
            mod -= 1; // round down
        }
        int modifier = mod / 2;
        return String.format("%s (%s%s)", value,
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

    public int getStr() {
        return strength;
    }

    public int getDex() {
        return dexterity;
    }

    public int getCon() {
        return constitution;
    }

    public int getInt() {
        return intelligence;
    }

    public int getWis() {
        return wisdom;
    }

    public int getCha() {
        return charisma;
    }

    @Override
    public String toString() {
        return toAbilityModifier(strength)
                + "|" + toAbilityModifier(dexterity)
                + "|" + toAbilityModifier(constitution)
                + "|" + toAbilityModifier(intelligence)
                + "|" + toAbilityModifier(wisdom)
                + "|" + toAbilityModifier(charisma);
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
