package dev.ebullient.json5e.tools5e;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import dev.ebullient.json5e.qute.QuteNote;
import dev.ebullient.json5e.qute.QuteSource;

public class Sourceless2QuteNote extends Json2QuteCommon {

    final String title;
    CompendiumSources currentSource;

    static IndexType getType(JsonNode jsonNode) {
        if (jsonNode.has("source")) {
            return IndexType.note;
        }
        return IndexType.sourceless;
    }

    Sourceless2QuteNote(JsonIndex index, JsonNode jsonNode, String title) {
        super(index, getType(jsonNode), jsonNode);
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
    public QuteSource build() {
        throw new IllegalStateException("Not implemented");
    }

    public QuteNote buildNote() {
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
        String name = entry.get("name").asText();
        if (!index.rulesSourceExcluded(entry, name)) {
            tags.addAll(currentSource.getSourceTags());
            maybeAddBlankLine(text);
            text.add("## " + replaceText(name));
            maybeAddBlankLine(text);
            appendEntryToText(text, entry, "###");
            maybeAddBlankLine(text);
            text.add(String.format("_Source: %s_", currentSource.getSourceText(index.srdOnly())));
        }
    }

    public QuteNote buildVariant() {
        currentSource = sources;
        List<String> tags = new ArrayList<>(sources.getSourceTags());

        return new QuteNote(title,
                sources.getSourceText(index.srdOnly()),
                getText("##"), tags);
    }

    public Map<String, QuteNote> buildReference(JsonNode data) {
        currentSource = sources;
        if (index().rulesSourceExcluded(node, title)) {
            return Map.of();
        }
        List<String> tags = new ArrayList<>(sources.getSourceTags());

        Map<String, QuteNote> notes = new HashMap<>();

        AtomicInteger prefix = new AtomicInteger(1);
        String pFormat;
        if (data.size() + 1 > 10) {
            pFormat = "%02d";
        } else {
            pFormat = "%01d";
        }

        data.forEach(x -> {
            List<String> text = new ArrayList<>();
            appendEntryToText(text, x.get("entries"), "##");
            String content = String.join("\n", text);
            if (!content.isBlank()) {
                String titlePage = title;
                if (x.has("page")) {
                    String page = x.get("page").asText();
                    titlePage = title + ", p. " + page;
                }
                String name = x.get("name").asText();
                QuteNote note = new QuteNote(name, titlePage, content, tags);
                notes.put(String.format("%s/%s-%s.md",
                        slugify(title),
                        String.format(pFormat, prefix.get()),
                        slugify(name)),
                        note);
                prefix.incrementAndGet();
            }
        });

        return notes;

    }

    public QuteNote buildItemProperties() {
        Set<String> tags = new HashSet<>();
        List<String> text = new ArrayList<>();

        node.forEach(entry -> {
            PropertyEnum propertyEnum = PropertyEnum.fromEncodedType(entry.get("abbreviation").asText());
            ObjectNode node = (ObjectNode) copyNode(entry);
            node.set("name", new TextNode(propertyEnum.longName));
            currentSource = index.constructSources(IndexType.sourceless, node);
            if (!index.rulesSourceExcluded(node, propertyEnum.longName)) {
                tags.addAll(currentSource.getSourceTags());
                maybeAddBlankLine(text);
                text.add("## " + propertyEnum.longName);
                maybeAddBlankLine(text);
                entry.withArray("entries").forEach(e -> appendEntryToText(text, e.get("entries"), null));
                if (propertyEnum == PropertyEnum.SPECIAL) {
                    text.add(
                            "A weapon with the special property has unusual rules governing its use, which are explained in the weapon's description.");
                }
                maybeAddBlankLine(text);
                text.add(String.format("_Source: %s_", currentSource.getSourceText(index.srdOnly())));
            }
        });

        return new QuteNote(title, null, String.join("\n", text), tags);
    }

    public QuteNote buildLoot() {
        Set<String> tags = new HashSet<>();
        List<String> text = new ArrayList<>();

        node.forEach(entry -> appendLootElement(entry, text));
        if (text.isEmpty()) {
            return null;
        }

        return new QuteNote(title, null, String.join("\n", text), tags);
    }

    private void appendLootElement(JsonNode entry, List<String> text) {
        currentSource = index.constructSources(IndexType.sourceless, entry);
        String name = entry.get("name").asText();
        if (!index.rulesSourceExcluded(entry, name)) {
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

    public QuteNote buildActions() {
        Set<String> tags = new HashSet<>();
        List<String> text = new ArrayList<>();
        Map<String, String> actionDuration = new HashMap<>();

        node.forEach(entry -> appendAction(entry, text, actionDuration, tags));
        if (text.isEmpty()) {
            return null;
        }

        return new QuteNote(title, null, String.join("\n", text), tags);
    }

    private void appendAction(JsonNode entry, List<String> text, Map<String, String> actionDuration, Set<String> tags) {
        currentSource = index.constructSources(IndexType.sourceless, entry);
        String name = entry.get("name").asText();

        if (index.rulesSourceExcluded(entry, name)) {
            tui().debugf("Skilling action %s (excluded)", name);
            return;
        }

        String revisedName = replaceText(name);
        String duration = flattenActionTime(entry.get("time"));

        maybeAddBlankLine(text);
        text.add("## " + revisedName);

        if (!duration.isEmpty()) {
            actionDuration.put(revisedName, duration);
            maybeAddBlankLine(text);
            text.add("- **Duration:** " + duration);
        }

        maybeAddBlankLine(text);
        appendEntryToText(text, entry, "###");

        if (node.has("fromVariant")) {
            maybeAddBlankLine(text);
            text.add("This action is an optional addition to the game, from the optional/variant rule "
                    + linkifyVariant(node.get("fromVariant").asText()) + ".");
        }

        maybeAddBlankLine(text);
        text.add(String.format("_Source: %s_", currentSource.getSourceText(index.srdOnly())));
        tags.addAll(currentSource.getSourceTags());
    }

    private String flattenActionTime(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        } else if (node.isTextual()) {
            return node.asText();
        } else if (node.isObject()) {
            return String.format("%s %s", node.get("number").asText(), node.get("unit").asText());
        } else {
            List<String> elements = new ArrayList<>();
            node.forEach(x -> elements.add(flattenActionTime(x)));
            return String.join(", ", elements);
        }
    }

    private String linkifyVariant(String variant) {
        // "fromVariant": "Action Options",
        // "fromVariant": "Spellcasting|XGE",
        String[] parts = variant.trim().split("\\|");
        if (parts.length > 1 && !index().sourceIncluded(parts[1])) {
            return variant + " from " + CompendiumSources.sourceToLongName(parts[1]);
        } else {
            return String.format("[%s](%svariant-rules/%s.md)",
                    variant, index().rulesRoot(), slugify(variant));
        }
    }
}
