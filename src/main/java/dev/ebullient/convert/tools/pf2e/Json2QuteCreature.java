package dev.ebullient.convert.tools.pf2e;

import static dev.ebullient.convert.StringUtil.join;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.QuteCreature;

public class Json2QuteCreature extends Json2QuteBase {

    public Json2QuteCreature(Pf2eIndex index, JsonNode rootNode) {
        super(index, Pf2eIndexType.creature, rootNode);
    }

    @Override
    protected QuteCreature buildQuteResource() {
        return Pf2eCreature.create(rootNode, this);
    }

    /**
     * Example JSON input:
     *
     * <pre>
     *     "perception": {"std": 6},
     *     "defenses": { ... },
     *     "skills": {
     *         "athletics": 30,
     *         "stealth": {
     *             "std": 36,
     *             "in forests": 42,
     *             "note": "additional note"
     *         },
     *         "notes": [
     *             "some note"
     *         ]
     *     },
     *     "abilityMods": {
     *         "str": 10,
     *         "dex": 10,
     *         "con": 10,
     *         "int": 10,
     *         "wis": 10,
     *         "cha": 10
     *     },
     *     "languages": { ... },
     *     "senses": [ { ... } ],
     *     "attacks": [ { ... } ],
     * </pre>
     */
    enum Pf2eCreature implements Pf2eJsonNodeReader {
        abilities,
        abilityMods,
        alignment,
        alias,
        attacks,
        defenses,
        description,
        entries,
        hasImages,
        inflicts, // not actually present in any of the entries
        isNpc,
        items,
        languages,
        level,
        notes,
        perception,
        rarity,
        rituals,
        senses,
        size,
        skills,
        speed,
        spellcasting,
        std,
        traits;

        private static QuteCreature create(JsonNode node, JsonSource convert) {
            Tags tags = new Tags(convert.getSources());
            Collection<String> traits = convert.collectTraitsFrom(node, tags);
            traits.addAll(alignment.getAlignmentsFrom(node, convert));

            return new QuteCreature(convert.getSources(),
                    entries.transformTextFrom(node, "\n", convert, "##"),
                    tags,
                    traits,
                    alias.replaceTextFromList(node, convert),
                    description.replaceTextFrom(node, convert),
                    level.intOrNull(node),
                    perception.getObjectFrom(node).map(std::intOrThrow).orElse(null),
                    defenses.getDefensesFrom(node, convert),
                    languages.getLanguagesFrom(node, convert),
                    skills.getSkillsFrom(node, convert),
                    senses.getSensesFrom(node, convert),
                    // Use a linked hash map to preserve insertion order
                    abilityMods.streamProps(node).collect(Collectors.toMap(
                            Map.Entry::getKey, e -> e.getValue().asInt(), (u, v) -> u, LinkedHashMap::new)),
                    items.replaceTextFromList(node, convert),
                    speed.getSpeedFrom(node, convert),
                    attacks.getAttacksFrom(node, convert),
                    abilities.getCreatureAbilitiesFrom(node, convert),
                    spellcasting.getSpellcastingFrom(node, convert),
                    rituals.getRitualsFrom(node, convert));
        }

        private QuteCreature.CreatureSkills getSkillsFrom(JsonNode source, JsonSource convert) {
            return getObjectFrom(source)
                    .map(n -> new QuteCreature.CreatureSkills(
                            streamPropsExcluding(source, notes)
                                    .map(e -> Pf2eNamedBonus.getNamedBonus(e.getKey(), e.getValue(), convert))
                                    .toList(),
                            notes.replaceTextFromList(n, convert)))
                    .filter(c -> !c.skills().isEmpty() || !c.notes().isEmpty())
                    .orElse(null);
        }

        private QuteCreature.CreatureLanguages getLanguagesFrom(JsonNode node, JsonSource convert) {
            return getObjectFrom(node)
                    .map(n -> new QuteCreature.CreatureLanguages(
                            Pf2eCreatureLanguages.languages.getListOfStrings(n, convert.tui()),
                            Pf2eCreatureLanguages.abilities.replaceTextFromList(n, convert),
                            Pf2eCreatureLanguages.notes.replaceTextFromList(n, convert)))
                    .filter(c -> !c.languages().isEmpty() || !c.abilities().isEmpty() || !c.notes().isEmpty())
                    .orElse(null);
        }

        enum Pf2eCreatureLanguages implements Pf2eJsonNodeReader {
            /** Known languages e.g. {@code ["Common", "Sylvan"]} */
            languages,
            /** Language-related abilities e.g. {@code ["{@ability telepathy} 100 feet", "(understands its creator)"]} */
            abilities,
            /** Notes e.g. {@code ["one elemental language", "one planar language"]} */
            notes;
        }

        /** Returns a {@link QuteCreature.CreatureAbilities} (with empty lists if this field is not present). */
        private QuteCreature.CreatureAbilities getCreatureAbilitiesFrom(JsonNode source, JsonSource convert) {
            return getObjectFrom(source)
                    .map(n -> new QuteCreature.CreatureAbilities(
                            Pf2eCreatureAbilities.top.getAbilityOrAfflictionsFrom(n, convert),
                            Pf2eCreatureAbilities.mid.getAbilityOrAfflictionsFrom(n, convert),
                            Pf2eCreatureAbilities.bot.getAbilityOrAfflictionsFrom(n, convert)))
                    .orElseGet(() -> new QuteCreature.CreatureAbilities(List.of(), List.of(), List.of()));
        }

        enum Pf2eCreatureAbilities implements Pf2eJsonNodeReader {
            top,
            mid,
            bot;
        }

        private List<QuteCreature.CreatureSense> getSensesFrom(JsonNode source, JsonSource convert) {
            return streamFrom(source)
                    .filter(convert::isObjectNode)
                    .map(n -> new QuteCreature.CreatureSense(
                            Pf2eCreatureSense.name.getTextFrom(n).map(convert::replaceText).orElseThrow(),
                            Pf2eCreatureSense.type.getTextFrom(n).map(convert::replaceText).orElse(null),
                            Pf2eCreatureSense.range.intOrNull(n)))
                    .toList();
        }

        enum Pf2eCreatureSense implements Pf2eJsonNodeReader {
            /** Name of the sense, e.g. {@code "scent"} (required) */
            name,
            /** Type of sense, e.g. {@code "precise"}, {@code "imprecise"}, {@code "vague"}, or {@code "other"} */
            type,
            /** Range of the sense (in feet, usually), optional integer. */
            range;
        }

        private List<QuteCreature.CreatureSpellcasting> getSpellcastingFrom(JsonNode source, JsonSource convert) {
            return streamFrom(source)
                    .map(n -> Pf2eCreatureSpellcasting.getSpellcasting(n, convert))
                    .filter(spellcasting -> !spellcasting.ranks().isEmpty() || !spellcasting.constantRanks().isEmpty())
                    .toList();
        }

        private List<QuteCreature.CreatureRitualCasting> getRitualsFrom(JsonNode source, JsonSource convert) {
            return streamFrom(source)
                    .map(n -> Pf2eCreatureSpellcasting.getRitual(n, convert))
                    .filter(rituals -> !rituals.ranks().isEmpty())
                    .toList();
        }

        enum Pf2eCreatureSpellcasting implements Pf2eJsonNodeReader {
            /** e.g. {@code "Champion Devotion Spells"} */
            name,
            /** e.g. {@code "see soul spells below"} */
            note,
            /** Required - one of {@code "arcane"}, {@code "divine"}, {@code "occult"}, or {@code "primal"} */
            tradition,
            /** Required - DC for spell effects */
            DC,

            /** Rituals only. Array of ritual references - see {@link Pf2eCreatureSpellReference} */
            rituals,

            /** Required - one of {@code "Innate"}, {@code "Prepared"}, {@code "Spontaneous"}, or {@code "Focus"} */
            type,
            /** Integer - number of focus points available */
            fp,
            /** Integer - The spell attack bonus */
            attack,
            /** Used within {@link #entry} only, as a key for a block. */
            constant,
            /**
             * An object where the keys are the spell rank or {@link #constant}, and the values are another object with
             * keys described below.
             */
            entry,

            /**
             * Used within {@link #entry} only. Integer. The level that these spells are heightened to - usually the
             * same as the key for this block, except for cantrips.
             */
            level,
            /**
             * Used within {@link #entry} only. Integer. The number of slots available to cast the spells within this
             * block.
             */
            slots,
            /**
             * Used within {@link #entry} only. A list of spell references.
             *
             * @see Pf2eCreatureSpellReference
             */
            spells;

            private static QuteCreature.CreatureRitualCasting getRitual(JsonNode source, JsonSource convert) {
                return new QuteCreature.CreatureRitualCasting(
                        tradition.getEnumValueFrom(source, QuteCreature.SpellcastingTradition.class),
                        DC.intOrNull(source),
                        rituals.streamFrom(source)
                                .collect(Collectors.toMap(
                                        n -> level.intOrNull(n),
                                        n -> Stream.of(Pf2eCreatureSpellReference.getSpellReference(n, convert)),
                                        Stream::concat))
                                .entrySet().stream()
                                .map(e -> new QuteCreature.CreatureSpells(e.getKey(), e.getValue().toList()))
                                .toList());
            }

            private static QuteCreature.CreatureSpellcasting getSpellcasting(JsonNode source, JsonSource convert) {
                return new QuteCreature.CreatureSpellcasting(
                        name.getTextOrNull(source),
                        type.getEnumValueFrom(source, QuteCreature.SpellcastingPreparation.class),
                        tradition.getEnumValueFrom(source, QuteCreature.SpellcastingTradition.class),
                        fp.intOrNull(source),
                        attack.intOrNull(source),
                        DC.intOrNull(source),
                        note.replaceTextFromList(source, convert),
                        entry.getSpellsFrom(source, convert),
                        constant.getSpellsFrom(entry.getFromOrEmptyObjectNode(source), convert));
            }

            private List<QuteCreature.CreatureSpells> getSpellsFrom(JsonNode source, JsonSource convert) {
                return streamPropsExcluding(source, constant)
                        .map(e -> new QuteCreature.CreatureSpells(
                                Integer.valueOf(e.getKey()),
                                level.intOrNull(e.getValue()),
                                slots.intOrNull(e.getValue()),
                                spells.streamFrom(e.getValue())
                                        .map(n -> Pf2eCreatureSpellReference.getSpellReference(n, convert))
                                        .toList()))
                        .filter(creatureSpells -> !creatureSpells.spells().isEmpty())
                        .sorted(Comparator.comparing(QuteCreature.CreatureSpells::knownRank).reversed())
                        .toList();
            }
        }

        enum Pf2eCreatureSpellReference implements Pf2eJsonNodeReader {
            /** e.g. {@code "comprehend language"}. */
            name,
            /** The book source for this spell, e.g. {@code "BotD"} */
            source,
            /** Integer, or {@code "at will"}. Amount of available casts. For spells only, not rituals. */
            amount,
            /** e.g. {@code ["self only"]} */
            notes;

            private static QuteCreature.CreatureSpellReference getSpellReference(JsonNode node, JsonSource convert) {
                String spellName = name.getTextOrThrow(node);
                return new QuteCreature.CreatureSpellReference(
                        spellName,
                        convert.linkify(Pf2eIndexType.spell, join("|", spellName, source.getTextOrNull(node))),
                        amount.getTextFrom(node)
                                .filter(s -> s.equalsIgnoreCase("at will"))
                                .map(unused -> 0)
                                .or(() -> amount.intFrom(node))
                                .orElse(1),
                        notes.replaceTextFromList(node, convert));
            }
        }

    }
}
