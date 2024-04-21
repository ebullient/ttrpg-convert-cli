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
                buildDefenses());
    }

    private QuteDataDefenses buildDefenses() {
        JsonNode defenseNode = Pf2eCreature.defenses.getFrom(rootNode);
        if (defenseNode == null) {
            return null;
        }
        return Pf2eDefenses.createInlineDefenses(defenseNode, this);
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
        traits,
    }
}
