package dev.ebullient.convert.tools.dnd5e;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.QuteObject;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QuteObject extends Json2QuteMonster {

    Json2QuteObject(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {
        String size = getSize(rootNode);
        String objectType = findObjectType();

        Tags tags = new Tags(getSources());
        tags.add("object", "size", size);
        tags.add("object", "type", objectType);
        if (creatureType != null) {
            tags.add("object", "type", creatureType);
        }

        List<ImageRef> fluffImages = new ArrayList<>();
        List<String> text = getFluff(Tools5eIndexType.monsterFluff, "##", fluffImages);
        appendToText(text, SourceField.entries.getFrom(rootNode), "##");

        return new QuteObject(sources,
                getSources().getName(),
                getSourceText(sources),
                isNpc, size,
                creatureType, objectType,
                acHp,
                speed(Tools5eFields.speed.getFrom(rootNode)),
                abilityScores(),
                joinAndReplace(rootNode, "senses"),
                immuneResist(),
                collectTraits("actionEntries"),
                getToken(), fluffImages,
                String.join("\n", text),
                tags);
    }

    private String findObjectType() {
        String type = ObjectFields.objectType.getTextOrEmpty(rootNode);
        return switch (type) {
            case "G", "GEN" -> "Generic";
            case "SW" -> "Siege weapon";
            case "U" -> "Unknown";
            default -> type;
        };
    }

    @Override
    Path getTokenSourcePath(String filename) {
        return Path.of("img", "objects", "tokens",
                getSources().mapPrimarySource(),
                filename + ".png");
    }

    enum ObjectFields implements JsonNodeReader {
        objectType
    }
}
