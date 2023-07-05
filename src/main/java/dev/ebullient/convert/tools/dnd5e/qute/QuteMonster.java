package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class QuteMonster extends Tools5eQuteBase {

    public final boolean isNpc;
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
    private final boolean useDiceRoller;

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
    public final Map<String, Trait> legendaryGroup;
    public final List<Spellcasting> spellcasting;
    public final List<String> books;

    public final String description;
    public final String environment;
    final ImageRef tokenImage;
    final List<ImageRef> fluffImages;
    final List<ImageRef> allImages;

    public QuteMonster(Tools5eSources sources, String name, String source, boolean isNpc, String size, String type,
            String subtype, String alignment,
            Integer ac, String acText, Integer hp, String hpText, String hitDice, String speed,
            AbilityScores scores, SavesAndSkills savesSkills, String senses, int passive, String vulnerable,
            String resist, String immune, String conditionImmune, String languages, String cr, String pb, List<Trait> trait,
            List<Trait> action, List<Trait> bonusAction, List<Trait> reaction, List<Trait> legendary,
            Map<String, Trait> legendaryGroup,
            List<Spellcasting> spellcasting, String description, String environment, List<String> books,
            ImageRef tokenImage, List<ImageRef> fluffImages, Collection<String> tags, boolean useDiceRoller) {

        super(sources, name, source, null, tags);

        this.isNpc = isNpc;
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
        this.legendaryGroup = legendaryGroup;
        this.spellcasting = spellcasting;
        this.description = description;
        this.environment = environment;
        this.books = books; // for YAML
        this.tokenImage = tokenImage;
        this.fluffImages = fluffImages;

        this.useDiceRoller = useDiceRoller;

        if (tokenImage != null || !fluffImages.isEmpty()) {
            allImages = new ArrayList<>();
            if (tokenImage != null) {
                allImages.add(tokenImage);
            }
            if (!fluffImages.isEmpty()) {
                allImages.addAll(fluffImages);
            }
        } else {
            allImages = List.of();
        }
    }

    @Override
    public List<ImageRef> images() {
        return allImages;
    }

    @Override
    public String targetPath() {
        return Tools5eQuteBase.monsterPath(isNpc, type);
    }

    public String getHp() {
        if (useDiceRoller && hitDice != null) {
            return "`dice: " + hitDice + "|text(" + hp + ")`";
        }
        return "" + hp;
    }

    public ImageRef getToken() {
        return tokenImage;
    }

    public List<ImageRef> getFluffImages() {
        return fluffImages;
    }

    public String getFullType() {
        return type + ((subtype == null || subtype.isEmpty()) ? "" : "(" + subtype + ")");
    }

    public AbilityScores getScores() {
        return scores;
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

    public Map<String, Integer> getSaveMap() {
        return savesSkills.saveMap;
    }

    public String getSkills() {
        if (savesSkills == null) {
            return null;
        }
        return savesSkills.skills;
    }

    public Map<String, Integer> getSkillMap() {
        return savesSkills.skillMap;
    }

    public String get5eInitiativeYaml() {
        Map<String, Object> map = new LinkedHashMap<>();
        addUnlessEmpty(map, "name", name);
        addIntegerUnlessEmpty(map, "ac", ac);
        addIntegerUnlessEmpty(map, "hp", hp);
        addUnlessEmpty(map, "hit_dice", hitDice);
        addUnlessEmpty(map, "cr", cr);
        addUnlessEmpty(map, "cr", cr);
        map.put("stats", scores.toArray()); // for initiative
        addUnlessEmpty(map, "source", books);
        return Tui.plainYaml().dump(map).trim();
    }

    public String get5eStatblockYaml() {
        Map<String, Object> map = new LinkedHashMap<>();
        addUnlessEmpty(map, "name", name);
        addUnlessEmpty(map, "size", size);
        addUnlessEmpty(map, "type", type);
        addUnlessEmpty(map, "subtype", subtype);
        addUnlessEmpty(map, "alignment", alignment);

        addIntegerUnlessEmpty(map, "ac", ac);
        addIntegerUnlessEmpty(map, "hp", hp);
        addUnlessEmpty(map, "hit_dice", hitDice);

        map.put("stats", scores.toArray());
        addUnlessEmpty(map, "speed", speed);
        if (savesSkills != null) {
            if (!savesSkills.saveMap.isEmpty()) {
                map.put("saves", savesSkills.saveMap);
            }
            if (!savesSkills.skillMap.isEmpty()) {
                map.put("skillsaves", savesSkills.skillMap);
            }
        }
        addUnlessEmpty(map, "damage_vulnerabilities", vulnerable);
        addUnlessEmpty(map, "damage_resistances", resist);
        addUnlessEmpty(map, "damage_immunities", immune);
        addUnlessEmpty(map, "condition_immunities", conditionImmune);
        map.put("senses", (senses.isBlank() ? "" : senses + ", ") + "passive Perception " + passive);
        map.put("languages", languages);
        addUnlessEmpty(map, "cr", cr);

        List<Trait> yamlTraits = new ArrayList<>(spellcastingToTraits());
        if (trait != null) {
            yamlTraits.addAll(trait);
        }
        addUnlessEmpty(map, "traits", yamlTraits);
        addUnlessEmpty(map, "actions", action);
        addUnlessEmpty(map, "bonus_actions", bonusAction);
        addUnlessEmpty(map, "reactions", reaction);
        addUnlessEmpty(map, "legendary_actions", legendary);
        addUnlessEmpty(map, "source", books);
        if (tokenImage != null) {
            map.put("image", tokenImage.vaultPath);
        }

        // De-markdown-ify
        return Tui.quotedYaml().dump(map).trim()
                .replaceAll("`", "")
                .replaceAll("\\*([^*]+)\\*", "$1") // em
                .replaceAll("\\*([^*]+)\\*", "$1") // bold
                .replaceAll("\\*([^*]+)\\*", "$1"); // bold em
    }

    void addIntegerUnlessEmpty(Map<String, Object> map, String key, Integer value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    void addUnlessEmpty(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    protected void addUnlessEmpty(Map<String, Object> map, String key, List<?> value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
    }

    List<Trait> spellcastingToTraits() {
        if (spellcasting == null) {
            return List.of();
        }
        return spellcasting.stream()
                .map(s -> new Trait(spellcastingToTraitName(s.name), s.getDesc()))
                .collect(Collectors.toList());
    }

    String spellcastingToTraitName(String name) {
        if (name.contains("Innate")) {
            return "innate";
        }
        return "spells";
    }

    @TemplateData
    @RegisterForReflection
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
                daily.forEach((k, v) -> appendList(text, innateKeyToTitle(k), v));
            }
            if (spells != null && !spells.isEmpty()) {
                spells.forEach((k, v) -> appendList(text, spellToTitle(k, v), v.spells));
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
                    return String.format("%s/day each", key.charAt(0));
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

    @TemplateData
    @RegisterForReflection
    public static class Spells {
        public int slots;
        public int lowerBound;
        public List<String> spells;
    }

    @TemplateData
    @RegisterForReflection
    public static class SavesAndSkills {
        public Map<String, Integer> saveMap;
        public Map<String, Integer> skillMap;
        public String saves;
        public String skills;
    }
}
