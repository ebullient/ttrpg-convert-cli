package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.tools.dnd5e.qute.QuteBackground;

public class Json2QuteBackground extends Json2QuteCommon {

    public static final Set<String> traits = new HashSet<>();
    public static final Set<String> ideals = new HashSet<>();
    public static final Set<String> bonds = new HashSet<>();
    public static final Set<String> flaws = new HashSet<>();

    final String backgroundName;

    Json2QuteBackground(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
        backgroundName = decoratedTypeName(decoratedBackgroundName(sources.getName()), sources);
    }

    @Override
    protected QuteBase buildQuteResource() {
        List<String> tags = new ArrayList<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();

        appendEntryToText(text, node, "##");

        List<ImageRef> images = new ArrayList<>();
        List<String> fluff = getFluff(Tools5eIndexType.backgroundfluff, "##", images);

        if (fluff != null) {
            boolean found = false;
            int max = Math.min(text.size(), 10);
            for (int i = 0; i < max; i++) {
                if (!found && text.get(i).startsWith("- **")) {
                    found = true;
                } else if (found && !text.get(i).startsWith("- **")) {
                    // first blank line after leading list
                    text.addAll(i, fluff);
                    text.add(i, "");
                    break;
                }
            }
        }

        if (text.isEmpty()) {
            return null;
        }
        appendFootnotes(text, 0);

        return new QuteBackground(sources,
                backgroundName,
                sources.getSourceText(index.srdOnly()),
                String.join("\n", text),
                images,
                tags);
    }

    public static String decoratedBackgroundName(String name) {
        if (name.startsWith("Variant")) {
            name = name.replace("Variant ", "") + " (Variant)";
        }
        return name;
    }

}
