package dev.ebullient.json5e.qute;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuteMonster implements QuteSource {
    final String name;
    public final String source;

    public final String size;
    public final String type;
    public final String subtype;
    public final String alignment;

    public final Integer ac;
    public final String acText;
    public final Integer hp;
    public final String hpText;
    public final String hitDice;
    public final String speed;

    protected final AbilityScores scores;
    public final Map<String, Integer> saves;
    public final Map<String, Integer> skills;

    public final String senses;
    public final int passive;
    public final String vulnerable;
    public final String resist;
    public final String immune;
    public final String conditionImmune;
    public final String languages;
    public final String cr;

    public final List<Trait> trait;
    public final List<Trait> action;
    public final List<Trait> bonusAction;
    public final List<Trait> reaction;
    public final List<Trait> legendary;
    final List<Spellcasting> spellcasting;

    public final String description;
    public final String environment;

    public final List<String> tags;

    public QuteMonster(String name, String source, String size, String type, String subtype, String alignment,
            Integer ac, String acText, Integer hp, String hpText, String hitDice, String speed, AbilityScores scores,
            Map<String, Integer> saves, Map<String, Integer> skills, String senses, int passive, String vulnerable,
            String resist, String immune, String conditionImmune, String languages, String cr, List<Trait> trait,
            List<Trait> action, List<Trait> bonusAction, List<Trait> reaction, List<Trait> legendary,
            List<Spellcasting> spellcasting, String description, String environment, List<String> tags) {
        this.name = name;
        this.source = source;
        this.size = size;
        this.type = type;
        this.subtype = subtype;
        this.alignment = alignment;
        this.ac = ac;
        this.acText = acText;
        this.hp = hp;
        this.hpText = hpText;
        this.hitDice = hitDice;
        this.speed = speed;
        this.scores = scores;
        this.saves = saves;
        this.skills = skills;
        this.senses = senses;
        this.passive = passive;
        this.vulnerable = vulnerable;
        this.resist = resist;
        this.immune = immune;
        this.conditionImmune = conditionImmune;
        this.languages = languages;
        this.cr = cr;
        this.trait = trait;
        this.action = action;
        this.bonusAction = bonusAction;
        this.reaction = reaction;
        this.legendary = legendary;
        this.spellcasting = spellcasting;
        this.description = description;
        this.environment = environment;
        this.tags = tags;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSource() {
        return source;
    }

    public String getFullType() {
        return type + ((subtype == null || subtype.isEmpty()) ? "" : "(" + subtype + ")");
    }

    public String getScoreString() {
        return scores.toString();
    }

    public String getSavingThrows() {
        if (saves == null) {
            return null;
        }
        return saves.entrySet().stream()
                .map(e -> e.getKey() + " " + (e.getValue() > 0 ? "+" : "") + e.getValue())
                .collect(Collectors.joining(", "));
    }

    public String getSkills() {
        if (skills == null) {
            return null;
        }
        return skills.entrySet().stream()
                .map(e -> e.getKey() + " " + (e.getValue() > 0 ? "+" : "") + e.getValue())
                .collect(Collectors.joining(", "));
    }

    public String get5eStatblockYaml() {
        return null;
    }

    public static class Trait {
        public final String name;
        public final String desc;

        public Trait(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }
    }

    public static class Spellcasting {
        public String name;
        public List<String> headerEntries;
        public List<String> will;
        public Map<String, List<String>> daily;
        public Map<String, Spells> spells;
        public List<String> footerEntries;
        public String ability;
    }

    public static class Spells {
        public int slots;
        public List<String> spells;
    }
}
