package dev.ebullient.convert.tools.pf2e;

import com.fasterxml.jackson.databind.JsonNode;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.QuteCreature;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataDefenses;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
                Pf2eCreatureLanguages.createCreatureLanguages(Pf2eCreature.languages.getFrom(rootNode), this));
    }

    /**
     * <pre>
     *     "perception": {
     *         "std": 6
     *     }
     * </pre>
     */
    private Integer getPerception() {
        JsonNode perceptionNode = Pf2eCreature.perception.getFrom(rootNode);
        if (perceptionNode == null) {
            return null;
        }
        return Pf2eCreature.std.getIntOrThrow(perceptionNode);
    }

    /**
     * <pre>
     *     "defenses": { ... }
     * </pre>
     */
    private QuteDataDefenses buildDefenses() {
        JsonNode defenseNode = Pf2eCreature.defenses.getFrom(rootNode);
        if (defenseNode == null) {
            return null;
        }
        return Pf2eDefenses.createInlineDefenses(defenseNode, this);
    }

    /**
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
