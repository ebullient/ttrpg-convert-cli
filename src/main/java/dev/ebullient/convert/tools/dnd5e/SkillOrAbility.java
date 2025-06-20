package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.asModifier;
import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.joinConjunct;
import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.JsonTextConverter.SourceField;

public interface SkillOrAbility {
    static final Comparator<SkillOrAbility> comparator = Comparator.comparingInt(SkillOrAbility::ordinal)
            .thenComparing(Comparator.comparing(SkillOrAbility::value));
    static final CustomSkillOrAbility special = new CustomSkillOrAbility("Special");

    String value();

    String source();

    int ordinal();

    public static SkillOrAbility fromTextValue(String v) {
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

    public static String format(String key, Tools5eIndex index, Tools5eSources sources) {
        SkillOrAbility skill = index.findSkillOrAbility(key, sources);
        return skill == null ? key : skill.value();
    }

    public class CustomSkillOrAbility implements SkillOrAbility {
        final String name;
        final String lower;
        final String key;
        final String source;

        public CustomSkillOrAbility(String name) {
            this.name = name;
            this.lower = name.toLowerCase();
            this.key = null;
            this.source = "";
        }

        public CustomSkillOrAbility(JsonNode skill) {
            this.name = toTitleCase(SourceField.name.getTextOrEmpty(skill));
            this.lower = this.name.toLowerCase();
            this.key = Tools5eIndexType.skill.createKey(skill);
            this.source = SourceField.source.getTextOrEmpty(skill);
        }

        @Override
        public String value() {
            return name;
        }

        @Override
        public String source() {
            return source;
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

        public String source() {
            return Tools5eIndexType.skill.defaultSourceString();
        }
    }

    /** Ability scores related to race/species */
    public static String getAbilityScore(JsonNode abilityScore) {
        return processAbilityScoreArray(abilityScore, false);
    }

    /** Ability score increases for feats and backgrounds */
    public static String getAbilityScoreIncreases(JsonNode abilityScores) {
        return processAbilityScoreArray(abilityScores, true);
    }

    /** Single ability score increase (feat entries) */
    public static String getAbilityScoreIncrease(JsonNode abilityScores) {
        return abilityScore(abilityScores, true);
    }

    private static String processAbilityScoreArray(JsonNode abilityScores, boolean increase) {
        if (abilityScores == null || !abilityScores.isArray()) {
            return null;
        }

        List<String> list = new ArrayList<>();
        for (JsonNode abilityNode : abilityScores) {
            String result = abilityScore(abilityNode, increase);
            if (isPresent(result)) {
                list.add(result);
            }
        }
        return String.join("; ", list);
    }

    private static String abilityScore(JsonNode abilityNode, boolean increase) {
        var transform = Tools5eIndex.instance();
        String max = "" + AsiFields.max.intOrDefault(abilityNode, 20);

        JsonNode choose = AsiFields.choose.getFrom(abilityNode);
        String result = "";
        if (choose == null) {
            result = transform.streamOfFieldNames(abilityNode)
                    .filter(n -> !n.equalsIgnoreCase("max"))
                    .map(n -> toAbilityString(n, abilityNode.get(n), increase))
                    .collect(Collectors.joining(" "));
        } else if (AsiChoiceFields.weighted.existsIn(choose)) {
            result = AsiChoiceFields.weighted.readWeightedChoice(choose);
        } else if (AsiChoiceFields.from.existsIn(choose)) {
            result = AsiChoiceFields.from.readFromChoice(choose, increase);
        }
        return result.replace("{@MAX}", max);
    }

    private static String toAbilityString(String nameAbv, JsonNode value, boolean increase) {
        if (increase) {
            SkillOrAbility ability = SkillOrAbility.fromTextValue(nameAbv);
            return "Increase your %s score by %s, to a maximum of {@MAX}.".formatted(
                    ability == null ? toTitleCase(nameAbv) : ability.value(),
                    value.asText());
        }
        return "%s %s".formatted(nameAbv, toModifier(value));
    }

    private static String toModifier(JsonNode value) {
        try {
            int v = Integer.parseInt(value.toString());
            return asModifier(v);
        } catch (NumberFormatException e) {
            return value.toString();
        }
    }

    public enum AsiChoiceFields implements JsonNodeReader {
        from,

        amount,
        count,
        entry,

        weighted,
        weights,

        unknown // catcher for unknown attributes (see #fromString())
        ;

        // _mergeAbilityIncrease_getText
        private String readFromChoice(JsonNode choiceNode, boolean increase) {
            String entry = AsiChoiceFields.entry.replaceTextFrom(choiceNode, Tools5eIndex.instance());
            if (isPresent(entry)) {
                return entry;
            }
            JsonNode fromNode = getFrom(choiceNode);
            if (fromNode == null) {
                return null;
            }
            int count = AsiChoiceFields.count.intOrDefault(fromNode, 1);
            int amount = AsiChoiceFields.amount.intOrDefault(fromNode, 1);
            List<String> options = optionsFrom(fromNode);

            if (increase) {
                return options.size() == 6
                        ? "Increase one ability score of your choice by %s, to a maximum of {@MAX}."
                                .formatted(amount)
                        : "Increase your %s by %s, to a maximum of {@MAX}."
                                .formatted(joinConjunct(", ", " or ", options), amount);
            }

            return "Apply %s to %s of %s."
                    .formatted(asModifier(amount), count == 1 ? "one" : count + " (distinct)",
                            joinConjunct(", ", " or ", options));
        }

        // _mergeAbilityIncrease_getText
        private String readWeightedChoice(JsonNode choiceNode) {
            JsonNode weightedNode = getFrom(choiceNode);
            if (weightedNode == null) {
                return null;
            }
            JsonNode weights = AsiChoiceFields.weights.getFrom(weightedNode);
            if (weights == null) {
                return null;
            }

            List<String> adjustments = new ArrayList<>();
            for (int i = 0; i < weights.size(); i++) {
                int adj = weights.get(i).asInt();
                adjustments.add("%s ability score to %s by %s".formatted(
                        i == 0 ? "an" : "another",
                        adj > 0 ? "increase" : "decrease",
                        Math.abs(adj)));
            }
            String allAdjustments = joinConjunct(", ", " and ", adjustments);

            List<String> options = optionsFrom(AsiChoiceFields.from.getFrom(weightedNode));
            if (options.size() == 6) {
                return "Choose %s.".formatted(allAdjustments);
            }

            return "Choose %s from among %s.".formatted(
                    allAdjustments,
                    joinConjunct(", ", " and ", options));
        }

        private static List<String> optionsFrom(JsonNode fromNode) {
            if (fromNode == null) {
                return null;
            }
            List<String> options = new ArrayList<>();
            for (JsonNode option : fromNode) {
                options.add(AsiFields.asiFieldFromString(option.asText()).longName());
            }
            return options;
        }
    }

    public enum AsiFields implements JsonNodeReader {
        str(SkillOrAbilityEnum.STR),
        dex(SkillOrAbilityEnum.DEX),
        con(SkillOrAbilityEnum.CON),
        intel(SkillOrAbilityEnum.INT, "int"),
        wis(SkillOrAbilityEnum.WIS),
        cha(SkillOrAbilityEnum.CHA),
        choose,
        hidden, // ignored
        max,
        unknown // catcher for unknown attributes (see #fromString())
        ;

        private final SkillOrAbilityEnum ability;
        private final String altName;

        AsiFields() {
            this(null, null);
        }

        AsiFields(SkillOrAbilityEnum ability) {
            this(ability, null);
        }

        AsiFields(SkillOrAbilityEnum ability, String altName) {
            this.ability = ability;
            this.altName = altName;
        }

        public String nodeName() {
            return altName == null ? name() : altName;
        }

        public SkillOrAbilityEnum getAbility() {
            return ability;
        }

        public String longName() {
            return ability == null ? "Choose" : ability.value();
        }

        private static AsiFields asiFieldFromString(String name) {
            for (AsiFields field : AsiFields.values()) {
                if (field.name().equalsIgnoreCase(name) || field.nodeName().equalsIgnoreCase(name)) {
                    return field;
                }
            }

            return AsiFields.unknown;
        }
    }
}
