package dev.ebullient.json5e.tools5e;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.json5e.qute.QuteBackground;
import dev.ebullient.json5e.qute.QuteSource;

public class Json2QuteBackground extends Json2QuteCommon {

    public static Set<String> traits = new HashSet<>();
    public static Set<String> ideals = new HashSet<>();
    public static Set<String> bonds = new HashSet<>();
    public static Set<String> flaws = new HashSet<>();

    Json2QuteBackground(JsonIndex index, IndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    public QuteSource build() {
        String backgroundName = decoratedBackgroundName(sources.getName());
        List<String> tags = new ArrayList<>(sources.getSourceTags());

        List<String> text = new ArrayList<>();
        appendEntryToText(text, node, "##");

        List<String> fluff = getFluff(IndexType.backgroundfluff, "##");
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

        return new QuteBackground(
                decoratedTypeName(backgroundName, sources),
                sources.getSourceText(index.srdOnly()),
                String.join("\n", text),
                tags);
    }

    String decoratedBackgroundName(String name) {
        if (name.startsWith("Variant")) {
            name = name.replace("Variant ", "") + " (Variant)";
        }
        return name;
    }

}
