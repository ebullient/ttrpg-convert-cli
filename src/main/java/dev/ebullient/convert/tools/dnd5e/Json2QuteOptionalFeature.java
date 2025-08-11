package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.ImageRef;
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
        List<String> typeList = Tools5eFields.featureType.getListOfStrings(rootNode, tui());
        for (String featureType : typeList) {
            tags.add("optional-feature", featureType);
        }

        String featureTypeFull = featureTypeToFull(typeList.get(0));
        if (featureTypeFull.startsWith("Fighting Style")) {
            featureTypeFull = "Fighting Style"; //trim class name, fighting styles can be for multiple classes
        } else if (featureTypeFull.equalsIgnoreCase("Maneuver, Battle Master")) {
            featureTypeFull = "Battle Master Maneuver";
        } else if (featureTypeFull.equalsIgnoreCase("Maneuver, Cavalier V2 (UA)")) {
            featureTypeFull = "Cavalier Maneuver, V2 (UA)";
        }

        List<ImageRef> images = new ArrayList<>();
        List<String> text = getFluff(Tools5eIndexType.optionalfeatureFluff, "##", images);
        appendToText(text, SourceField.entries.getFrom(rootNode), "##");

        return new QuteFeat(getSources(),
                getSources().getName(),
                getSourceText(sources),
                listPrerequisites(rootNode),
                null,
                null,
                featureTypeFull,
                images,
                String.join("\n", text),
                tags);
    }
}
