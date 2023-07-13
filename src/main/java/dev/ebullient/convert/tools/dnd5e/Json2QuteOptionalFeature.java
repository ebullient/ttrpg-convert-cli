package dev.ebullient.convert.tools.dnd5e;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QuteOptionalFeature extends Json2QuteCommon {
    public Json2QuteOptionalFeature(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {
        Tags tags = new Tags(getSources());

        for (String featureType : Tools5eFields.featureType.getListOfStrings(rootNode, tui())) {
            tags.add("optional-feature", slugify(featureType));
        }

        return new Tools5eQuteBase(getSources(),
                getSources().getName(),
                getSourceText(sources),
                getText("##"),
                tags) {
            @Override
            public String template() {
                return "note2md.txt";
            }
        };
    }
}
