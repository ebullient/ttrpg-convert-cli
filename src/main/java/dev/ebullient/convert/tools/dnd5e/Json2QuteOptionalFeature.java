package dev.ebullient.convert.tools.dnd5e;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.QuteFeat;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QuteOptionalFeature extends Json2QuteCommon {
    public Json2QuteOptionalFeature(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {
        Tags tags = new Tags(getSources());

        for (String featureType : Tools5eFields.featureType.getListOfStrings(rootNode, tui())) {
            tags.add("optional-feature", featureType);
        }

        return new QuteFeat(getSources(),
                getSources().getName(),
                getSourceText(sources),
                listPrerequisites(),
                null,
                getText("##"),
                tags) {
            @Override
            public String template() {
                return "feat2md.txt";
            }
        };
    }
}
