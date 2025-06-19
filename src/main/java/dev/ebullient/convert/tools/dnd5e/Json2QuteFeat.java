package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.isPresent;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

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

        List<ImageRef> images = new ArrayList<>();
        List<String> text = getFluff(Tools5eIndexType.featFluff, "##", images);
        appendToText(text, SourceField.entries.getFrom(rootNode), "##");

        // TODO: update w/ additionalSpells
        return new QuteFeat(sources,
                linkifier().decoratedName(type, rootNode),
                getSourceText(sources),
                listPrerequisites(rootNode),
                null, // Level coming someday..
                SkillOrAbility.getAbilityScoreIncreases(FeatFields.ability.getFrom(rootNode)),
                categoryToFull(FeatFields.category.getTextOrEmpty(rootNode)),
                images,
                String.join("\n", text),
                tags);
    }

    protected String categoryToFull(String category) {
        if (!isPresent(category)) {
            return "";
        }
        return switch (category.toUpperCase()) {
            case "EB" -> "Epic Boon Feat";
            case "FS" -> "Fighting Style Feat";
            case "FS:P" -> "Fighting Style Replacement (Paladin)";
            case "FS:R" -> "Fighting Style Replacement (Ranger)";
            case "G" -> "General Feat";
            case "O" -> "Origin Feat";
            default -> category;
        };
    };

    enum FeatFields implements JsonNodeReader {
        ability,
        additionalSpells,
        category,
        ;
    }
}
