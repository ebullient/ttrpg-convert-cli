package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
            traits.addAll(toAlignments(rootNode, Pf2eCreature.alignment));
        }
        Optional<Integer> level = Pf2eCreature.level.getIntFrom(rootNode);

        return new QuteCreature(sources, text, tags,
                traits,
                Field.alias.replaceTextFromList(rootNode, this),
                Pf2eCreature.description.replaceTextFrom(rootNode, this),
                level.orElse(null),
                getPerception(),
                buildDefenses(),
                Pf2eCreatureLanguages.createCreatureLanguages(Pf2eCreature.languages.getFrom(rootNode), this),
                buildSkills());
    }

    /**
     * Example JSON input:
     *
     * <pre>
     *     "perception": {
     *         "std": 6
     *     }
     * </pre>
     */
    private Integer getPerception() {
        JsonNode perceptionNode = Pf2eCreature.perception.getFrom(rootNode);
        if (perceptionNode == null || !perceptionNode.isObject()) {
            return null;
        }
        return Pf2eCreature.std.getIntOrThrow(perceptionNode);
    }

    /**
     * Example JSON input:
     *
     * <pre>
     *     "defenses": { ... }
     * </pre>
     */
    private QuteDataDefenses buildDefenses() {
        JsonNode defenseNode = Pf2eCreature.defenses.getFrom(rootNode);
        if (defenseNode == null || !defenseNode.isObject()) {
            return null;
        }
        return Pf2eDefenses.createInlineDefenses(defenseNode, this);
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
    private QuteCreature.CreatureSkills buildSkills() {
        JsonNode skillsNode = Pf2eCreature.skills.getFrom(rootNode);
        if (skillsNode == null || !skillsNode.isObject()) {
            return null;
        }
        return new QuteCreature.CreatureSkills(
                skillsNode.properties().stream()
                        .filter(e -> !e.getKey().equals(Pf2eCreature.notes.name()))
                        .map(e -> Pf2eTypeReader.Pf2eSkillBonus.createSkillBonus(e.getKey(), e.getValue(), this))
                        .toList(),
                Pf2eCreature.notes.replaceTextFromList(rootNode, this));
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

        static QuteCreature.CreatureLanguages createCreatureLanguages(JsonNode node, Pf2eTypeReader convert) {
            if (node == null) {
                return null;
            }
            return new QuteCreature.CreatureLanguages(
                    languages.getListOfStrings(node, convert.tui()),
                    abilities.getListOfStrings(node, convert.tui()).stream().map(convert::replaceText).toList(),
                    notes.getListOfStrings(node, convert.tui()).stream().map(convert::replaceText).toList());
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
        traits,
    }
}
