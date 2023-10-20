package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndexType;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * 5eTools creature attributes ({@code monster2md.txt})
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.Tools5eQuteBase}.
 * </p>
 */
@TemplateData
public class QuteMonster extends Tools5eQuteBase {

    /** True if this is an NPC */
    public final boolean isNpc;
    /** Creature size (capitalized) */
    public final String size;
    /** Creature type (lowercase) */
    public final String type;
    /** Creature subtype (lowercase) */
    public final String subtype;
    /** Creature alignment */
    public final String alignment;

    /** Creature AC and HP as {@link dev.ebullient.convert.tools.dnd5e.qute.AcHp} */
    public AcHp acHp;
    /** Creature immunities and resistances as {@link dev.ebullient.convert.tools.dnd5e.qute.ImmuneResist} */
    public final ImmuneResist immuneResist;

    /** Creature speed as a comma-separated list */
    public final String speed;
    /** Creature ability scores as {@link dev.ebullient.convert.tools.dnd5e.qute.AbilityScores} */
    public final AbilityScores scores;
    /**
     * Creature saving throws and skill modifiers as {@link dev.ebullient.convert.tools.dnd5e.qute.SavesAndSkills}
     */
    public final SavesAndSkills savesSkills;
    /** Comma-separated string of creature senses (if present). */
    public final String senses;
    /** Passive perception as a numerical value */
    public final int passive;

    /** Comma-separated string of languages the creature understands. */
    public final String languages;
    /** Challenge rating */
    public final String cr;
    /** Proficiency bonus (modifier) */
    public final String pb;
    /** Creature traits as a list of {@link dev.ebullient.convert.qute.NamedText} */
    public final Collection<NamedText> trait;
    /** Creature actions as a list of {@link dev.ebullient.convert.qute.NamedText} */
    public final Collection<NamedText> action;
    /** Creature bonus actions as a list of {@link dev.ebullient.convert.qute.NamedText} */
    public final Collection<NamedText> bonusAction;
    /** Creature reactions as a list of {@link dev.ebullient.convert.qute.NamedText} */
    public final Collection<NamedText> reaction;
    /** Creature legendary traits as a list of {@link dev.ebullient.convert.qute.NamedText} */
    public final Collection<NamedText> legendary;
    /**
     * Map of grouped legendary traits (Lair Actions, Regional Effects, etc.). The key the group name, and the value is a list
     * of
     * {@link dev.ebullient.convert.qute.NamedText}.
     */
    public final Collection<NamedText> legendaryGroup;
    /**
     * Markdown link to legendary group (can be embedded).
     */
    public final String legendaryGroupLink;
    /** Creature abilities as a list of {@link dev.ebullient.convert.tools.dnd5e.qute.Spellcasting} attributes */
    public final List<Spellcasting> spellcasting;
    /** Formatted text containing the creature description. Same as `{resource.text}` */
    public final String description;
    /** Formatted text describing the creature's environment. Usually a single word. */
    public final String environment;
    /** Token image as {@link dev.ebullient.convert.qute.ImageRef} */
    public final ImageRef token;
    /** List of {@link dev.ebullient.convert.qute.ImageRef} related to the creature */
    public final List<ImageRef> fluffImages;

    public QuteMonster(Tools5eSources sources, String name, String source, boolean isNpc, String size, String type,
            String subtype, String alignment,
            AcHp acHp, String speed,
            AbilityScores scores, SavesAndSkills savesSkills, String senses, int passive,
            ImmuneResist immuneResist,
            String languages, String cr, String pb,
            Collection<NamedText> trait,
            Collection<NamedText> action, Collection<NamedText> bonusAction, Collection<NamedText> reaction,
            Collection<NamedText> legendary,
            Collection<NamedText> legendaryGroup, String legendaryGroupLink,
            List<Spellcasting> spellcasting, String description, String environment,
            ImageRef tokenImage, List<ImageRef> fluffImages, Tags tags, boolean useDiceRoller) {

        super(sources, name, source, description, tags);

        this.isNpc = isNpc;
        this.size = size;
        this.type = type;
        this.subtype = subtype;
        this.alignment = alignment;
        this.acHp = acHp;
        this.speed = speed;
        this.scores = scores;
        this.savesSkills = savesSkills;
        this.senses = senses;
        this.passive = passive;
        this.immuneResist = immuneResist;
        this.languages = languages;
        this.cr = cr;
        this.pb = pb;
        this.trait = trait;
        this.action = action;
        this.bonusAction = bonusAction;
        this.reaction = reaction;
        this.legendary = legendary;
        this.legendaryGroup = legendaryGroup;
        this.legendaryGroupLink = legendaryGroupLink;
        this.spellcasting = spellcasting;
        this.description = description;
        this.environment = environment;
        this.token = tokenImage;
        this.fluffImages = fluffImages;
    }

    @Override
    public String targetPath() {
        return Tools5eQuteBase.monsterPath(isNpc, type);
    }

    /** List of source books (abbreviated name). Fantasy statblock uses this list. */
    public final List<String> getBooks() {
        return getSourceAndPage().stream()
                .map(x -> x.source)
                .toList();
    }

    /** See {@link dev.ebullient.convert.tools.dnd5e.qute.AcHp#hp} */
    public String getHp() {
        return acHp.getHp();
    }

    /** See {@link dev.ebullient.convert.tools.dnd5e.qute.AcHp#ac} */
    public Integer getAc() {
        return acHp.ac;
    }

    /** See {@link dev.ebullient.convert.tools.dnd5e.qute.AcHp#acText} */
    public String getAcText() {
        return acHp.acText;
    }

    /** See {@link dev.ebullient.convert.tools.dnd5e.qute.AcHp#hpText} */
    public String getHpText() {
        return acHp.hpText;
    }

    /** See {@link dev.ebullient.convert.tools.dnd5e.qute.AcHp#hitDice} */
    public String getHitDice() {
        return acHp.hitDice;
    }

    /** See {@link dev.ebullient.convert.tools.dnd5e.qute.ImmuneResist#vulnerable} */
    public String getVulnerable() {
        return immuneResist.vulnerable;
    }

    /** See {@link dev.ebullient.convert.tools.dnd5e.qute.ImmuneResist#resist} */
    public String getResist() {
        return immuneResist.resist;
    }

    /** See {@link dev.ebullient.convert.tools.dnd5e.qute.ImmuneResist#immune} */
    public String getImmune() {
        return immuneResist.immune;
    }

    /** See {@link dev.ebullient.convert.tools.dnd5e.qute.ImmuneResist#conditionImmune} */
    public String getConditionImmune() {
        return immuneResist.conditionImmune;
    }

    /** Creature type (lowercase) and subtype if present: `{resource.type} ({resource.subtype})` */
    public String getFullType() {
        return type + ((subtype == null || subtype.isEmpty()) ? "" : " (" + subtype + ")");
    }

    @Deprecated
    public String getScoreString() {
        return scores.toString();
    }

    /**
     * String representation of saving throws.
     * Equivalent to `{resource.savesSkills.saves}`
     */
    public String getSavingThrows() {
        if (savesSkills == null) {
            return null;
        }
        return savesSkills.saves;
    }

    @Deprecated
    public Map<String, String> getSaveMap() {
        return savesSkills.saveMap;
    }

    /**
     * String representation of saving throws.
     * Equivalent to `{resource.savesSkills.skills}`
     */
    public String getSkills() {
        if (savesSkills == null) {
            return null;
        }
        return savesSkills.skills;
    }

    @Deprecated
    public Map<String, String> getSkillMap() {
        return savesSkills.skillMap;
    }

    /**
     * A minimal YAML snippet containing monster attributes required by the
     * Initiative Tracker plugin. Use this in frontmatter.
     */
    public String get5eInitiativeYaml() {
        Map<String, Object> map = new LinkedHashMap<>();
        addUnlessEmpty(map, "name", name + yamlMonsterName());
        addIntegerUnlessEmpty(map, "ac", acHp.ac);
        addIntegerUnlessEmpty(map, "hp", acHp.hp);
        addUnlessEmpty(map, "hit_dice", acHp.hitDice);
        addUnlessEmpty(map, "cr", cr);
        map.put("stats", scores.toArray()); // for initiative
        addUnlessEmpty(map, "source", getBooks());
        return Tui.plainYaml().dump(map).trim();
    }

    /**
     * Complete monster attributes in the format required by the Fantasy statblock plugin.
     * Uses double-quoted syntax to deal with a variety of characters occuring in
     * trait descriptions. Usable in frontmatter or Fantasy Statblock code blocks.
     */
    public String get5eStatblockYaml() {
        Map<String, Object> map = new LinkedHashMap<>();
        addUnlessEmpty(map, "name", name + yamlMonsterName());
        addUnlessEmpty(map, "size", size);
        addUnlessEmpty(map, "type", type);
        addUnlessEmpty(map, "subtype", subtype);
        addUnlessEmpty(map, "alignment", alignment);

        addIntegerUnlessEmpty(map, "ac", acHp.ac);
        addIntegerUnlessEmpty(map, "hp", acHp.hp);
        addUnlessEmpty(map, "hit_dice", acHp.hitDice);

        map.put("stats", scores.toArray());
        addUnlessEmpty(map, "speed", speed);
        if (savesSkills != null) {
            if (!savesSkills.saveMap.isEmpty()) {
                map.put("saves", mapOfNumbers(savesSkills.saveMap));
            }
            if (!savesSkills.skillMap.isEmpty()) {
                map.put("skillsaves", mapOfNumbers(savesSkills.skillMap));
            }
        }
        addUnlessEmpty(map, "damage_vulnerabilities", immuneResist.vulnerable);
        addUnlessEmpty(map, "damage_resistances", immuneResist.resist);
        addUnlessEmpty(map, "damage_immunities", immuneResist.immune);
        addUnlessEmpty(map, "condition_immunities", immuneResist.conditionImmune);
        map.put("senses", (senses.isBlank() ? "" : senses + ", ") + "passive Perception " + passive);
        map.put("languages", languages);
        addUnlessEmpty(map, "cr", cr);

        Collection<NamedText> yamlTraits = new ArrayList<>(spellcastingToTraits());
        if (trait != null) {
            yamlTraits.addAll(trait);
        }
        addUnlessEmpty(map, "traits", yamlTraits);
        addUnlessEmpty(map, "actions", action);
        addUnlessEmpty(map, "bonus_actions", bonusAction);
        addUnlessEmpty(map, "reactions", reaction);
        addUnlessEmpty(map, "legendary_actions", legendary);
        addUnlessEmpty(map, "source", getBooks());
        if (token != null) {
            map.put("image", token.getVaultPath());
        }

        // De-markdown-ify
        return Tui.quotedYaml().dump(map).trim()
                .replaceAll("`", "")
                .replaceAll("\\*([^*]+)\\*", "$1") // em
                .replaceAll("\\*([^*]+)\\*", "$1") // bold
                .replaceAll("\\*([^*]+)\\*", "$1"); // bold em
    }

    public String yamlMonsterName() {
        String source = getBooks().get(0);
        if (Tools5eIndexType.monster.defaultSourceString().equalsIgnoreCase(source)) {
            return "";
        } else {
            return " (" + source + ")";
        }
    }

    Collection<NamedText> spellcastingToTraits() {
        if (spellcasting == null) {
            return List.of();
        }
        return spellcasting.stream()
                .map(s -> new NamedText(spellcastingToTraitName(s.name), s.getDesc()))
                .collect(Collectors.toList());
    }

    String spellcastingToTraitName(String name) {
        if (name.contains("Innate")) {
            return "innate";
        }
        return "spells";
    }

    /**
     * 5eTools creature spellcasting attributes.
     * <p>
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * To use it, reference it directly:<br />
     * ```<br />
     * {#for spellcasting in resource.spellcasting}<br />
     * {spellcasting}<br />
     * {/for}<br />
     * ```<br />
     * or, using `{#each}` instead:<br />
     * ```<br />
     * {#each resource.spellcasting}<br />
     * {it}<br />
     * {/each}<br />
     * ```
     * </p>
     */
    @TemplateData
    @RegisterForReflection
    public static class Spellcasting {
        /** Name: "Spellcasting" or "Innate Spellcasting" */
        public String name;
        /** Formatted text that should be printed before the list of spells */
        public List<String> headerEntries;
        /** Spells (links) that can be cast at will */
        public List<String> will;
        /** Map: key = nuber of times per day, value: list of spells (links) */
        public Map<String, List<String>> daily;
        /**
         * Map: key = spell level, value: spell level information as
         * {@link dev.ebullient.convert.tools.dnd5e.qute.QuteSpell.Spells}
         */
        public Map<String, Spells> spells;
        /** Formatted text that should be printed after the list of spells */
        public List<String> footerEntries;
        public String ability;

        @Override
        public String toString() {
            return "***" + name + ".*** " + getDesc();
        }

        /** Formatted description: renders all attributes (other than name) */
        public String getDesc() {
            List<String> text = new ArrayList<>();
            if (!headerEntries.isEmpty()) {
                text.addAll(headerEntries);
                text.add("");
            }

            appendList(text, "At will", will);
            if (daily != null && !daily.isEmpty()) {
                daily.forEach((k, v) -> appendList(text, innateKeyToTitle(k), v));
            }
            if (spells != null && !spells.isEmpty()) {
                spells.forEach((k, v) -> appendList(text, spellToTitle(k, v), v.spells));
            }
            if (!footerEntries.isEmpty()) {
                text.add("");
                text.addAll(footerEntries);
            }
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

    /**
     * 5eTools creature spell attributes (associated with a spell level)
     */
    @TemplateData
    @RegisterForReflection
    public static class Spells {
        /** Available spell slots */
        public int slots;
        /** Set if this represents a spell range (the associated key is the upper bound) */
        public int lowerBound;
        /** List of spells (links) */
        public List<String> spells;
    }

    /**
     * 5eTools creature saving throws and skill attributes.
     */
    @TemplateData
    @RegisterForReflection
    public static class SavesAndSkills {
        /**
         * Creature saving throws as a map of key-value pairs.
         * Iterate over all map entries to display the values:<br />
         * `{#each resource.savesSkills.saveMap}**{it.key}** {it.value}{/each}`
         */
        public Map<String, String> saveMap;

        /**
         * Creature skills as a map of key-value pairs.
         * Iterate over all map entries to display the values:<br />
         * `{#each resource.savesSkills.skillMap}**{it.key}** {it.value}{/each}`
         */
        public Map<String, String> skillMap;

        /** Creature saving throws as a list: Constitution +6, Intelligence +8 */
        public String saves;

        /** Creature skills as a list: History +12, Perception +12 */
        public String skills;
    }
}
