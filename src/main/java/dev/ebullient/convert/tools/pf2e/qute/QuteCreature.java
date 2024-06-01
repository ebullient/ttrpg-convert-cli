package dev.ebullient.convert.tools.pf2e.qute;

import static dev.ebullient.convert.StringUtil.flatJoin;
import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.parenthesize;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    public QuteCreature(Pf2eSources sources, String text, Tags tags,
            Collection<String> traits, List<String> aliases,
            String description, Integer level, Integer perception,
            QuteDataDefenses defenses, CreatureLanguages languages, CreatureSkills skills,
            List<CreatureSense> senses, Map<String, Integer> abilityMods,
            List<String> items, QuteDataSpeed speed,
            List<QuteInlineAttack> attacks, CreatureAbilities abilities) {
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

    /**
     * A creature's abilities, split into the section of the statblock where they should be displayed. Each section is
     * a list of {@link QuteAbilityOrAffliction QuteAbilityOrAffliction}. Use
     * {@link QuteCreature.CreatureAbilities#formattedTop() formattedTop},
     * {@link QuteCreature.CreatureAbilities#formattedTop() formattedMiddle}, and
     * {@link QuteCreature.CreatureAbilities#formattedTop() formattedBottom} to
     * get pre-formatted abilities according to the templates defined for
     * {@link QuteAbility QuteAbility} or
     * {@link QuteAffliction QuteAffliction}.
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

        /** Returns the top-section abilities as a formatted string */
        public String formattedTop() {
            return formatted(top);
        }

        /** Returns the middle-section abilities as a formatted string. */
        public String formattedMiddle() {
            return formatted(middle);
        }

        /** Returns the bottom-section abilities as a formatted string. */
        public String formattedBottom() {
            return formatted(bottom);
        }

        private String formatted(List<QuteAbilityOrAffliction> abilities) {
            return join("\n", abilities);
        }
    }
}
