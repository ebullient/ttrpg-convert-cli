package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.tools.ToolsIndex;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteNote;

public class Json2QuteCompose extends Json2QuteCommon {
    List<JsonNode> nodes = new ArrayList<>();
    Tools5eSources currentSources;
    String title;
    String targetPath;

    public Json2QuteCompose(Tools5eIndexType type, Tools5eIndex index, String title) {
        this(type, index, title, ".");
    }

    public Json2QuteCompose(Tools5eIndexType type, Tools5eIndex index, String title, String targetPath) {
        super(index, type, null);
        this.title = title;
        this.targetPath = targetPath;
    }

    public void add(JsonNode node) {
        String key = ToolsIndex.TtrpgValue.indexKey.getFromNode(node);
        if (index.isIncluded(key)) {
            nodes.add(node);
        } else {
            tui().debugf("%s: %s is excluded", type.name(), key);
        }
    }

    @Override
    public String getName() {
        return title;
    }

    @Override
    public Tools5eSources getSources() {
        return currentSources;
    }

    @Override
    protected Tools5eQuteNote buildQuteNote() {
        if (nodes.isEmpty()) {
            return null;
        }

        Set<String> tags = new HashSet<>();
        List<String> text = new ArrayList<>();

        if (type == Tools5eIndexType.itemType || type == Tools5eIndexType.itemProperty) {
            nodes.forEach(x -> flatten(x));
        }

        nodes.sort(Comparator.comparing(Fields.name::getTextOrEmpty));

        if (type == Tools5eIndexType.itemProperty) {
            appendItemProperties(text, tags);
        } else {
            int count = 0;
            for (JsonNode entry : nodes) {
                count = appendElement(entry, text, tags, count);
            }
        }

        return new Tools5eQuteNote(title, null, text, tags)
                .withTargetPath(targetPath);
    }

    private int appendElement(JsonNode entry, List<String> text, Set<String> tags, int count) {
        Tools5eSources tempSources = Tools5eSources.findOrTemporary(entry);

        boolean pushed = parseState.push(entry);
        try {
            String abbreviation = getTextOrEmpty(entry, "abbreviation");
            String name = replaceText(getTextOrEmpty(entry, "name"));

            tags.addAll(tempSources.getSourceTags());

            maybeAddBlankLine(text);
            text.add("## " + replaceText(name));
            text.add(getSourceText(tempSources));

            if (type == Tools5eIndexType.action) {
                appendAction(entry, text);
            } else if (entry.has("table")) {
                appendTable(name, entry, text);
            } else {
                appendEntryToText(text, entry, "###");
            }

            if (type == Tools5eIndexType.itemType && abbreviation != null) {
                List<JsonNode> more = index.elementsMatching(Tools5eIndexType.itemTypeAdditionalEntries, abbreviation);
                for (JsonNode m : more) {
                    appendEntryToText(text, m, "###");
                }
            }

            return appendFootnotes(text, count);
        } finally {
            parseState.pop(pushed);
        }
    }

    private void flatten(JsonNode entry) {
        if (!entry.has("name") && entry.has("entries")) {
            JsonNode nested = entry.get("entries").get(0);
            if (nested.has("name")) {
                ((ObjectNode) entry).set("name", nested.get("name"));
                ((ObjectNode) entry).set("entries", nested.get("entries"));
            }
        }
    }

    private void appendTable(String name, JsonNode entry, List<String> text) {
        String blockid = slugify(name);

        maybeAddBlankLine(text);
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
    }

    String chooseFrom(JsonNode choose) {
        // TODO: Replace generic/group lists
        if (choose.has("fromGroup")) {
            return joinAndReplace(choose.withArray("fromGroup"));
        } else if (choose.has("fromGeneric")) {
            return joinAndReplace(choose.withArray("fromGeneric"));
        } else if (choose.has("fromItems")) {
            // TODO: Another/following table!
            return joinAndReplace(choose.withArray("fromItems"));
        }
        throw new IllegalArgumentException("What kind of item to choose? " + choose.toPrettyString());
    }

    private void appendAction(JsonNode entry, List<String> text) {
        String duration = flattenActionTime(entry.get("time"));

        if (!duration.isEmpty()) {
            maybeAddBlankLine(text);
            text.add("- **Duration:** " + duration);
        }

        maybeAddBlankLine(text);
        appendEntryToText(text, entry, "###");

        if (entry.has("fromVariant")) {
            maybeAddBlankLine(text);
            text.add("This action is an optional addition to the game, from the optional/variant rule "
                    + linkifyVariant(entry.get("fromVariant").asText()) + ".");
        }
    }

    private String flattenActionTime(JsonNode entry) {
        if (entry == null || entry.isNull()) {
            return "";
        } else if (entry.isTextual()) {
            return entry.asText();
        } else if (entry.isObject()) {
            return String.format("%s %s", entry.get("number").asText(), entry.get("unit").asText());
        } else {
            List<String> elements = new ArrayList<>();
            entry.forEach(x -> elements.add(flattenActionTime(x)));
            return String.join(", ", elements);
        }
    }

    private void appendItemProperties(List<String> text, Set<String> tags) {
        final JsonNode srdEntries = TtrpgConfig.activeGlobalConfig("srdEntries").get("properties");

        for (JsonNode srdEntry : iterableElements(srdEntries)) {
            boolean p2 = parseState.push(srdEntry);
            try {
                String name = srdEntry.get("name").asText();

                maybeAddBlankLine(text);
                text.add("## " + name);
                if (!srdEntry.has("srd")) {
                    text.add(index().getSourceText(srdEntry));
                }
                text.add("");

                if (name.equals("Weapon Properties")) {
                    ArrayNode propertyEntries = srdEntry.withArray("entries");
                    Set<String> abbreviations = new HashSet<>();
                    List<JsonNode> properties = new ArrayList<>();
                    for (JsonNode x : iterableElements(propertyEntries)) {
                        String abbr = getTextOrDefault(x, "abbreviation",
                                getTextOrEmpty(x, "name")).toLowerCase();

                        if (propertyIncluded(x, abbr)) {
                            properties.add(x);
                            abbreviations.add(abbr);
                        }
                    }
                    for (JsonNode x : nodes) {
                        String abbr = getTextOrDefault(x, "abbreviation",
                                getTextOrEmpty(x, "name")).toLowerCase();
                        if (propertyIncluded(x, abbr) && !abbreviations.contains(abbr)) {
                            properties.add(x);
                        }
                    }
                    properties.sort(Comparator.comparing(Fields.name::getTextOrEmpty));
                    propertyEntries.removeAll();
                    propertyEntries.addAll(properties);
                    appendEntryToText(text, propertyEntries, "###");
                } else {
                    appendEntryToText(text, srdEntry.get("entries"), "###");
                }
            } finally {
                parseState.pop(p2);
            }
        }
    }

    private boolean propertyIncluded(JsonNode x, String abbr) {
        String source = getTextOrEmpty(x, "source");
        return booleanOrDefault(x, "srd", false)
                || (!source.isEmpty() && index.sourceIncluded(source));
    }
}
