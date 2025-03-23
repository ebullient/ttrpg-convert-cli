package dev.ebullient.convert.tools.dnd5e;

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

        // TODO: update w/ category, additionalSpells
        return new QuteFeat(sources,
                type.decoratedName(rootNode),
                getSourceText(sources),
                listPrerequisites(rootNode),
                null, // Level coming someday..
                SkillOrAbility.getAbilityScoreIncreases(FeatFields.ability.getFrom(rootNode)),
                images,
                String.join("\n", text),
                tags);
    }

    enum FeatFields implements JsonNodeReader {
        ability,
        additionalSpells,
        category,
        ;
    }
}
