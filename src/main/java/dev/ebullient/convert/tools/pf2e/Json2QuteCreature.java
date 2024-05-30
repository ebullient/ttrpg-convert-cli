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
                    level.getIntFrom(node).orElse(null),
                    perception.getPerceptionFrom(node),
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
                    abilities.getCreatureAbilitiesFrom(node, convert));
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

        private Integer getPerceptionFrom(JsonNode node) {
            return getObjectFrom(node).map(std::getIntOrThrow).orElse(null);
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
                            Pf2eCreatureAbilities.top.getAbilitiesFrom(n, convert),
                            Pf2eCreatureAbilities.mid.getAbilitiesFrom(n, convert),
                            Pf2eCreatureAbilities.bot.getAbilitiesFrom(n, convert)))
                    .orElseGet(() -> new QuteCreature.CreatureAbilities(List.of(), List.of(), List.of()));
        }

        enum Pf2eCreatureAbilities implements Pf2eJsonNodeReader {
            top,
            mid,
            bot;

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
            private List<QuteAbilityOrAffliction> getAbilitiesFrom(JsonNode node, JsonSource convert) {
                // The Pf2e schema doesn't match the data here - afflictions are marked with "type": "affliction", but
                // abilities are unmarked.
                return streamFrom(node)
                        .filter(Pf2eAffliction::isAfflictionBlock) // for now, we only want afflictions
                        .map(n -> (QuteAbilityOrAffliction) Pf2eAffliction.createInlineAffliction(n, convert))
                        .toList();
            }
        }

        private List<QuteCreature.CreatureSense> getSensesFrom(JsonNode source, JsonSource convert) {
            return streamFrom(source)
                    .filter(convert::isObjectNode)
                    .map(n -> new QuteCreature.CreatureSense(
                            Pf2eCreatureSense.name.getTextFrom(n).map(convert::replaceText).orElseThrow(),
                            Pf2eCreatureSense.type.getTextFrom(n).map(convert::replaceText).orElse(null),
                            Pf2eCreatureSense.range.getIntFrom(n).orElse(null)))
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
    }
}
