package dev.ebullient.convert.tools.dnd5e;

import com.fasterxml.jackson.databind.JsonNode;

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

        return new QuteFeat(sources,
                type.decoratedName(rootNode),
                getSourceText(sources),
                listPrerequisites(),
                null, // Level coming someday..
                getText("##"),
                tags);
    }
}
