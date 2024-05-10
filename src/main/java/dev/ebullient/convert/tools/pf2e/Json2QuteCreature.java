package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.QuteCreature;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataDefenses;

public class Json2QuteCreature extends Json2QuteBase {

    public Json2QuteCreature(Pf2eIndex index, JsonNode rootNode) {
        super(index, Pf2eIndexType.creature, rootNode);
    }

    @Override
    protected QuteCreature buildQuteResource() {
        List<String> text = new ArrayList<>();
        Tags tags = new Tags(sources);

        appendToText(text, SourceField.entries.getFrom(rootNode), "##");

        Collection<String> traits = collectTraitsFrom(rootNode, tags);
        if (Pf2eCreature.alignment.existsIn(rootNode)) {
            traits.addAll(getAlignments(Pf2eCreature.alignment.getFrom(rootNode)));
        }

        return new QuteCreature(sources, text, tags,
                traits,
                Field.alias.replaceTextFromList(rootNode, this),
                Pf2eCreature.description.replaceTextFrom(rootNode, this),
                Pf2eCreature.level.getIntFrom(rootNode).orElse(null),
                Pf2eCreature.perception(rootNode),
                Pf2eCreature.defenses(rootNode, this),
                Pf2eCreatureLanguages.create(Pf2eCreature.languages.getFrom(rootNode), this),
                Pf2eCreature.skills(rootNode, this),
                Pf2eCreature.senses.streamFrom(rootNode).map(n -> Pf2eCreatureSense.create(n, this)).toList(),
                Pf2eCreature.abilityModifiers(rootNode),
                Pf2eCreature.items.replaceTextFromList(rootNode, this));
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
    enum Pf2eCreatureLanguages implements JsonNodeReader {
        languages,
        abilities,
        notes;

        static QuteCreature.CreatureLanguages create(JsonNode node, Pf2eTypeReader convert) {
            return node == null ? null
                    : new QuteCreature.CreatureLanguages(
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
    enum Pf2eCreatureSense implements JsonNodeReader {
        name,
        type,
        range;

        static QuteCreature.CreatureSense create(JsonNode node, Pf2eTypeReader convert) {
            return node == null ? null
                    : new QuteCreature.CreatureSense(
                            name.getTextFrom(node).map(convert::replaceText).orElseThrow(),
                            type.getTextFrom(node).map(convert::replaceText).orElse(null),
                            range.getIntFrom(node).orElse(null));
        }
    }

    enum Pf2eCreature implements JsonNodeReader {
        abilities,
        abilityMods,
        alignment,
        attacks,
        defenses,
        description,
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


        /**
         * Example JSON input:
         *
         * <pre>
         *     "perception": {
         *         "std": 6
         *     }
         * </pre>
         */
        private static Integer perception(JsonNode source) {
            return perception.getObjectFrom(source).map(std::getIntOrThrow).orElse(null);
        }

        /**
         * Example JSON input:
         *
         * <pre>
         *     "defenses": { ... }
         * </pre>
         */
        private static QuteDataDefenses defenses(JsonNode source, Pf2eTypeReader convert) {
            return defenses.getObjectFrom(source).map(n -> Pf2eDefenses.createInlineDefenses(n, convert)).orElse(null);
        }

        /**
         * Example JSON input:
         *
         * <pre>
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
         *     }
         * </pre>
         */
        private static QuteCreature.CreatureSkills skills(JsonNode source, Pf2eTypeReader convert) {
            return new QuteCreature.CreatureSkills(
                skills.streamPropsExcluding(source, notes)
                    .map(e -> Pf2eTypeReader.Pf2eSkillBonus.createSkillBonus(e.getKey(), e.getValue(), convert))
                    .toList(),
                notes.replaceTextFromList(source, convert));
        }

        /**
         * Example JSON input:
         *
         * <pre>
         *     {
         *         "str": 10,
         *         "dex": 10,
         *         "con": 10,
         *         "int": 10,
         *         "wis": 10,
         *         "cha": 10
         *     }
         * </pre>
         */
        private static Map<String, Integer> abilityModifiers(JsonNode source) {
            // Use a linked hash map to preserve insertion order
            Map<String, Integer> mods = new LinkedHashMap<>();
            abilityMods.streamPropsExcluding(source).forEachOrdered(e -> mods.put(e.getKey(), e.getValue().asInt()));
            return mods;
        }
    }
}
