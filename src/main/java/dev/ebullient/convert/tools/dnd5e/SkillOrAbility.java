package dev.ebullient.convert.tools.dnd5e;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

public interface SkillOrAbility {
    static final Comparator<SkillOrAbility> comparator = Comparator.comparingInt(SkillOrAbility::ordinal)
            .thenComparing(Comparator.comparing(SkillOrAbility::value));
    static final CustomSkillOrAbility special = new CustomSkillOrAbility("Special");

    String value();

    int ordinal();

    public static SkillOrAbility fromTextValue(String v, Tools5eIndex index) {
        if (v == null || v.isBlank()) {
            return SkillOrAbilityEnum.None;
        }
        String lower = v.toLowerCase().replace(" saving throws", "").replace("_", "");
        if ("special".equals(lower)) {
            return special;
        }
        for (SkillOrAbilityEnum s : SkillOrAbilityEnum.values()) {
            if (s.lowerValue.equals(lower) || s.name().toLowerCase().equals(lower)) {
                return s;
            }
        }
        SkillOrAbility skill = index.isHomebrew() ? index.homebrew.getSkillType(lower) : null;
        if (skill != null) {
            return skill;
        }
        index.tui().warnf("Unknown skill or ability value " + v + " (compared using " + lower + ")");
        return null;
    }

    public static final List<String> allSkills = Stream.of(SkillOrAbilityEnum.values())
            .filter(x -> x.isSkill)
            .map(x -> x.longValue)
            .collect(Collectors.toList());

    public static final List<String> allSaves = Stream.of(SkillOrAbilityEnum.values())
            .filter(x -> !x.isSkill)
            .map(x -> x.longValue)
            .collect(Collectors.toList());

    public static String format(String key, Tools5eIndex index) {
        SkillOrAbility skill = fromTextValue(key, index);
        return skill == null ? key : fromTextValue(key, index).value();
    }

    public class CustomSkillOrAbility implements SkillOrAbility {
        final String name;
        final String lower;
        final Tools5eSources sources;

        public CustomSkillOrAbility(String name) {
            this.name = name;
            this.lower = name.toLowerCase();
            this.sources = null;
        }

        public CustomSkillOrAbility(JsonNode skill) {
            this.sources = Tools5eSources.constructSources(skill);
            this.name = this.sources.getName();
            this.lower = this.name.toLowerCase();
        }

        @Override
        public String value() {
            return name;
        }

        public int ordinal() {
            return 99;
        }
    }

    enum SkillOrAbilityEnum implements SkillOrAbility {
        STR("Strength", false),
        DEX("Dexterity", false),
        CON("Constitution", false),
        INT("Intelligence", false),
        WIS("Wisdom", false),
        CHA("Charisma", false),

        Acrobatics("Acrobatics", true),
        AnimalHandling("Animal Handling", true),
        Arcana("Arcana", true),
        Athletics("Athletics", true),
        Deception("Deception", true),
        History("History", true),
        Insight("Insight", true),
        Intimidation("Intimidation", true),
        Investigation("Investigation", true),
        Medicine("Medicine", true),
        Nature("Nature", true),
        Perception("Perception", true),
        Performance("Performance", true),
        Persuasion("Persuasion", true),
        Religion("Religion", true),
        SleightOfHand("Sleight of Hand", true),
        Stealth("Stealth", true),
        Survival("Survival", true),
        Any("Any", false),
        Varies("Varies", false),
        SidekickSpellcasting("Spellcasting", true),
        None("None", false);

        private final String longValue;
        private final String lowerValue;
        private final boolean isSkill;

        SkillOrAbilityEnum(String longValue, boolean isSkill) {
            this.longValue = longValue;
            this.lowerValue = longValue.toLowerCase();
            this.isSkill = isSkill;
        }

        public String value() {
            return longValue;
        }
    }
}
