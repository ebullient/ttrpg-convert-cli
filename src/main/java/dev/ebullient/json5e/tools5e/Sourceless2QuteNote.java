package dev.ebullient.json5e.tools5e;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.json5e.qute.QuteNote;

public class Sourceless2QuteNote extends Json2QuteCommon {

    String title;
    CompendiumSources currentSource;

    Sourceless2QuteNote(JsonIndex index, JsonNode jsonNode, String title) {
        super(index, IndexType.sourceless, jsonNode);
        this.title = title;
    }

    @Override
    public String getName() {
        return title;
    }

    @Override
    public CompendiumSources getSources() {
        return currentSource;
    }

    @Override
    public QuteNote build() {
        Set<String> tags = new HashSet<>();
        List<String> text = new ArrayList<>();

        node.forEach(entry -> appendElement(entry, text, tags));
        if (text.isEmpty()) {
            return null;
        }

        return new QuteNote(title, null, String.join("\n", text), tags);
    }

    private void appendElement(JsonNode entry, List<String> text, Set<String> tags) {
        currentSource = index.constructSources(IndexType.sourceless, entry);
        if (index.keyIsIncluded(currentSource.key)) {
            tags.addAll(currentSource.getSourceTags());
            maybeAddBlankLine(text);
            text.add("## " + replaceText(entry.get("name").asText()));
            maybeAddBlankLine(text);
            appendEntryToText(text, entry, "###");
            maybeAddBlankLine(text);
            text.add(String.format("_Source: %s_", currentSource.getSourceText(index.srdOnly())));
        }
    }

    public QuteNote buildLoot() {
        Set<String> tags = new HashSet<>();
        List<String> text = new ArrayList<>();

        node.forEach(entry -> appendLootElement(entry, text, tags));
        if (text.isEmpty()) {
            return null;
        }

        return new QuteNote(title, null, String.join("\n", text), tags);
    }

    private void appendLootElement(JsonNode entry, List<String> text, Set<String> tags) {
        currentSource = index.constructSources(IndexType.sourceless, entry);
        if (index.keyIsIncluded(currentSource.key)) {
            String name = entry.get("name").asText();
            String blockid = slugify(name);
            maybeAddBlankLine(text);
            text.add("## " + name);
            text.add("");
            text.add(String.format("`dice: [](%s.md#^%s)`", slugify(title), blockid));
            text.add("");

            ArrayNode table = entry.withArray("table");
            if (table.get(0).isTextual()) {
                String header = "| " + name + " |";
                text.add(header);
                text.add(header.replaceAll("[^|]", "-"));
                table.forEach(x -> text.add("| " + replaceText(x.asText()) + " |"));
            } else {
                String header = "| dice: d100 | " + name + " |";
                text.add(header);
                text.add(header.replaceAll("[^|]", "-"));
                table.forEach(r -> {
                    String item;
                    if (r.has("item")) {
                        item = replaceText(r.get("item").asText());
                    } else if (r.has("choose")) {
                        item = "Choose: " + chooseFrom(r.get("choose"));
                    } else {
                        throw new IllegalArgumentException("What kind of item? " + r.toPrettyString());
                    }

                    if (r.has("min") && r.has("max")) {
                        int min = r.get("min").asInt();
                        int max = r.get("max").asInt();
                        if (min == max) {
                            text.add(String.format("| %s | %s |", min, item));
                        } else {
                            text.add(String.format("| %s-%s | %s |", min, max, item));
                        }
                    }
                });
            }
            text.add("^" + blockid);
            maybeAddBlankLine(text);
            text.add(String.format("_Source: %s_", currentSource.getSourceText(index.srdOnly())));
        }
    }

    String chooseFrom(JsonNode choose) {
        if (choose.has("fromGroup")) {
            return joinAndReplace(choose.withArray("fromGroup"));
        } else if (choose.has("fromGeneric")) {
            return joinAndReplace(choose.withArray("fromGeneric"));
        }
        throw new IllegalArgumentException("What kind of item to choose? " + choose.toPrettyString());
    }
}
