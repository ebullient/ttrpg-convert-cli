package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.isPresent;

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

        List<ImageRef> images = new ArrayList<>();
        List<String> text = getFluff(Tools5eIndexType.optionalfeatureFluff, "##", images);
        appendToText(text, SourceField.entries.getFrom(rootNode), "##");

        return new QuteFeat(getSources(),
                getSources().getName(),
                getSourceText(sources),
                listPrerequisites(rootNode),
                null,
                null,
                featureTypeToFull(typeList.get(0)),
                images,
                String.join("\n", text),
                tags);
    }

    protected String featureTypeToFull(String featureType) {
        if (!isPresent(featureType)) {
            return "";
        }
        return switch (featureType.toUpperCase()) {
            case "AI" -> "Artificer Infusion";
            case "AS" -> "Arcane Shot";
            case "ED" -> "Elemental Discipline";
            case "EI" -> "Eldritch Invocation";
            case "FS:B", "FS:F", "FS:P", "FS:R" -> "Fighting Style";
            case "MM" -> "Metamagic";
            case "MV:B" -> "Battle Master Maneuver";
            case "PB" -> "Pact Boon";
            case "RN" -> "Rune Knight Rune";
            default -> featureType;
        };
    };
}
