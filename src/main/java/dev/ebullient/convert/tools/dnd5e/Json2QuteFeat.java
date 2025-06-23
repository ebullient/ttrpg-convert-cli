package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.QuteFeat;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QuteFeat extends Json2QuteCommon {
    public Json2QuteFeat(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {
        Tags tags = new Tags(getSources());
        tags.add("feat");

        List<ImageRef> images = getFluffImages(Tools5eIndexType.featFluff);

        String category = featureTypeToFull(FeatFields.category.getTextOrEmpty(rootNode));
        if (category.equalsIgnoreCase("Fighting Style, Paladin")) {
            category = "Fighting Style Replacement (Paladin)";
        } else if (category.equalsIgnoreCase("Fighting Style, Ranger")) {
            category = "Fighting Style Replacement (Ranger)";
        }

        // Initialize full entries with ability score increases merged
        JsonNode fullEntries = initFullEntries();

        // Convert entries to text
        List<String> text = new ArrayList<>();
        appendToText(text, fullEntries, null);

        // TODO: update w/ additionalSpells
        return new QuteFeat(sources,
                linkifier().decoratedName(type, rootNode),
                getSourceText(sources),
                listPrerequisites(rootNode),
                null, // Level coming someday..
                SkillOrAbility.getAbilityScoreIncreases(FeatFields.ability.getFrom(rootNode)),
                category,
                images,
                String.join("\n", text),
                tags);
    }

    JsonNode initFullEntries() {
        ArrayNode entries = SourceField.entries.readArrayFrom(rootNode);

        var abilities = FeatFields.ability.streamFrom(rootNode)
                .filter(x -> !Tools5eFields.hidden.booleanOrDefault(x, false))
                .toList();

        // If there are no abilities, just return the entries as is
        if (abilities.isEmpty()) {
            return entries;
        }

        // Find the first element of type "list"
        var targetList = streamOf(entries)
                .filter(node -> SourceField.type.getTextOrEmpty(node).equals("list"))
                .findFirst().orElse(null);

        if (targetList != null) {
            var items = Tools5eFields.items.readArrayFrom(targetList);
            boolean allItems = Tools5eFields.items.streamFrom(targetList)
                    .allMatch(x -> SourceField.type.getTextOrEmpty(x).equals("item"));

            for (JsonNode abilityNode : abilities) {
                items.insert(0, allItems
                        ? entryToListItemItem(abilityNode)
                        : entryToListItemText(abilityNode));
            }
        } else {
            // No list found, handle other cases...
            int firstEntriesIndex = -1;
            for (int i = 0; i < entries.size(); i++) {
                if (SourceField.type.getTextOrEmpty(entries.get(i)).equals("entries")) {
                    firstEntriesIndex = i;
                    break;
                }
            }
            if (firstEntriesIndex >= 0) {
                // Gather all displayed abilities
                var abilityEntries = mapper().createArrayNode();
                for (JsonNode abilityNode : abilities) {
                    abilityEntries.add(entryToListItemText(abilityNode));
                }

                // Create new entries element for abilities
                var abilityEntry = mapper().createObjectNode()
                        .put("type", "entries")
                        .put("name", "Ability Score Increase")
                        .set("entries", abilityEntries);

                // Insert the new ability entry at the beginning
                entries.insert(firstEntriesIndex, abilityEntry);
            } else {
                // No nested entries found, just return the original entries
                for (JsonNode abilityNode : abilities) {
                    entries.insert(0, entryToListItemText(abilityNode));
                }
            }
        }

        return entries;
    }

    JsonNode entryToListItemText(JsonNode abilityNode) {
        return new TextNode(SkillOrAbility.getAbilityScoreIncrease(abilityNode));
    }

    JsonNode entryToListItemItem(JsonNode abilityNode) {
        return mapper().createObjectNode()
                .put("type", "item")
                .put("name", "Ability Score Increase")
                .put("entry", SkillOrAbility.getAbilityScoreIncrease(abilityNode));
    }

    enum FeatFields implements JsonNodeReader {
        ability,
        additionalSpells,
        category,
        ;
    }
}
