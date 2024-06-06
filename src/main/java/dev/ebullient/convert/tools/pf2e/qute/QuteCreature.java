package dev.ebullient.convert.tools.pf2e.qute;

import static dev.ebullient.convert.StringUtil.flatJoin;
import static dev.ebullient.convert.StringUtil.format;
import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.parenthesize;
import static dev.ebullient.convert.StringUtil.pluralize;
import static dev.ebullient.convert.StringUtil.toOrdinal;
import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.ebullient.convert.StringUtil;
import dev.ebullient.convert.qute.QuteUtil;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools Creature attributes ({@code creature2md.txt})
 * <p>
 * Extension of {@link Pf2eQuteBase Pf2eQuteBase}
 * </p>
 */
@TemplateData
public class QuteCreature extends Pf2eQuteBase {

    /** Aliases for this note (optional) */
    public final List<String> aliases;
    /** Collection of traits (decorated links, optional) */
    public final Collection<String> traits;
    /** Short creature description (optional) */
    public final String description;
    /** Creature level (number, optional) */
    public final Integer level;
    /** Creature perception (number, optional) */
    public final Integer perception;
    /**
     * Languages as {@link QuteCreature.CreatureLanguages CreatureLanguages}
     */
    public final CreatureLanguages languages;
    /** Defenses (AC, saves, etc) as {@link QuteDataDefenses QuteDataDefenses} */
    public final QuteDataDefenses defenses;
    /**
     * Skill bonuses as {@link QuteCreature.CreatureSkills CreatureSkills}
     */
    public final CreatureSkills skills;
    /** Senses as a list of {@link QuteCreature.CreatureSense CreatureSense} */
    public final List<CreatureSense> senses;
    /** Ability modifiers as a map of (name, modifier) */
    public final Map<String, Integer> abilityMods;
    /** Items held by the creature as a list of strings */
    public final List<String> items;
    /** The creature's speed, as an {@link QuteDataSpeed QuteDataSpeed} */
    public final QuteDataSpeed speed;
    /** The creature's attacks, as a list of {@link QuteInlineAttack QuteInlineAttack} */
    public final List<QuteInlineAttack> attacks;

    /**
     * The creature's abilities, as a
     * {@link QuteCreature.CreatureAbilities CreatureAbilities}.
     */
    public final CreatureAbilities abilities;
    /** The creature's spellcasting capabilities, as a list of {@link QuteCreature.CreatureSpellcasting} */
    public final List<CreatureSpellcasting> spellcasting;

    public QuteCreature(Pf2eSources sources, String text, Tags tags,
            Collection<String> traits, List<String> aliases,
            String description, Integer level, Integer perception,
            QuteDataDefenses defenses, CreatureLanguages languages, CreatureSkills skills,
            List<CreatureSense> senses, Map<String, Integer> abilityMods,
            List<String> items, QuteDataSpeed speed,
            List<QuteInlineAttack> attacks, CreatureAbilities abilities,
            List<CreatureSpellcasting> spellcasting) {
        super(sources, text, tags);
        this.traits = traits;
        this.aliases = aliases;
        this.description = description;
        this.level = level;
        this.perception = perception;
        this.languages = languages;
        this.defenses = defenses;
        this.skills = skills;
        this.senses = senses;
        this.abilityMods = abilityMods;
        this.items = items;
        this.speed = speed;
        this.attacks = attacks;
        this.abilities = abilities;
        this.spellcasting = spellcasting;
    }

    /**
     * The languages and language features known by a creature. Example default output:
     *
     * <blockquote>
     * Common, Sylvan; telepathy 100ft; knows any language the summoner does
     * </blockquote>
     *
     * @param languages Languages known (optional)
     * @param notes Language-related notes (optional)
     * @param abilities Language-related abilities (optional)
     */
    @TemplateData
    public record CreatureLanguages(
            List<String> languages,
            List<String> notes,
            List<String> abilities) implements QuteUtil {

        @Override
        public String toString() {
            return flatJoin("; ", List.of(join(", ", languages)), abilities, notes);
        }
    }

    /**
     * A creature's skill information. Example default output:
     *
     * <blockquote>
     * Athletics +10, Cult Lore +10 (lore on their cult), Stealth +10 (+12 in forests); Some skill note
     * </blockquote>
     *
     * @param skills Skill bonuses for the creature, as a list of
     *        {@link QuteDataGenericStat.QuteDataNamedBonus QuteDataNamedBonus}
     * @param notes Notes for the creature's skills (list of strings, optional)
     */
    @TemplateData
    public record CreatureSkills(
            List<QuteDataGenericStat.QuteDataNamedBonus> skills,
            List<String> notes) {

        @Override
        public String toString() {
            return flatJoin("; ", List.of(join(", ", skills)), notes);
        }
    }

    /**
     * A creature's senses. Example default output:
     * <blockquote>
     * tremorsense (imprecise) 20ft
     * </blockquote>
     *
     * @param name The name of the sense (required, string)
     * @param type The type of the sense - e.g. precise, imprecise (optional, string)
     * @param range The range of the sense (optional, integer)
     */
    @TemplateData
    public record CreatureSense(String name, String type, Integer range) implements QuteUtil {

        @Override
        public String toString() {
            return join(" ", name, parenthesize(type), range);
        }
    }

    public enum SpellcastingTradition {
        arcane,
        divine,
        occult,
        primal;
    }

    public enum SpellcastingPreparation {
        innate,
        prepared,
        spontaneous,
        focus;
    }

    /**
     * A creature's abilities, split into the section of the statblock where they should be displayed. Each section is
     * a list of {@link QuteAbilityOrAffliction QuteAbilityOrAffliction}. Using the value directly will give you a
     * pre-formatted ability according to the embedded template defined for {@link QuteAbility QuteAbility} or
     * {@link QuteAffliction QuteAffliction} as appropriate.
     *
     * @param top Abilities which should be displayed in the top section of the statblock
     * @param middle Abilities which should be displayed in the middle section of the statblock
     * @param bottom Abilities which should be displayed in the bottom section of the statblock
     */
    @TemplateData
    public record CreatureAbilities(
            List<QuteAbilityOrAffliction> top,
            List<QuteAbilityOrAffliction> middle,
            List<QuteAbilityOrAffliction> bottom) implements QuteUtil {
    }

    /**
     * Information about a type of spellcasting available to this creature.
     *
     * @param customName A custom name for this set of spells, e.g. "Champion Devotion Spells". Use
     *        {@link QuteCreature.CreatureSpellcasting#name()} to get a name which takes this into account
     *        if it exists.
     * @param preparation The type of preparation for these spells, as a {@link QuteCreature.SpellcastingPreparation}
     * @param tradition The tradition for these spells, as a {@link QuteCreature.SpellcastingTradition}
     * @param focusPoints The number of focus points available to this creature for these spells. Present only if these
     *        are focus spells.
     * @param attackBonus The spell attack bonus for these spells (integer)
     * @param dc The difficulty class for these spells (integer)
     * @param notes Any notes associated with these spells
     * @param ranks The spells for each rank, as a list of {@link QuteCreature.CreatureSpells}.
     * @param constantRanks The constant spells for each rank, as a list of {@link QuteCreature.CreatureSpells}
     */
    @TemplateData
    public record CreatureSpellcasting(
            String customName,
            SpellcastingPreparation preparation,
            SpellcastingTradition tradition,
            Integer focusPoints,
            Integer attackBonus,
            Integer dc,
            List<String> notes,
            List<CreatureSpells> ranks,
            List<CreatureSpells> constantRanks) {
        /**
         * The name for this set of spells. This is either the custom name, or derived from the tradition and
         * preparation - e.g. "Occult Prepared Spells", or "Divine Innate Spells".
         */
        public String name() {
            return customName != null && !customName.isBlank()
                    ? customName
                    : toTitleCase(join(" ", tradition, preparation, "Spells"));
        }

        /**
         * Stats for this kind of spellcasting, including the DC, attack bonus, and any focus points.
         *
         * <blockquote>
         * DC 20, attack +25, 2 Focus Points
         * </blockquote>
         */
        public String formattedStats() {
            return join(", ",
                    format("DC %d", dc),
                    format("attack %+d", attackBonus),
                    focusPoints == null ? "" : focusPoints + " Focus " + pluralize("Point", focusPoints));
        }
    }

    /**
     * A collection of spells with some additional information.
     * <blockquote>
     * <b>Cantrips (9th)</b> <a href="#">daze</a>, <a href="#">shadow siphon</a> (acid only) (×2)
     * </blockquote>
     * <blockquote>
     * <b>4th</b> <a href="#">confusion</a>, <a href="#">phantasmal killer</a> (2 slots)
     * </blockquote>
     *
     * @param baseRank The base rank for these spells (0 for cantrips).
     * @param knownRank The rank that these spells are known at. Usually present only for cantrips.
     * @param slots The number of slots available for these spells. Not present for constant spells.
     * @param spells A list of spells, as a list of {@link QuteCreature.CreatureSpellReference}
     */
    @TemplateData
    public record CreatureSpells(
            Integer baseRank,
            Integer knownRank,
            Integer slots,
            List<CreatureSpellReference> spells) {
        /** A string of the rank (base and known) for this set of spells. e.g. "5th", or "Cantrips (9th)" */
        public String rank() {
            return join(" ",
                    baseRank == 0 ? "Cantrips" : toOrdinal(baseRank), parenthesize(toOrdinal(knownRank)));
        }

        @Override
        public String toString() {
            return join(" ",
                    "**%s**".formatted(rank()),
                    join(", ", spells),
                    format("(%d slots)", slots));
        }
    }

    /**
     * A spell known by the creature.
     * <blockquote>
     * <a href="#">shadow siphon</a> (acid only) (×2)
     * </blockquote>
     *
     * @param name The name of the spell
     * @param link A formatted link to the spell's note, or just the spell's name if we couldn't get a link.
     * @param amount The number of casts available for this spell. A value of 0 represents an at will spell. Use
     *        {@link QuteCreature.CreatureSpellReference#formattedAmount()} to get this as a formatted string.
     * @param notes Any notes associated with this spell, e.g. "at will only"
     */
    @TemplateData
    public record CreatureSpellReference(
            String name,
            String link,
            Integer amount,
            List<String> notes) {

        /** The number of casts as a formatted string, e.g. "(at will)" or "(×2)". Empty when the amount is 1. */
        public String formattedAmount() {
            return amount == 1 ? "" : parenthesize(amount == 0 ? "at will" : "×" + amount);
        }

        public String formattedNotes() {
            return notes.stream().map(StringUtil::parenthesize).collect(Collectors.joining(" "));
        }

        @Override
        public String toString() {
            return join(" ", link, formattedAmount(), formattedNotes());
        }
    }
}
