package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.QuteCharoption;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QuteCharoption extends Json2QuteCommon {

    Json2QuteCharoption(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {
        Tags tags = new Tags(getSources());

        List<String> optionTypes = new ArrayList<>();
        String preText = null;
        for (String optionType : CharoptionFields.optionType.getListOfStrings(rootNode, tui())) {
            tags.add("charoption", optionType);
            optionTypes.add(getOptionTypeFull(optionType));
            if (preText == null) {
                preText = getOptionTypePreText(optionType);
            }
        }

        List<ImageRef> images = new ArrayList<>();
        List<String> text = new ArrayList<>();
        if (preText != null) {
            text.add(preText);
            maybeAddBlankLine(text);
        }
        appendToText(text, SourceField.entries.getFrom(rootNode), "##");
        text.addAll(getFluff(Tools5eIndexType.charoptionFluff, "##", images));

        return new QuteCharoption(getSources(),
                getName(),
                getSourceText(getSources()),
                listPrerequisites(rootNode),
                String.join(", ", optionTypes),
                images,
                String.join("\n", text),
                tags);
    }

    enum CharoptionFields implements JsonNodeReader {
        optionType
    }

    String getOptionTypeFull(String key) {
        return switch (key) {
            case "SG" -> "Supernatural Gift";
            case "OF" -> "Optional Feature";
            case "DG" -> "Dark Gift";
            case "RF:B" -> "Replacement Feature: Background";
            case "CS" -> "Character Secret";
            case "PTH" -> "Path";
            default -> key;
        };
    }

    String getOptionTypePreText(String key) {
        return switch (key) {
            case "RF:B" -> "You may replace the standard feature of your background with this feature.";
            case "CS" -> "See the character secrets section for more information.";
            default -> null;
        };
    }
}
