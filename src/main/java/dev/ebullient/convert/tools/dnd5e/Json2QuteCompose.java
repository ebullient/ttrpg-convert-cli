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
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.ToolsIndex;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteNote;

public class Json2QuteCompose extends Json2QuteCommon {
    final List<JsonNode> nodes = new ArrayList<>();
    Tools5eSources currentSources;
    final String title;
    final String targetPath;

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

        Tags tags = new Tags();
        List<String> text = new ArrayList<>();

        if (type == Tools5eIndexType.itemType || type == Tools5eIndexType.itemProperty) {
            nodes.forEach(x -> flatten(x));
        }

        nodes.sort(Comparator.comparing(SourceField.name::getTextOrEmpty));

        if (type == Tools5eIndexType.itemProperty) {
            appendItemProperties(text, tags);
        } else {
            for (JsonNode entry : nodes) {
                appendElement(entry, text, tags);
            }
        }

        return new Tools5eQuteNote(title, null, text, tags)
                .withTargetPath(targetPath);
    }

    private void appendElement(JsonNode entry, List<String> text, Tags tags) {
        currentSources = Tools5eSources.findOrTemporary(entry);

        boolean pushed = parseState().push(entry);
        try {
            String abbreviation = Tools5eFields.abbreviation.getTextOrNull(entry);
            String name = SourceField.name.replaceTextFrom(entry, index);

            tags.addSourceTags(currentSources);

            maybeAddBlankLine(text);
            text.add("## " + name);
            text.add(getLabeledSource(entry));

            if (type == Tools5eIndexType.action) {
                appendAction(entry, text);
            } else if (entry.has("table")) {
                appendTable(name, entry, text);
            } else {
                appendToText(text, entry, "###");
            }

            if (type == Tools5eIndexType.itemType && abbreviation != null) {
                List<JsonNode> more = index.elementsMatching(Tools5eIndexType.itemTypeAdditionalEntries, abbreviation);
                for (JsonNode m : more) {
                    appendToText(text, m, "###");
                }
            }
        } finally {
            parseState().pop(pushed);
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

        String duration = flattenActionTime(ComposedTypeFields.time.getFrom(entry));
        if (!duration.isEmpty()) {
            maybeAddBlankLine(text);
            text.add("- **Duration**: " + duration);
        }

        maybeAddBlankLine(text);
        appendToText(text, entry, "###");

        List<String> seeAlso = ComposedTypeFields.seeAlsoAction.linkifyListFrom(entry, Tools5eIndexType.action, index);
        if (!seeAlso.isEmpty()) {
            maybeAddBlankLine(text);
            text.add("See also: " + String.join(", ", seeAlso));
        }

        String fromVariant = ComposedTypeFields.fromVariant.getTextOrNull(entry);
        if (fromVariant != null) {
            maybeAddBlankLine(text);
            text.add("This action is an optional addition to the game, from the optional/variant rule "
                    + linkifyVariant(fromVariant) + ".");

        }
    }

    private String flattenActionTime(JsonNode entry) {
        if (entry == null || entry.isNull()) {
            return "";
        } else if (entry.isTextual()) {
            return entry.asText();
        } else if (entry.isObject()) {
            return String.format("%s %s", ComposedTypeFields.number.replaceTextFrom(entry, index),
                    ComposedTypeFields.unit.replaceTextFrom(entry, index));
        } else {
            List<String> elements = new ArrayList<>();
            entry.forEach(x -> elements.add(flattenActionTime(x)));
            return String.join(", ", elements);
        }
    }

    private void appendItemProperties(List<String> text, Tags tags) {
        final JsonNode srdEntries = TtrpgConfig.activeGlobalConfig("srdEntries").get("properties");

        for (JsonNode srdEntry : iterableElements(srdEntries)) {
            currentSources = Tools5eSources.findOrTemporary(srdEntry);
            boolean p2 = parseState().push(srdEntry);
            try {
                String name = srdEntry.get("name").asText();

                maybeAddBlankLine(text);
                text.add("## " + name);
                if (!srdEntry.has("srd")) {
                    text.add(getLabeledSource(srdEntry));
                }
                text.add("");

                if (name.equals("Weapon Properties")) {
                    ArrayNode propertyEntries = srdEntry.withArray("entries");
                    Set<String> abbreviations = new HashSet<>();
                    List<JsonNode> properties = new ArrayList<>();
                    for (JsonNode x : iterableElements(propertyEntries)) {
                        String abbr = Tools5eFields.abbreviation.getTextOrDefault(x, SourceField.name.getTextOrEmpty(x))
                                .toLowerCase();
                        if (propertyIncluded(x, abbr)) {
                            properties.add(x);
                            abbreviations.add(abbr);
                        }
                    }
                    for (JsonNode x : nodes) {
                        String abbr = Tools5eFields.abbreviation.getTextOrDefault(x, SourceField.name.getTextOrEmpty(x))
                                .toLowerCase();
                        if (propertyIncluded(x, abbr) && !abbreviations.contains(abbr)) {
                            properties.add(x);
                        }
                    }
                    properties.sort(Comparator.comparing(SourceField.name::getTextOrEmpty));
                    propertyEntries.removeAll();
                    propertyEntries.addAll(properties);
                    appendToText(text, propertyEntries, "###");
                } else {
                    appendToText(text, srdEntry.get("entries"), "###");
                }
            } finally {
                parseState().pop(p2);
            }
        }
    }

    private boolean propertyIncluded(JsonNode x, String abbr) {
        String source = SourceField.source.getTextOrEmpty(x);
        return booleanOrDefault(x, "srd", false)
                || (!source.isEmpty() && index.sourceIncluded(source));
    }

    enum ComposedTypeFields implements JsonNodeReader {
        fromGeneric,
        fromGroup,
        fromItems,
        fromVariant,
        number,
        seeAlsoAction,
        time,
        unit
    }
}
