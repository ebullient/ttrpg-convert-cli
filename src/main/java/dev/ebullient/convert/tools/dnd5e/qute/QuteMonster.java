package dev.ebullient.convert.tools.dnd5e.qute;

import static dev.ebullient.convert.StringUtil.asModifier;
import static dev.ebullient.convert.StringUtil.joinConjunct;
import static dev.ebullient.convert.StringUtil.pluralize;
import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.ebullient.convert.io.JavadocIgnore;
import dev.ebullient.convert.io.JavadocVerbatim;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.qute.QuteUtil;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndex;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndexType;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import dev.ebullient.convert.tools.dnd5e.qute.AbilityScores.AbilityScore;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * 5eTools creature attributes ({@code monster2md.txt})
 *
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase}.
 */
@TemplateData
public class QuteMonster extends Tools5eQuteBase {
    private static final List<String> abilities = List.of("strength", "dexterity", "constitution", "intelligence", "wisdom",
            "charisma");

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
    /** Creature gear as list of item links */
    public final List<String> gear;

    /** Creature speed as a comma-separated list */
    public final String speed;
    /** Creature ability scores as {@link dev.ebullient.convert.tools.dnd5e.qute.AbilityScores} */
    public final AbilityScores scores;
    /**
     * Creature saving throws and skill modifiers as {@link dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.SavesAndSkills}
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
    /** Initiative bonus as {@link dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.Initiative} */
    public final Initiative initiative;
    /** Creature traits as {@link dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.Traits} */
    public final Traits allTraits;
    /** Formatted text containing the creature description. Same as `{resource.text}` */
    public final String description;
    /** Formatted text describing the creature's environment. Usually a single word. */
    public final String environment;
    /** Token image as {@link dev.ebullient.convert.qute.ImageRef} */
    public final ImageRef token;

    private final List<Spellcasting> spellcasting;

    public QuteMonster(Tools5eSources sources, String name, String source, boolean isNpc, String size, String type,
            String subtype, String alignment,
            AcHp acHp, String speed,
            AbilityScores scores, SavesAndSkills savesSkills, String senses, int passive,
            ImmuneResist immuneResist, List<String> gear,
            String languages, String cr, String pb, Initiative initiative,
            Traits traits,
            List<Spellcasting> spellcasting,
            String description, String environment,
            ImageRef tokenImage, List<ImageRef> images, Tags tags) {

        super(sources, name, source, images, description, tags);

        this.isNpc = isNpc;
        this.size = size;
        this.type = type;
        this.subtype = subtype;
        this.alignment = alignment;
        this.acHp = acHp;
        this.speed = speed;
        this.scores = scores == null
                ? AbilityScores.DEFAULT
                : scores;
        this.savesSkills = savesSkills.withParent(this);
        this.senses = senses;
        this.passive = passive;
        this.immuneResist = immuneResist;
        this.gear = gear;
        this.languages = languages;
        this.cr = cr;
        this.pb = pb;
        this.allTraits = traits;
        this.initiative = initiative;
        this.spellcasting = spellcasting;
        this.description = description;
        this.environment = environment;
        this.token = tokenImage;

        if (isPresent(spellcasting)) {
            for (var sc : spellcasting) {
                NamedText nt = new NamedText(sc.name, sc.getDesc());
                if (nt.hasContent()) {
                    switch (sc.displayAs) {
                        case "trait" -> allTraits.traits().add(0, nt);
                        case "action" -> allTraits.actions().add(nt);
                        case "bonus" -> allTraits.bonusActions().add(nt);
                        case "reaction" -> allTraits.reactions().add(nt);
                        case "legendary" -> allTraits.legendaryActions().add(nt);
                        case "mythic" -> allTraits.mythicActions.add(nt);
                    }
                }
            }
        }
    }

    @Override
    public String targetPath() {
        return linkifier().monsterPath(isNpc, type);
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

    /**
     * Always returns null/empty to suppress previous default behavior that
     * rendered spellcasting as part of traits.
     *
     * 2024 rules interleave spellcasting with traits, actions, bonus actions, etc.
     *
     * @deprecated
     */
    public String getSpellcasting() {
        return null;
    }

    /**
     * Creature spellcasting abilities as a list of {@link dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.Spellcasting}
     * attributes
     */
    public List<Spellcasting> getRawSpellcasting() {
        return spellcasting;
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
        return savesSkills.getSaves();
    }

    /**
     * String representation of saving throws.
     * Equivalent to `{resource.savesSkills.skills}`
     */
    public String getSkills() {
        if (savesSkills == null) {
            return null;
        }
        return savesSkills.getSkills();
    }

    /** Creature traits as a list of {@link dev.ebullient.convert.qute.NamedText} */
    public List<NamedText> getTrait() {
        return traitsWithHeader(allTraits.traits);
    }

    /** Creature actions as a list of {@link dev.ebullient.convert.qute.NamedText} */
    public List<NamedText> getAction() {
        return traitsWithHeader(allTraits.actions);
    }

    /** Creature bonus actions as a list of {@link dev.ebullient.convert.qute.NamedText} */
    public List<NamedText> getBonusAction() {
        return traitsWithHeader(allTraits.bonusActions);
    }

    /** Creature reactions as a list of {@link dev.ebullient.convert.qute.NamedText} */
    public List<NamedText> getReaction() {
        return traitsWithHeader(allTraits.reactions);
    }

    /** Creature legendary traits as a list of {@link dev.ebullient.convert.qute.NamedText} */
    public List<NamedText> getLegendary() {
        return traitsWithHeader(allTraits.legendaryActions);
    }

    private List<NamedText> traitsWithHeader(TraitDescription traitDesc) {
        if (isPresent(traitDesc) && traitDesc.isPresent()) {
            List<NamedText> traits = new ArrayList<>();
            if (isPresent(traitDesc.description())) {
                traits.add(new NamedText("", traitDesc.description()));
            }
            traits.addAll(traitDesc.traits());
            return traits;
        }
        return List.of();
    }

    /**
     * Map of grouped legendary traits (Lair Actions, Regional Effects, etc.).
     * The key the group name, and the value is a list of {@link dev.ebullient.convert.qute.NamedText}.
     */
    public Collection<NamedText> getLegendaryGroup() {
        List<NamedText> legendaryGroupTraits = new ArrayList<>();
        if (isPresent(allTraits.lairActions) && allTraits.lairActions.isPresent()) {
            legendaryGroupTraits.add(allTraits.lairActions.asNamedText());
        }
        if (isPresent(allTraits.regionalEffects) && allTraits.regionalEffects.isPresent()) {
            legendaryGroupTraits.add(allTraits.regionalEffects.asNamedText());
        }
        if (isPresent(allTraits.mythicActions) && allTraits.mythicActions.isPresent()) {
            legendaryGroupTraits.add(allTraits.mythicActions.asNamedText());
        }
        return legendaryGroupTraits;
    }

    /**
     * Markdown link to legendary group (can be embedded).
     */
    public String getLegendaryGroupLink() {
        return allTraits.legendaryGroupLink();
    }

    /**
     * A minimal YAML snippet containing monster attributes required by the
     * Initiative Tracker plugin. Use this in frontmatter.
     *
     * The source book will not be included in the monster name.
     */
    public String get5eInitiativeYamlNoSource() {
        return get5eInitiativeYaml(false);
    }

    /**
     * A minimal YAML snippet containing monster attributes required by the
     * Initiative Tracker plugin. Use this in frontmatter.
     *
     * The source book will be included in the name if it isn't the default monster source ("MM").
     */
    public String get5eInitiativeYaml() {
        return get5eInitiativeYaml(true);
    }

    private String get5eInitiativeYaml(boolean withSource) {
        Map<String, Object> map = new LinkedHashMap<>();
        addUnlessEmpty(map, "name", name + yamlMonsterName(withSource));
        addIntegerUnlessEmpty(map, "ac", acHp.ac);
        addIntegerUnlessEmpty(map, "hp", acHp.hp);
        if (initiative != null) {
            map.put("modifier", initiative.bonus);
            // TODO: passive initiative
        }
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
     *
     * The source book will not be included in the monster name.
     */
    public String get5eStatblockYamlNoSource() {
        return render5eStatblockYaml(false);
    }

    /**
     * Complete monster attributes in the format required by the Fantasy statblock plugin.
     * Uses double-quoted syntax to deal with a variety of characters occuring in
     * trait descriptions. Usable in frontmatter or Fantasy Statblock code blocks.
     *
     * The source book will be included in the name if it isn't the default monster source ("MM").
     */
    public String get5eStatblockYaml() {
        return render5eStatblockYaml(true);
    }

    private String render5eStatblockYaml(boolean withSource) {
        // Map our types to the fields and values that Fantasy Statblock expects

        Map<String, Object> map = new LinkedHashMap<>();
        addUnlessEmpty(map, "name", name + yamlMonsterName(withSource));
        addUnlessEmpty(map, "size", size);
        addUnlessEmpty(map, "type", type);
        addUnlessEmpty(map, "subtype", subtype);
        addUnlessEmpty(map, "alignment", alignment);

        addIntegerUnlessEmpty(map, "ac", acHp.ac);
        addUnlessEmpty(map, "ac_class", acHp.acText);
        addIntegerUnlessEmpty(map, "hp", acHp.hp);
        addUnlessEmpty(map, "hit_dice", acHp.hitDice);

        if (initiative != null) {
            map.put("modifier", initiative.bonus);
            // TODO: passive initiative?
        }

        map.put("stats", scores.toArray());
        addUnlessEmpty(map, "speed", speed);

        if (savesSkills != null) {
            if (isPresent(savesSkills.saves)) {
                map.put("saves", savesSkills.getSaveValues());
            }
            if (isPresent(savesSkills.skills) || isPresent(savesSkills.skillChoices)) {
                map.put("skillsaves", savesSkills.getSkillValues());
            }
        }
        addUnlessEmpty(map, "damage_vulnerabilities", immuneResist.vulnerable);
        addUnlessEmpty(map, "damage_resistances", immuneResist.resist);
        addUnlessEmpty(map, "damage_immunities", immuneResist.immune);
        addUnlessEmpty(map, "condition_immunities", immuneResist.conditionImmune);
        addUnlessEmpty(map, "gear", gear);
        map.put("senses", (senses.isBlank() ? "" : senses + ", ") + "passive Perception " + passive);
        map.put("languages", languages);
        addUnlessEmpty(map, "cr", cr);

        addUnlessEmpty(map, "traits", traitsFrom(allTraits.traits()));
        addUnlessEmpty(map, "actions", traitsFrom(allTraits.actions()));
        addUnlessEmpty(map, "bonus_actions", traitsFrom(allTraits.bonusActions()));
        addUnlessEmpty(map, "reactions", traitsFrom(allTraits.reactions()));
        addUnlessEmpty(map, "lair_actions", traitsFrom(allTraits.lairActions()));
        addUnlessEmpty(map, "regional_effects", traitsFrom(allTraits.regionalEffects()));

        TraitDescription legendary = allTraits.legendaryActions();
        if (isPresent(legendary)) {
            addUnlessEmpty(map, "legendary_description", legendary.description());
            addUnlessEmpty(map, "legendary_actions", legendary.traits());
        }

        TraitDescription mythic = allTraits.mythicActions();
        if (isPresent(mythic)) {
            addUnlessEmpty(map, "mythic_description", mythic.description());
            addUnlessEmpty(map, "mythic_actions", mythic.traits());
        }

        addUnlessEmpty(map, "source", getBooks());
        if (token != null) {
            map.put("image", token.getVaultPath());
        }

        // De-markdown-ify
        return Tui.quotedYaml().dump(map).trim()
                .replaceAll("`", "");
    }

    private List<NamedText> traitsFrom(TraitDescription traitDesc) {
        if (isPresent(traitDesc) && isPresent(traitDesc.traits())) {
            return traitDesc.traits();
        }
        return null;
    }

    private String yamlMonsterName(boolean withSource) {
        if (withSource) {
            String source = getBooks().get(0);
            String outputSource = Tools5eIndexType.monster.defaultOutputSource();
            if (!outputSource.equalsIgnoreCase(source)) {
                return " (" + source + ")";
            }
        }
        return "";
    }

    /**
     * 5eTools creature traits.
     *
     * @param traits Creature traits as a list of {@link dev.ebullient.convert.qute.NamedText}
     * @param actions Creature actions as a list of {@link dev.ebullient.convert.qute.NamedText}
     * @param bonusActions Creature bonus actions as a list of {@link dev.ebullient.convert.qute.NamedText}
     * @param reactions Creature reactions as a list of {@link dev.ebullient.convert.qute.NamedText}
     * @param legendaryActions Creature legendary traits as a list of {@link dev.ebullient.convert.qute.NamedText}
     * @param lairActions Creature lair actions as a list of {@link dev.ebullient.convert.qute.NamedText}
     * @param regionalEffects Creature regional effects as a list of {@link dev.ebullient.convert.qute.NamedText}
     * @param mythicActions Creature mythic traits as a list of {@link dev.ebullient.convert.qute.NamedText}
     * @param legendaryGroupLink Link to the legendary group, if present
     * @param legendaryActionCount Number of legendary actions
     * @param legendaryActionsLairCount Number of legendary lair actions
     */
    @TemplateData
    @RegisterForReflection
    public record Traits(
            TraitDescription traits,
            TraitDescription actions,
            TraitDescription bonusActions,
            TraitDescription reactions,
            TraitDescription legendaryActions,
            TraitDescription lairActions,
            TraitDescription regionalEffects,
            TraitDescription mythicActions,
            String legendaryGroupLink) implements QuteUtil {
    }

    /**
     * 5eTools creature trait description.
     *
     * @param title Title of the trait description
     * @param description Formatted text describing the collection of traits
     * @param traits Traits as a list of {@link dev.ebullient.convert.qute.NamedText}
     */
    @TemplateData
    @RegisterForReflection
    public record TraitDescription(
            String title,
            String description,
            List<NamedText> traits) implements QuteUtil {

        public void add(int i, NamedText nt) {
            traits.add(0, nt);
        }

        public boolean isPresent() {
            return isPresent(description) || isPresent(traits);
        }

        public void add(NamedText nt) {
            traits.add(nt);
        }

        public NamedText asNamedText() {
            List<String> text = new ArrayList<>();
            if (isPresent(description)) {
                text.add(description);
                if (isPresent(traits)) {
                    text.add("");
                }
            }
            for (var nt : traits) {
                text.add(nt.toString());
            }
            return new NamedText(title, text, traits);
        }
    }

    /**
     * 5eTools creature spellcasting attributes.
     *
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     *
     * To use it, reference it directly:
     *
     * ```md
     * {#for spellcasting in resource.spellcasting}
     * {spellcasting}
     * {/for}
     * ```
     *
     * or, using `{#each}` instead:
     *
     * ```md
     * {#each resource.spellcasting}
     * {it}
     * {/each}
     * ```
     */
    @TemplateData
    @RegisterForReflection
    public static class Spellcasting implements QuteUtil {
        /** Name: "Spellcasting" or "Innate Spellcasting" */
        public String name;
        /** Formatted text that should be printed before the list of spells */
        public List<String> headerEntries;

        /** Spells (links) that can be cast a fixed number of times (constant), at will (will), or as a ritual */
        public Map<String, List<String>> fixed;

        /**
         * Map of frequency to spells (links).
         *
         * Frequencies (key)
         * - charges
         * - daily
         * - legendary
         * - monthly
         * - recharge
         * - rest
         * - restLong
         * - weekly
         * - yearly
         *
         * Value is another map containing additional key/value pairs, where the key is a number,
         * and the value is a list of spells (links).
         *
         * If the key ends with `e` (like `1e` or `2e`), each will be appended, e.g. "1/day each"
         * to specify that each spell can be cast once per day.
         */
        @JavadocVerbatim
        public Map<String, Map<String, List<String>>> variable;

        /**
         * Map: key = spell level, value: spell level information as
         * {@link dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.Spells}
         */
        public Map<String, Spells> spells;
        /** Formatted text that should be printed after the list of spells */
        public List<String> footerEntries;
        public String ability;

        /**
         * Groups that should be hidden. Values: `constant`, `will`, `rest`, `restLong`, `daily`, `weekly`, `monthly`, `yearly`,
         * `ritual`, `spells`, `charges`, `recharge`, `legendary`
         */
        public List<String> hidden;

        /**
         * Attribute should be displayed as specified trait type. Values: `trait` (default), `action`, `bonus`, `reaction`,
         * `legendary`
         */
        public String displayAs = "trait";

        @Override
        public String toString() {
            return "%s%s".formatted(
                    isPresent(name) ? ("***" + name + ".*** ") : "",
                    getDesc());

        }

        /** Formatted description: renders all attributes (except name) unless the trait is hidden */
        public String getDesc() {
            List<String> text = new ArrayList<>();
            if (!headerEntries.isEmpty()) {
                text.addAll(headerEntries);
                text.add("");
            }

            if (fixed.containsKey("constant") && !hidden.contains("constant")) {
                appendList(text, "Constant", fixed.get("constant"));
            }
            if (fixed.containsKey("will") && !hidden.contains("will")) {
                appendList(text, "At will", fixed.get("will"));
            }
            for (var duration : DurationType.values()) {
                String key = duration.name();
                if (hidden.contains(key)) {
                    continue;
                }
                if (variable.containsKey(key) && !hidden.contains(key)) {
                    Map<String, List<String>> v = variable.get(key);
                    switch (duration) {
                        case recharge -> {
                            Function<String, String> f = (num) -> {
                                // This is a {@recharge} tag
                                return Tools5eIndex.instance().replaceText(String.format(duration.durationText, num));
                            };
                            appendList(text, f, v);
                        }
                        case charges, legendary -> {
                            Function<String, String> f = (num) -> {
                                boolean isEach = num.endsWith("e");
                                String value = String.format(duration.durationText, num.replace("e", ""));
                                return isEach
                                        ? pluralize(value, num.replace("e", "")) + " each"
                                        : pluralize(value, num);
                            };
                            appendList(text, f, v);
                        }
                        default -> {
                            Function<String, String> f = (num) -> {
                                boolean isEach = num.endsWith("e");
                                String value = String.format(duration.durationText, num.replace("e", ""));
                                return isEach
                                        ? value + " each"
                                        : value;
                            };
                            appendList(text, f, v);
                        }
                    }
                }
            }

            if (fixed.containsKey("ritual") && !hidden.contains("ritual")) {
                appendList(text, "Rituals", fixed.get("ritual"));
            }

            if (isPresent(spells) && !hidden.contains("spells")) {
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
                return String.format(" (%s slots)", spells.slots);
            }
            return "";
        }

        void appendList(List<String> text, String title, List<String> spells) {
            if (spells == null || spells.isEmpty()) {
                return;
            }
            maybeAddBlankLine(text);
            text.add(String.format("**%s:** %s", title, String.join(", ", spells)));
        }

        void appendList(List<String> text, Function<String, String> titleFunction, Map<String, List<String>> spells) {
            if (spells == null || spells.isEmpty()) {
                return;
            }
            for (int i = 9; i > 0; i--) {
                String key = String.valueOf(i);
                List<String> spellList = spells.get(key);
                if (isPresent(spellList)) {
                    appendList(text, titleFunction.apply(key), spellList);
                }

                key = key + "e";
                spellList = spells.get(key);
                if (isPresent(spellList)) {
                    appendList(text, titleFunction.apply(key), spellList);
                }
            }
        }
    }

    /**
     * 5eTools creature spell attributes (associated with a spell level)
     */
    @TemplateData
    public static class Spells {
        /** Available spell slots */
        public int slots;
        /** Set if this represents a spell range (the associated key is the upper bound) */
        public int lowerBound;
        /** List of spells (links) */
        public List<String> spells;
    }

    /**
     * Saving throw modifier.
     *
     * Usually an integer, but may be a "special" value (string, homebrew).
     *
     * @param ability Ability name, will be null if "special"
     * @param modifier Modifier value. Will be 0 if unset or "special"
     * @param special Either the "special" value or a non-numeric modifier value
     */
    @TemplateData
    public record SavingThrow(String ability, int modifier, String special) implements QuteUtil {

        public SavingThrow(String ability, String special) {
            this("special".equalsIgnoreCase(ability) ? null : ability,
                    0, special);
        }

        public SavingThrow(String ability, int modifier) {
            this("special".equalsIgnoreCase(ability) ? null : ability,
                    modifier, null);
        }

        public SavingThrow(String ability, AbilityScore score) {
            this(ability, score.modifier(), score.special());
        }

        /** @return true if this saving throw has a "special" value */
        public boolean isSpecial() {
            return special != null;
        }

        public Object mapValue() {
            return isSpecial() ? special : asModifier(modifier);
        }

        @Override
        public String toString() {
            if (isSpecial()) {
                return Tools5eIndex.instance().replaceText(special);
            }
            return "%s %s".formatted(ability, asModifier(modifier));
        }
    }

    /**
     * Skill modifier.
     *
     * Usually an integer, but may be a "special" value (string, homebrew).
     *
     * @param skill Skill name, will be null if "special"
     * @param skillLink Skill name as a link, will be null if "special"
     * @param modifier Modifier value. Will be 0 if unset or "special"
     * @param special Either the "special" value or a non-numeric modifier value
     */
    @TemplateData
    public record SkillModifier(String skill, String skillLink, int modifier, String special) {
        public SkillModifier(String skill, String skillLink, String special) {
            this(skill, skillLink, 0, special);
        }

        public SkillModifier(String skill, String skillLink, int modifier) {
            this(skill, skillLink, modifier, null);
        }

        /** @return true if this saving throw has a "special" value */
        public boolean isSpecial() {
            return special != null;
        }

        public Object mapValue() {
            return isSpecial() ? special : asModifier(modifier);
        }

        @Override
        public String toString() {
            if (isSpecial()) {
                return Tools5eIndex.instance().replaceText(special);
            }
            return "%s %s".formatted(skillLink, asModifier(modifier));
        }
    }

    /**
     * 5eTools creature saving throws and skill attributes.
     */
    @TemplateData
    @RegisterForReflection
    public static class SavesAndSkills implements QuteUtil {
        @JsonIgnore
        private QuteMonster parent;

        private SavesAndSkills withParent(QuteMonster parent) {
            this.parent = parent;
            return this;
        }

        /**
         * Creature saving throws as a list of {@link dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.SavingThrow}.
         */
        public List<SavingThrow> saves;

        /**
         * Creature skill modifiers as a list of {@link dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.SkillModifier}.
         */
        public List<SkillModifier> skills;

        /**
         * Sometimes creatures have choices (one of the following...)
         * This is a list of lists of {@link dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.SkillModifier},
         * where each sublist is a a group to choose from.
         */
        public List<List<SkillModifier>> skillChoices;

        /** Creature saving throws as a list: Constitution +6, Intelligence +8 */
        public String getSaves() {
            if (!isPresent(saves)) {
                return "";
            }
            return saves.stream()
                    .map(SavingThrow::toString)
                    .collect(Collectors.joining(", "));
        }

        /** Saving throws as a list of maps (for YAML Statblock) */
        public List<Map<String, Object>> getSaveValues() {
            if (!isPresent(saves)) {
                return List.of();
            }
            return saves.stream()
                    .map(s -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        if (s.isSpecial()) {
                            String ability = s.ability();
                            if (ability != null) {
                                map.put("name", ability);
                            }
                            map.put("desc", s.mapValue());
                        } else {
                            map.put(s.ability().toLowerCase(), s.modifier());
                        }
                        return map;
                    })
                    .collect(Collectors.toList());
        }

        /** Saving throws as a list of maps (for YAML Statblock) */
        public Map<String, Object> getSaveOrDefault() {
            Map<String, Object> saveDefaults = new HashMap<>();
            for (String ability : abilities) {
                Optional<SavingThrow> optSt = Optional.empty();
                if (isPresent(saves)) {
                    optSt = saves.stream()
                            .filter(st -> st.ability().equalsIgnoreCase(ability))
                            .findFirst();
                }

                if (optSt.isEmpty()) {
                    AbilityScore score = parent.scores.getScore(ability);
                    if (score != null) {
                        addToMap(saveDefaults, new SavingThrow(ability, score), true);
                    } else {
                        addToMap(saveDefaults, new SavingThrow(getSkills(), "⏤"), true);
                    }
                } else {
                    addToMap(saveDefaults, optSt.get(), false);
                }
            }
            return saveDefaults;
        }

        private void addToMap(Map<String, Object> map, SavingThrow s, boolean isDefault) {
            map.put(s.ability().toLowerCase(),
                    (isDefault ? "%s" : "**%s**").formatted(s.mapValue()));
        }

        /**
         * Creature skills as a list (with links)
         *
         * - `[History](..) +12, [Perception](...) +12`
         * - `[History](..) +12; [Perception](...) +12; _One of_ [Athletics](...) +12 or [Acrobatics](...) +12`
         *
         */
        public String getSkills() {
            if (!isPresent(skills) && !isPresent(skillChoices)) {
                return "";
            }
            String separator = ", ";
            List<String> text = new ArrayList<>();
            if (isPresent(skills)) {
                text.addAll(skills.stream()
                        .map(SkillModifier::toString)
                        .collect(Collectors.toList()));
            }
            List<String> choices = flattenChoices();
            if (isPresent(choices)) {
                text.addAll(choices);
                separator = "; ";
            }
            return String.join(separator, text);
        }

        /** Skill modifiers as a list of maps (for YAML Statblock) */
        public List<Map<String, Object>> getSkillValues() {
            if (!isPresent(skills) && !isPresent(skillChoices)) {
                return List.of();
            }
            List<Map<String, Object>> skillList = new ArrayList<>();
            for (SkillModifier s : skills) {
                Map<String, Object> map = new LinkedHashMap<>();
                String name = s.skillLink();
                if (name != null) {
                    map.put("name", name);
                }
                map.put("desc", s.mapValue());
                skillList.add(map);
            }
            List<String> choices = flattenChoices();
            if (isPresent(choices)) {
                for (String choice : choices) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("desc", choice);
                    skillList.add(map);
                }
            }
            return skillList;
        }

        public List<String> flattenChoices() {
            if (skillChoices == null || skillChoices.isEmpty()) {
                return List.of();
            }
            List<String> text = new ArrayList<>();
            for (List<SkillModifier> chooseOneSkill : skillChoices) {
                if (chooseOneSkill.isEmpty()) {
                    continue;
                }
                List<String> inner = new ArrayList<>();
                inner.addAll(chooseOneSkill.stream()
                        .map(SkillModifier::toString)
                        .collect(Collectors.toList()));
                text.add("\n\nOne of " + joinConjunct(" or ", inner));
            }
            return text;
        }
    }

    /**
     * 5eTools creature initiative attributes.
     *
     * @param bonus Initiative modifier
     * @param mode Initiative mode: "advantage", "disadvantage", or "none"
     * @param passive Passive initiative value (number)
     */
    @TemplateData
    public record Initiative(int bonus, InitiativeMode mode, int passive) {
        public Initiative(int bonus) {
            this(bonus, InitiativeMode.none, 10 + bonus);
        }

        public Initiative(int bonus, InitiativeMode mode) {
            // const advDisMod = mon.initiative.advantageMode === "adv" ? 5 : mon.initiative.advantageMode === "dis" ? -5 : 0;
            // return 10 + initBonus + advDisMod;
            this(bonus, mode,
                    10 + bonus +
                            (mode == InitiativeMode.advantage
                                    ? 5
                                    : mode == InitiativeMode.disadvantage ? -5 : 0));
        }

        /** String representation of passive initiative value */
        public String getPassiveInitiative() {
            if (mode == InitiativeMode.none) {
                return "`" + passive + "`";
            } else {
                return "<span title=\"This creature has %s on Initiative.\">`%s`</span>".formatted(
                        toTitleCase(mode.name()), passive);
            }
        }

        public String toString() {
            var index = Tools5eIndex.instance();
            return index.replaceText("{@initiative %s} (%s)".formatted(
                    bonus,
                    getPassiveInitiative()));
        }
    }

    /**
     * Initiative mode: "advantage", "disadvantage", or "none"
     */
    public enum InitiativeMode {
        /** Creature rolls initiative with advantage */
        advantage,
        /** Creature rolls initiative with disadvantage */
        disadvantage,
        /** Creature rolls initiative normally (default) */
        none;

        public static InitiativeMode fromString(String string) {
            if (string == null || string.isBlank()) {
                return none;
            }
            var lower = string.toLowerCase();
            if (lower.startsWith("adv")) {
                return advantage;
            } else if (lower.startsWith("dis")) {
                return disadvantage;
            }
            return none;
        }
    }

    @JavadocIgnore
    @TemplateData
    public enum HiddenType {
        constant,
        will,
        rest,
        restLong,
        daily,
        weekly,
        monthly,
        yearly,
        ritual,
        spells,
        charges,
        recharge,
        legendary,
    }

    @JavadocIgnore
    @TemplateData
    public enum DurationType {
        recharge("{@recharge %s}"),
        legendary("%s legendary action"),
        charges("%s charge"),
        rest("%s/rest"),
        restLong("%s/long rest"),
        daily("%s/day"),
        weekly("%s/week"),
        monthly("%s/month"),
        yearly("%s/year"),;

        final String durationText;

        DurationType(String durationText) {
            this.durationText = durationText;
        }
    }
}
