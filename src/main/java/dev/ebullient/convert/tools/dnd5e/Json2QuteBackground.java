package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.QuteBackground;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QuteBackground extends Json2QuteCommon {

    public static final Set<String> traits = new HashSet<>();
    public static final Set<String> ideals = new HashSet<>();
    public static final Set<String> bonds = new HashSet<>();
    public static final Set<String> flaws = new HashSet<>();

    final String backgroundName;

    Json2QuteBackground(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
        backgroundName = type.decoratedName(jsonNode);
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {
        Tags tags = new Tags(getSources());
        tags.add("background");

        List<String> text = new ArrayList<>();
        appendToText(text, rootNode, "##");

        List<ImageRef> images = new ArrayList<>();
        List<String> fluff = getFluff(Tools5eIndexType.backgroundFluff, "##", images);

        if (fluff != null) {
            boolean found = false;
            for (int i = 0; i < text.size(); i++) {
                if (text.get(i).startsWith("##")) {
                    found = true;
                    text.add(i, "");
                    text.addAll(i, fluff);
                    break;
                }
            }
            if (!found) {
                maybeAddBlankLine(text);
                text.addAll(fluff);
            }
        }
        return new QuteBackground(sources,
                backgroundName,
                getSourceText(sources),
                listPrerequisites(),
                String.join("\n", text),
                images,
                tags);
    }
}
