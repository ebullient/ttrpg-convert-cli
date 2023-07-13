package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.QuteHazard;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QuteHazard extends Json2QuteCommon {

    public Json2QuteHazard(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {
        Tags tags = new Tags(getSources());
        List<String> text = new ArrayList<>();

        String hazardType = TrapHazFields.trapHazType.getTextOrNull(rootNode);
        if (hazardType != null) {
            tags.add("hazard", slugify(hazardType));
        }

        appendToText(text, rootNode.get("entries"), "##");

        return new QuteHazard(getSources(),
                getSources().getName(),
                getSourceText(getSources()),
                getHazardType(hazardType),
                String.join("\n", text),
                tags);
    }

    enum TrapHazFields implements JsonNodeReader {
        trapHazType
    }

    String getHazardType(String key) {
        if (key == null) {
            return "Generic Hazard";
        }
        return switch (key) {
            case "ENV" -> "Environmental Hazard";
            case "EST" -> "Eldritch Storm";
            case "MAG" -> "Magical Trap";
            case "MECH" -> "Mechanical Trap";
            case "WTH" -> "Weather";
            case "WLD" -> "Wilderness Hazard";
            default -> "Generic Hazard"; // also "GEN"
        };
    }

}
