package dev.ebullient.convert.tools.pf2e;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Json2QuteAffliction.Pf2eAffliction;
import dev.ebullient.convert.tools.pf2e.qute.QuteAbilityOrAffliction;
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
     *     "top": [ { ... } ],
     *     "mid": [ { ... } ],
     *     "bot": [ { ... } ],
     * </pre>
     */
    enum Pf2eCreatureAbilities implements Pf2eJsonNodeReader {
        top,
        mid,
        bot;

        static QuteCreature.CreatureAbilities create(JsonNode node, Pf2eTypeReader convert) {
            return new QuteCreature.CreatureAbilities(
                    createAbilityList(top.ensureArrayIn(node), convert),
                    createAbilityList(mid.ensureArrayIn(node), convert),
                    createAbilityList(bot.ensureArrayIn(node), convert));
        }

        /**
         * Example JSON input:
         *
         * <pre>
         *     [
         *       { &lt;ability data&gt; },
         *       { "type": "affliction", &lt;affliction data&gt; }
         *     ]
         * </pre>
         */
        private static List<QuteAbilityOrAffliction> createAbilityList(JsonNode node, Pf2eTypeReader convert) {
            // The Pf2e schema doesn't match the data here - afflictions are marked with "type": "affliction", but
            // abilities are unmarked.
            return convert.streamOf(node)
                    .filter(Pf2eAffliction::isAfflictionBlock) // for now, we only want afflictions
                    .map(n -> (QuteAbilityOrAffliction) Pf2eAffliction.createInlineAffliction(n, convert))
                    .toList();
        }
    }

    /**
     * Example JSON input:
     *
     * <pre>
     *     "languages": {
     *         "languages": ["Common", "Sylvan"],
     *         "abilities": ["{&#64;ability telepathy} 100 feet"],
     *         "notes": ["some other notes"],
     *     }
     * </pre>
     */
    enum Pf2eCreatureLanguages implements Pf2eJsonNodeReader {
        languages,
        abilities,
        notes;

        static QuteCreature.CreatureLanguages create(JsonNode node, Pf2eTypeReader convert) {
            return new QuteCreature.CreatureLanguages(
                    languages.getListOfStrings(node, convert.tui()),
                    abilities.replaceTextFromList(node, convert),
                    notes.replaceTextFromList(node, convert));
        }
    }

    /**
     * Example JSON input:
     *
     * <pre>
     *     {
     *         "name": "scent",
     *         "type": "imprecise",
     *         "range": 60,
     *     }
     * </pre>
     */
    enum Pf2eCreatureSense implements Pf2eJsonNodeReader {
        name,
        type,
        range;

        static QuteCreature.CreatureSense create(JsonNode node, Pf2eTypeReader convert) {
            return new QuteCreature.CreatureSense(
                    name.getTextFrom(node).map(convert::replaceText).orElseThrow(),
                    type.getTextFrom(node).map(convert::replaceText).orElse(null),
                    range.getIntFrom(node).orElse(null));
        }
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
        inflicts,
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

        private static QuteCreature create(JsonNode node, Pf2eTypeReader convert) {
            Tags tags = new Tags(convert.getSources());
            Collection<String> traits = convert.collectTraitsFrom(node, tags);
            traits.addAll(alignment.getAlignmentsFrom(node, convert));

            return new QuteCreature(convert.getSources(),
                    entries.transformTextFrom(node, "\n", convert, "##"),
                    tags,
                    traits,
                    alias.replaceTextFromList(node, convert),
                    description.replaceTextFrom(node, convert),
                    level.getIntFrom(node).orElse(null),
                    perception.getObjectFrom(node).map(std::getIntOrThrow).orElse(null),
                    defenses.getObjectFrom(node).map(n -> Pf2eDefenses.createInlineDefenses(n, convert)).orElse(null),
                    languages.getObjectFrom(node).map(n -> Pf2eCreatureLanguages.create(n, convert)).orElse(null),
                    new QuteCreature.CreatureSkills(
                            skills.streamPropsExcluding(node, notes)
                                    .map(e -> Pf2eSkillBonus.createSkillBonus(e.getKey(), e.getValue(), convert))
                                    .toList(),
                            notes.replaceTextFromList(node, convert)),
                    senses.streamFrom(node)
                            .map(n -> convert.isObjectNode(n) ? Pf2eCreatureSense.create(n, convert) : null)
                            .filter(Objects::nonNull)
                            .toList(),
                    // Use a linked hash map to preserve insertion order
                    abilityMods.streamProps(node)
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey, e -> e.getValue().asInt(), (u, v) -> u, LinkedHashMap::new)),
                    items.replaceTextFromList(node, convert),
                    speed.getSpeedFrom(node, convert),
                    attacks.streamFrom(node)
                            .map(n -> convert.isObjectNode(n) ? Pf2eAttack.createInlineAttack(n, convert) : null)
                            .filter(Objects::nonNull)
                            .toList(),
                    Pf2eCreatureAbilities.create(abilities.getFromOrEmptyObjectNode(node), convert));
        }
    }
}
