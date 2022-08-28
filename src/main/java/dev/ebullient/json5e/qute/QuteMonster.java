package dev.ebullient.json5e.qute;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    protected final SavesAndSkills savesSkills;

    public final String senses;
    public final int passive;
    public final String vulnerable;
    public final String resist;
    public final String immune;
    public final String conditionImmune;
    public final String languages;
    public final String cr;
    public final String pb;

    public final List<Trait> trait;
    public final List<Trait> action;
    public final List<Trait> bonusAction;
    public final List<Trait> reaction;
    public final List<Trait> legendary;
    public final List<Spellcasting> spellcasting;

    public final String description;
    public final String environment;

    public final List<String> tags;

    public QuteMonster(String name, String source, String size, String type, String subtype, String alignment,
            Integer ac, String acText, Integer hp, String hpText, String hitDice, String speed, AbilityScores scores,
            SavesAndSkills savesSkills, String senses, int passive, String vulnerable,
            String resist, String immune, String conditionImmune, String languages, String cr, String pb, List<Trait> trait,
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
        this.savesSkills = savesSkills;
        this.senses = senses;
        this.passive = passive;
        this.vulnerable = vulnerable;
        this.resist = resist;
        this.immune = immune;
        this.conditionImmune = conditionImmune;
        this.languages = languages;
        this.cr = cr;
        this.pb = pb;
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
        if (savesSkills == null) {
            return null;
        }
        return savesSkills.saves;
    }

    public String getSkills() {
        if (savesSkills == null) {
            return null;
        }
        return savesSkills.skills;
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

        public String getDesc() {
            List<String> text = new ArrayList<>(headerEntries);

            appendList(text, "At will", will);
            if (daily != null && !daily.isEmpty()) {
                daily.forEach((k, v) -> {
                    appendList(text, innateKeyToTitle(k), v);
                });
            }
            if (spells != null && !spells.isEmpty()) {
                spells.forEach((k, v) -> {
                    appendList(text, spellToTitle(k, v), v.spells);
                });
            }

            text.addAll(footerEntries);
            return String.join("\n", text);
        }

        String spellToTitle(String key, Spells spells) {
            if ("0".equals(key)) {
                return "Cantrips (at will)";
            }
            if (spells.lowerBound > 0) {
                return String.format("%s-%s level%s",
                        levelToString(spells.lowerBound + ""),
                        levelToString(key),
                        spellSlots(key, spells));
            } else {
                return String.format("%s level%s",
                        levelToString(key),
                        spellSlots(key, spells));
            }
        }

        String spellSlots(String key, Spells spells) {
            if (spells.slots > 0) {
                return String.format(" (%s %s-level slots)",
                        spells.slots, levelToString(key));
            }
            return "";
        }

        String innateKeyToTitle(String key) {
            switch (key) {
                case "1":
                case "2":
                case "3":
                    return String.format("%s/day", key);
                case "1e":
                case "2e":
                case "3e":
                    return String.format("%s/day each", key);
            }
            return "Unknown";
        }

        void appendList(List<String> text, String title, List<String> spells) {
            if (spells == null || spells.isEmpty()) {
                return;
            }
            maybeAddBlankLine(text);
            text.add(String.format("**%s**: %s", title, String.join(", ", spells)));
        }

        void maybeAddBlankLine(List<String> text) {
            if (text.size() > 0 && !text.get(text.size() - 1).isBlank()) {
                text.add("");
            }
        }

        String levelToString(String level) {
            switch (level) {
                case "1":
                    return "1st";
                case "2":
                    return "2nd";
                case "3":
                    return "3rd";
                default:
                    return level + "th";
            }
        }
    }

    public static class Spells {
        public int slots;
        public int lowerBound;
        public List<String> spells;
    }

    public static class SavesAndSkills {
        public Map<String, Integer> saveMap;
        public Map<String, Integer> skillMap;
        public String saves;
        public String skills;
    }
}
