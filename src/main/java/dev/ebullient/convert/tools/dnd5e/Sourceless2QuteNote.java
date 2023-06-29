package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.qute.QuteNote;
import dev.ebullient.convert.tools.dnd5e.qute.QuteSource;

public class Sourceless2QuteNote extends Json2QuteCommon {

    final String title;
    Tools5eSources currentSource;
    String imagePath = null;

    static Tools5eIndexType getType(JsonNode jsonNode) {
        if (jsonNode.has("source")) {
            return Tools5eIndexType.note;
        }
        return Tools5eIndexType.syntheticGroup;
    }

    Sourceless2QuteNote(Tools5eIndex index, JsonNode jsonNode, String title) {
        super(index, getType(jsonNode), jsonNode);
        this.title = title;
    }

    @Override
    public String getName() {
        return title;
    }

    @Override
    public Tools5eSources getSources() {
        return currentSource;
    }

    @Override
    public String getImagePath() {
        if (imagePath != null) {
            return imagePath;
        }
        String key = getSources().getKey();
        if (key.contains("adventure-")) {
            return QuteSource.ADVENTURE_PATH + "/" + slugify(title);
        } else if (key.contains("book-")) {
            return QuteSource.BOOK_PATH + "/" + slugify(title);
        }
        return super.getImagePath();
    }

    @Override
    protected QuteNote buildQuteNote() {
        Set<String> tags = new HashSet<>();
        List<String> text = new ArrayList<>();

        for (JsonNode entry : iterableElements(node)) {
            appendElement(entry, text, tags);
        }
        if (text.isEmpty()) {
            return null;
        }

        return new QuteNote(title, null, text, tags);
    }

    private void appendElement(JsonNode entry, List<String> text, Set<String> tags) {
        Tools5eSources tempSources = Tools5eSources.findOrTemporary(entry);
        String name = entry.get("name").asText();
        if (!index.rulesSourceExcluded(entry, name)) {
            tags.addAll(tempSources.getSourceTags());
            maybeAddBlankLine(text);
            text.add("## " + replaceText(name));
            text.add(index().getSourceText(tempSources));
            text.add("");
            appendEntryToText(text, entry, "###");
            maybeAddBlankLine(text);
        }
    }

    public QuteNote buildVariant() {
        if (index().rulesSourceExcluded(node, title)) {
            return null;
        }
        currentSource = sources;
        imagePath = "variant-rules";
        boolean pushed = parseState.push(currentSource, node);
        try {
            Set<String> tags = new TreeSet<>(sources.getSourceTags());
            return new QuteNote(title, sources.getSourceText(index.srdOnly()),
                    getText("##"), tags);
        } catch (Exception e) {
            tui().errorf(e, "Error processing variant '%s': %s", title, e.toString());
            throw e;
        } finally {
            parseState.pop(pushed);
            imagePath = null;
        }
    }

    public Map<String, QuteNote> buildReference(JsonNode data) {
        if (index().rulesSourceExcluded(node, title)) {
            return Map.of();
        }
        Set<String> tags = new TreeSet<>(sources.getSourceTags());

        Map<String, QuteNote> notes = new HashMap<>();

        AtomicInteger prefix = new AtomicInteger(1);
        String pFormat;
        if (data.size() + 1 > 10) {
            pFormat = "%02d";
        } else {
            pFormat = "%01d";
        }

        currentSource = sources;
        boolean p1 = parseState.push(currentSource, node); // outer node
        try {
            for (JsonNode x : iterableElements(data)) {
                boolean p2 = parseState.push(x); // inner node
                try {
                    List<String> text = new ArrayList<>();
                    appendEntryToText(text, x.get("entries"), "##");
                    String content = String.join("\n", text);
                    if (!content.isBlank()) {
                        String titlePage = title;
                        if (x.has("page")) {
                            String page = x.get("page").asText();
                            titlePage = title + ", p. " + page;
                        }
                        String name = getTextOrDefault(x, "name", "");
                        QuteNote note = new QuteNote(name, titlePage, content, tags);
                        notes.put(String.format("%s/%s-%s.md",
                                slugify(title),
                                String.format(pFormat, prefix.get()),
                                slugify(name)),
                                note);
                        prefix.incrementAndGet();
                    }
                } finally {
                    parseState.pop(p2);
                }
            }
        } finally {
            parseState.pop(p1);
        }

        return notes;
    }

    public QuteNote buildItemProperties() {
        boolean p1 = parseState.push(node);
        try {
            Set<String> tags = new HashSet<>();
            tags.add("compendium/src/srd");

            List<String> text = new ArrayList<>();
            for (JsonNode entry : iterableElements(node)) {
                boolean p2 = parseState.push(entry);
                try {
                    String name = entry.get("name").asText();
                    if (index().rulesSourceExcluded(entry, name)) {
                        continue;
                    }
                    maybeAddBlankLine(text);
                    text.add("## " + name);
                    if (!entry.has("srd")) {
                        text.add(index().getSourceText(entry));
                    }
                    text.add("");
                    appendEntryToText(text, entry.get("entries"), "###");
                } finally {
                    parseState.pop(p2);
                }
            }

            return new QuteNote(title, "_Source: SRD / Basic Rules_", text, tags);
        } finally {
            parseState.pop(p1);
        }
    }

    public QuteNote buildLoot() {
        boolean pushed = parseState.push(node);
        try {
            Set<String> tags = new HashSet<>();
            List<String> text = new ArrayList<>();

            node.forEach(entry -> appendLootElement(entry, text));
            if (text.isEmpty()) {
                return null;
            }

            return new QuteNote(title, null, text, tags);
        } finally {
            parseState.pop(pushed);
        }
    }

    private void appendLootElement(JsonNode entry, List<String> text) {
        Tools5eSources tmpSources = Tools5eSources.findOrTemporary(entry);

        String name = entry.get("name").asText();
        if (index.rulesSourceExcluded(entry, name)) {
            return;
        }

        boolean pushed = parseState.push(entry);
        try {
            String blockid = slugify(name);
            maybeAddBlankLine(text);
            text.add("## " + name);
            text.add(index().getSourceText(tmpSources));
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
        } finally {
            parseState.pop(pushed);
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
        boolean pushed = parseState.push(node);
        try {
            Set<String> tags = new HashSet<>();
            List<String> text = new ArrayList<>();
            Map<String, String> actionDuration = new HashMap<>();

            node.forEach(entry -> appendAction(entry, text, actionDuration, tags));
            if (text.isEmpty()) {
                return null;
            }
            return new QuteNote(title, "_Source: SRD / Basic Rules_", text, tags);
        } finally {
            parseState.pop(pushed);
        }
    }

    private void appendAction(JsonNode entry, List<String> text, Map<String, String> actionDuration, Set<String> tags) {
        String name = entry.get("name").asText();

        if (index.rulesSourceExcluded(entry, name)) {
            tui().debugf("Action %s (excluded)", name);
            return;
        }

        boolean pushed = parseState.push(entry);
        try {
            String revisedName = replaceText(name);
            String duration = flattenActionTime(entry.get("time"));

            Tools5eSources tmpSources = Tools5eSources.findOrTemporary(entry);

            maybeAddBlankLine(text);
            text.add("## " + revisedName);
            text.add(index().getSourceText(tmpSources));
            text.add("");

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
            tags.addAll(tmpSources.getSourceTags());
        } finally {
            parseState.pop(pushed);
        }
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
            return variant + " from " + TtrpgConfig.sourceToLongName(parts[1]);
        } else {
            return String.format("[%s](%svariant-rules/%s.md)",
                    variant, index().rulesVaultRoot(), slugify(variant));
        }
    }
}
