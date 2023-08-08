package dev.ebullient.convert.tools.dnd5e;

import static java.util.Map.entry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.SourceAndPage;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.ParseState;
import dev.ebullient.convert.tools.dnd5e.Json2QuteClass.ClassFeature;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;
import io.quarkus.runtime.annotations.RegisterForReflection;

public interface JsonSource extends JsonTextReplacement {
    int CR_UNKNOWN = 100001;
    int CR_CUSTOM = 100000;

    default boolean textContains(List<String> haystack, String needle) {
        return haystack.stream().anyMatch(x -> x.contains(needle));
    }

    default String getTextOrEmpty(JsonNode x, String field) {
        if (x.has(field)) {
            return x.get(field).asText();
        }
        return "";
    }

    default String getTextOrDefault(JsonNode x, String field, String value) {
        if (x.has(field)) {
            return x.get(field).asText();
        }
        return value;
    }

    default boolean booleanOrDefault(JsonNode source, String key, boolean value) {
        JsonNode result = source.get(key);
        return result == null ? value : result.asBoolean(value);
    }

    default int intOrDefault(JsonNode source, String key, int value) {
        JsonNode result = source.get(key);
        return result == null ? value : result.asInt();
    }

    default JsonNode copyNode(JsonNode sourceNode) {
        try {
            return mapper().readTree(sourceNode.toString());
        } catch (JsonProcessingException ex) {
            tui().errorf(ex, "Unable to copy %s", sourceNode.toString());
            throw new IllegalStateException("JsonProcessingException processing " + sourceNode);
        }
    }

    default JsonNode copyReplaceNode(JsonNode sourceNode, Pattern replace, String with) {
        try {
            String modified = replace.matcher(sourceNode.toString()).replaceAll(with);
            return mapper().readTree(modified);
        } catch (JsonProcessingException ex) {
            tui().errorf(ex, "Unable to copy %s", sourceNode.toString());
            throw new IllegalStateException("JsonProcessingException processing " + sourceNode);
        }
    }

    default String getSourceText(JsonNode node) {
        return getSourceText(Tools5eSources.findOrTemporary(node));
    }

    default String getSourceText(ParseState parseState) {
        return parseState().sourcePageString("_Source: %s_");
    }

    default String getSourceText(Tools5eSources currentSource) {
        return String.format("_Source: %s_", currentSource.getSourceText(index().srdOnly()));
    }

    default ImageRef buildImageRef(Tools5eIndex index, Path sourcePath, Path target) {
        return getSources().buildImageRef(index, sourcePath, target, useCompendium());
    }

    default ImageRef buildImageRef(Tools5eIndex index, JsonMediaHref mediaHref, String imageBasePath) {
        return getSources().buildImageRef(index, mediaHref, imageBasePath, useCompendium());
    }

    /**
     * External (and recursive) entry point for content parsing.
     * Parse attributes of the given node and add resulting lines
     * to the provided list.
     *
     * @param text Parsed content is appended to this list
     * @param node Textual, Array, or Object node containing content to parse/render
     * @param heading The current header depth and/or if headings are allowed for this text element
     */
    @Override
    default void appendToText(List<String> text, JsonNode node, String heading) {
        boolean pushed = parseState().push(node); // store state
        try {
            if (node == null) {
                // do nothing
            } else if (node.isTextual()) {
                text.add(replaceText(node.asText()));
            } else if (node.isNumber()) {
                text.add(node.asText());
            } else if (node.isArray()) {
                for (JsonNode f : iterableElements(node)) {
                    maybeAddBlankLine(text);
                    appendToText(text, f, heading);
                }
            } else if (node.isObject()) {
                appendObjectToText(text, node, heading);
            } else {
                tui().errorf("Unknown entry type in %s: %s", getSources(), node.toPrettyString());
            }
        } finally {
            parseState().pop(pushed); // restore state
        }
    }

    default void appendObjectToText(List<String> text, JsonNode node, String heading) {
        AppendTypeValue type = AppendTypeValue.valueFrom(node, SourceField.type);
        String source = SourceField.source.getTextOrEmpty(node);

        // entriesOtherSource handled here.
        if (!source.isEmpty() && !cfg().sourceIncluded(source)) {
            if (!cfg().sourceIncluded(getSources().alternateSource())) {
                return;
            }
        }

        boolean pushed = parseState().push(node);
        try {
            if (type != null) {
                switch (type) {
                    case entries, section -> appendEntriesToText(text, node, heading);
                    case entry, item, itemSpell, itemSub -> appendEntryItem(text, node);
                    case abilityDc, abilityAttackMod, abilityGeneric -> appendAbility(type, text, node);
                    case flowchart -> appendFlowchart(text, node, heading);
                    case gallery -> appendGallery(text, node);
                    case homebrew -> appendCallout("note", "Homebrew", text, node);
                    case hr -> {
                        maybeAddBlankLine(text);
                        text.add("---");
                        text.add("");
                    }
                    case image -> appendImage(text, node);
                    case inline, inlineBlock -> {
                        List<String> inner = new ArrayList<>();
                        appendToText(inner, SourceField.entries.getFrom(node), null);
                        text.add(String.join("", inner));
                    }
                    case inset, insetReadaloud -> appendInset(text, node);
                    case link -> appendLink(text, node);
                    case list -> {
                        String style = Tools5eFields.style.getTextOrEmpty(node);
                        if ("list-no-bullets".equals(style)) {
                            appendToText(text, SourceField.items.getFrom(node), null);
                        } else {
                            appendList(text, (ArrayNode) SourceField.items.getFrom(node));
                        }
                    }
                    case optfeature -> appendOptionalFeature(text, node, heading);
                    case options -> appendOptions(text, node);
                    case quote -> appendQuote(text, node);
                    case refClassFeature -> appendClassFeatureRef(text, node, Tools5eIndexType.classfeature, "classFeature");
                    case refOptionalfeature -> appendOptionalFeatureRef(text, node);
                    case refSubclassFeature -> appendClassFeatureRef(text, node, Tools5eIndexType.subclassFeature,
                            "subclassFeature");
                    case statblock -> appendStatblock(text, node);
                    case table -> appendTable(text, node);
                    case tableGroup -> appendTableGroup(text, node, heading);
                    case variant -> appendCallout("danger", "Variant", text, node);
                    case variantInner, variantSub -> appendCallout("example", "Variant", text, node);
                    default -> tui().errorf("Unknown entry object type %s from %s: %s", type, getSources(),
                            node.toPrettyString());
                }
                // any entry/entries handled by type...
                return;
            }

            appendToText(text, SourceField.entry.getFrom(node), heading);
            appendToText(text, SourceField.entries.getFrom(node), heading);

            JsonNode additionalEntries = Tools5eFields.additionalEntries.getFrom(node);
            if (additionalEntries != null) {
                String altSource = getSources().alternateSource();
                for (JsonNode entry : iterableElements(additionalEntries)) {
                    String entrySource = SourceField.source.getTextOrNull(entry);
                    if (entrySource != null && !index().sourceIncluded(entrySource)) {
                        return;
                    } else if (!index().sourceIncluded(altSource)) {
                        return;
                    }
                    appendToText(text, entry, heading);
                }
            }
        } catch (RuntimeException ex) {
            tui().errorf(ex, "Error [%s] occurred while parsing %s", ex.getMessage(), node.toString());
            throw ex;
        } finally {
            parseState().pop(pushed);
        }
    }

    default void appendAbility(AppendTypeValue type, List<String> text, JsonNode entry) {
        if (type == AppendTypeValue.abilityDc) {
            text.add(String.format(
                    "**Spell save DC**: 8 + your proficiency bonus + your %s modifier",
                    asAbilityEnum(entry.withArray("attributes").get(0))));
        } else if (type == AppendTypeValue.abilityAttackMod) {
            text.add(String.format(
                    "**Spell attack modifier**: your proficiency bonus + your %s modifier",
                    asAbilityEnum(entry.withArray("attributes").get(0))));
        } else { // abilityGeneric
            List<String> abilities = new ArrayList<>();
            iterableElements(Tools5eFields.attributes.getFrom(entry))
                    .forEach(x -> abilities.add(asAbilityEnum(x)));

            List<String> inner = new ArrayList<>();
            SourceField.name.appendUnlessEmptyFrom(entry, inner);
            Tools5eFields.text.appendUnlessEmptyFrom(entry, inner);
            inner.add(String.join(", ", abilities));
            inner.add("modifier");

            maybeAddBlankLine(text);
            text.add(String.join(" ", inner));
            maybeAddBlankLine(text);
        }
    }

    default void appendCallout(String callout, String title, List<String> text, JsonNode entry) {
        List<String> inner = new ArrayList<>();
        appendToText(inner, entry.get("entries"), null);

        maybeAddBlankLine(text);
        text.add("> [!" + callout + "] " + replaceText(SourceField.name.getTextOrDefault(entry, title)));
        inner.forEach(x -> text.add("> " + x));
    }

    default void appendClassFeatureRef(List<String> text, JsonNode entry, Tools5eIndexType featureType, String fieldName) {
        ClassFeature cf = Json2QuteClass.findClassFeature(this, featureType, entry, fieldName);
        if (cf == null) {
            return; // skipped or not found
        }
        if (parseState().inList()) {
            // emit within an existing list item
            cf.appendListItemText(this, text, parseState().getSource(featureType));
        } else {
            // emit inline as proper section
            cf.appendText(this, text, parseState().getSource(featureType));
        }
    }

    default void appendEntriesToText(List<String> text, JsonNode entryNode, String heading) {
        String name = SourceField.name.getTextOrNull(entryNode);
        if (heading == null) {
            List<String> inner = new ArrayList<>();
            appendToText(inner, SourceField.entries.getFrom(entryNode), null);
            if (prependField(entryNode, SourceField.name, inner)) {
                maybeAddBlankLine(text);
            }
            text.addAll(inner);
        } else if (name != null) {
            maybeAddBlankLine(text);
            // strip links from heading titles. Cross-referencing headers with links is hard
            text.add(heading + " " + replaceText(name).replaceAll("\\[(.*?)\\]\\(.*?\\)", "$1"));
            if (index().differentSource(getSources(), parseState().getSource())) {
                text.add(getSourceText(entryNode));
            }
            text.add("");
            appendToText(text, SourceField.entries.getFrom(entryNode), "#" + heading);
        } else {
            appendToText(text, SourceField.entries.getFrom(entryNode), heading);
        }
    }

    /** Internal */
    default void appendEntryItem(List<String> text, JsonNode itemNode) {
        List<String> inner = new ArrayList<>();
        appendToText(inner, SourceField.entry.getFrom(itemNode), null);
        appendToText(inner, SourceField.entries.getFrom(itemNode), null);
        if (prependField(itemNode, SourceField.name, inner)) {
            maybeAddBlankLine(text);
        }
        text.addAll(inner);
    }

    default void appendGallery(List<String> text, JsonNode imageNode) {
        text.add("> [!gallery]");
        imageNode.withArray("images").forEach(image -> {
            ImageRef imageRef = readImageRef(image);
            if (imageRef != null) {
                text.add("> " + imageRef.getEmbeddedLink("gallery"));
            }
        });
    }

    default void appendImage(List<String> text, JsonNode imageNode) {
        ImageRef imageRef = readImageRef(imageNode);
        if (imageRef == null) {
            tui().warnf("Image information not found in %s", imageNode);
            return;
        }
        maybeAddBlankLine(text);
        text.add(imageRef.getEmbeddedLink());
    }

    default void appendLink(List<String> text, JsonNode link) {
        JsonMediaHref mediaRef = readLink(link);
        if (mediaRef == null) {
            tui().warnf("link information not found in %s", link);
            return;
        }
        if ("external".equals(mediaRef.href.type)) {
            text.add("[" + mediaRef.text + "](" + mediaRef.href.url + ")");
        }
        text.add(mediaRef.text);
    }

    default void appendList(List<String> text, ArrayNode itemArray) {
        String indent = parseState().getListIndent();
        boolean pushed = parseState().indentList();
        try {
            maybeAddBlankLine(text);
            itemArray.forEach(e -> {
                List<String> item = new ArrayList<>();
                appendToText(item, e, null);
                if (item.size() > 0) {
                    text.add(indent + "- " + item.get(0) + "  ");
                    item.remove(0);
                    item.forEach(x -> text.add(x.isEmpty() ? "" : indent + "    " + x + "  "));
                }
            });
        } finally {
            parseState().pop(pushed);
        }
    }

    default void appendOptionalFeature(List<String> text, JsonNode entry, String heading) {
        maybeAddBlankLine(text);
        text.add(heading + " " + SourceField.name.getTextOrEmpty(entry));
        String prereq = getTextOrDefault(entry, "prerequisite", null);
        if (prereq != null) {
            text.add("*Prerequisites* " + prereq);
        }
        text.add("");
        appendToText(text, SourceField.entries.getFrom(entry), "#" + heading);
    }

    default void appendOptionalFeatureRef(List<String> text, JsonNode entry) {
        String lookup = Tools5eFields.optionalfeature.getTextOrNull(entry);
        if (lookup == null) {
            tui().warnf("Optional Feature not found in %s", entry);
            return; // skipped or not found
        }
        String[] parts = lookup.split("\\|");
        String nodeSource = parts.length > 1 && !parts[1].isBlank() ? parts[1]
                : Tools5eIndexType.optionalfeature.defaultSourceString();
        String key = Tools5eIndexType.optionalfeature.createKey(lookup, nodeSource);
        if (index().isIncluded(key)) {
            if (parseState().inList()) {
                text.add(linkifyOptionalFeature(lookup));
            } else {
                tui().errorf("TODO refOptionalfeature %s -> %s",
                        lookup, Tools5eIndexType.optionalfeature.fromRawKey(lookup));
            }
        }
    }

    default void appendOptions(List<String> text, JsonNode entry) {
        String indent = parseState().getListIndent();
        boolean pushed = parseState().indentList();
        try {
            List<String> list = new ArrayList<>();
            for (JsonNode e : iterableEntries(entry)) {
                List<String> item = new ArrayList<>();
                appendToText(item, e, null);
                if (item.size() > 0) {
                    text.add(indent + "- " + item.get(0) + "  ");
                    item.remove(0);
                    item.forEach(x -> text.add(x.isEmpty() ? "" : indent + "    " + x + "  "));
                }
            }

            if (list.size() > 0) {
                maybeAddBlankLine(text);
                int count = intOrDefault(entry, "count", 0);
                text.add(String.format("Options%s:",
                        count > 0 ? " (choose " + count + ")" : ""));
                maybeAddBlankLine(text);
                text.addAll(list);
            }
        } finally {
            parseState().pop(pushed);
        }
    }

    default void appendInset(List<String> text, JsonNode entry) {
        List<String> insetText = new ArrayList<>();
        appendToText(insetText, entry.get("entries"), null);
        if (insetText.isEmpty()) {
            return; // nothing to do (empty content)
        }

        String title = null;
        String id = null;
        if (entry.has("name")) {
            title = entry.get("name").asText();
            id = title;
        } else if (getSources().getType() == Tools5eIndexType.race) {
            title = insetText.remove(0);
            id = title;
        } else if (entry.has("id")) {
            id = entry.get("id").asText();
        }

        maybeAddBlankLine(text);
        if (insetText.get(0).startsWith("> ")) {
            // do not wrap empty or already inset content in another inset
            text.addAll(insetText);
        } else {
            if (id != null) {
                insetText.add(0, "");
                insetText.add(0, "[!quote] " + (title == null ? "..." : title));
            }
            insetText.forEach(x -> text.add("> " + x));
        }

        if (id != null) {
            text.add("^" + slugify(id));
        }
    }

    default void appendFlowchart(List<String> text, JsonNode entry, String heading) {
        if (entry.has("name")) {
            maybeAddBlankLine(text);
            text.add(heading + " " + entry.get("name").asText());
        }

        for (JsonNode n : entry.withArray("blocks")) {
            maybeAddBlankLine(text);
            text.add("> [!flowchart] " + getTextOrEmpty(n, "name"));
            for (JsonNode e : n.withArray("entries")) {
                text.add("> " + replaceText(e.asText()));
            }
            text.add("%% %%");
        }
    }

    default void appendQuote(List<String> text, JsonNode entry) {
        List<String> quoteText = new ArrayList<>();
        if (entry.has("by")) {
            String by = replaceText(entry.get("by").asText());
            quoteText.add("[!quote]- A quote from " + by + "  ");
        } else {
            quoteText.add("[!quote]-  ");
        }
        appendToText(quoteText, entry.get("entries"), null);

        maybeAddBlankLine(text);
        quoteText.forEach(x -> text.add("> " + x));
        maybeAddBlankLine(text);
    }

    default void appendStatblock(List<String> text, JsonNode entry) {
        // Most use "tag", except for subclass, which uses "prop"
        Tools5eIndexType type = Tools5eIndexType.fromText(getTextOrDefault(entry, "tag", getTextOrEmpty(entry, "prop")));
        if (type == null) {
            tui().errorf("Unrecognized statblock type in %s", entry);
            return;
        }

        String name = SourceField.name.getTextOrEmpty(entry);
        String source = SourceField.source.getTextOrEmpty(entry);
        String link = type == Tools5eIndexType.subclass
                ? linkify(type, Tools5eIndexType.getSubclassTextReference(
                        Tools5eFields.className.getTextOrEmpty(entry),
                        Tools5eFields.classSource.getTextOrEmpty(entry),
                        name, source, name))
                : linkify(type, name + "|" + source);

        if (link.matches("\\[.*]\\(.*\\)")) {
            maybeAddBlankLine(text);
            if (type == Tools5eIndexType.monster) {
                text.add("!" + link.replaceAll("\\)$", "#^statblock)"));
            } else {
                text.add("> [!summary] " + name);
                text.add("> !" + link);
            }
        } else {
            text.add(link);
            tui().warnf("statblock entry did not resolve to a markdown link: %s", entry);
        }
    }

    default void appendTable(List<String> text, JsonNode tableNode) {
        boolean pushed = parseState().push(tableNode);
        try {
            List<String> table = new ArrayList<>();

            String header;
            String blockid = "";
            String caption = TableFields.caption.getTextOrEmpty(tableNode);

            String known = findTable(Tools5eIndexType.table, tableNode);
            if (known != null) {
                maybeAddBlankLine(text);
                text.add(known);
                return;
            }

            if (TableFields.colLabels.existsIn(tableNode)) {
                header = String.join(" | ", TableFields.colLabels.replaceTextFromList(tableNode, this));

                blockid = slugify(header.replaceAll("d\\d+", "")
                        .replace("|", "")
                        .replaceAll("\\s+", " ")
                        .trim());
            } else if (TableFields.colStyles.existsIn(tableNode)) {
                header = TableFields.colStyles.getListOfStrings(tableNode, tui()).stream()
                        .map(x -> "  ")
                        .collect(Collectors.joining(" | "));
            } else {
                int length = TableFields.rows.size(tableNode);
                String[] array = new String[length];
                Arrays.fill(array, " ");
                header = "|" + String.join(" | ", array) + " |";
            }

            for (JsonNode r : TableFields.rows.iterateArrayFrom(tableNode)) {
                JsonNode cells;
                if ("row".equals(TableFields.type.getTextOrNull(r))) {
                    cells = TableFields.row.getFrom(r);
                } else {
                    cells = r;
                }

                String row = "| " + streamOf(cells)
                        .map(x -> {
                            JsonNode roll = RollFields.roll.getFrom(x);
                            if (roll != null) {
                                if (RollFields.exact.existsIn(roll)) {
                                    return RollFields.exact.getFrom(roll);
                                }
                                return new TextNode(
                                        RollFields.min.getTextOrEmpty(roll) + "-" + RollFields.max.getTextOrEmpty(roll));
                            }
                            return x;
                        })
                        .map(x -> flattenToString(x).replace("\n", "<br />"))
                        .collect(Collectors.joining(" | ")) + " |";
                table.add(row);
            }

            header = "| " + header.replaceAll("^(d\\d+.*)", "dice: $1") + " |";
            table.add(0, header.replaceAll("[^|]", "-"));
            table.add(0, header);

            if (!caption.isBlank()) {
                table.add(0, "");
                table.add(0, "**" + caption + "**");
                blockid = slugify(caption);
            }
            if (!blockid.isBlank()) {
                table.add("^" + blockid);
            }

            switch (blockid) {
                case "personality-trait" -> Json2QuteBackground.traits.addAll(table);
                case "ideal" -> Json2QuteBackground.ideals.addAll(table);
                case "bond" -> Json2QuteBackground.bonds.addAll(table);
                case "flaw" -> Json2QuteBackground.flaws.addAll(table);
            }

            JsonNode intro = TableFields.intro.getFrom(tableNode);
            if (intro != null) {
                maybeAddBlankLine(text);
                appendToText(text, intro, null);
            }
            maybeAddBlankLine(text);
            text.addAll(table);

            JsonNode footnotes = TableFields.footnotes.getFrom(tableNode);
            if (footnotes != null) {
                maybeAddBlankLine(text);
                boolean pushF = parseState().push(true);
                appendToText(text, footnotes, null);
                parseState().pop(pushF);
            }
            JsonNode outro = TableFields.outro.getFrom(tableNode);
            if (outro != null) {
                maybeAddBlankLine(text);
                appendToText(text, outro, null);
            }
        } finally {
            parseState().pop(pushed);
        }
    }

    default void appendTableGroup(List<String> text, JsonNode tableGroup, String heading) {
        boolean pushed = parseState().push(tableGroup);
        try {
            String name = findTableName(tableGroup);
            String known = findTable(Tools5eIndexType.tableGroup, tableGroup);
            if (known != null) {
                maybeAddBlankLine(text);
                text.add(known);
                return;
            }

            maybeAddBlankLine(text);
            text.add(heading + " " + name);
            if (index().differentSource(getSources(), parseState().getSource())) {
                text.add(getSourceText(parseState()));
            }
            maybeAddBlankLine(text);
            appendToText(text, Tools5eFields.tables.getFrom(tableGroup), "#" + heading);
        } finally {
            parseState().pop(pushed);
        }
    }

    default String findTableName(JsonNode tableNode) {
        return TableFields.caption.getTextOrDefault(tableNode,
                SourceField.name.getTextOrEmpty(tableNode));
    }

    default String findTable(Tools5eIndexType keyType, JsonNode matchTable) {
        if (getSources().getType() == Tools5eIndexType.table || getSources().getType() == Tools5eIndexType.tableGroup) {
            return null;
        }
        String name = findTableName(matchTable);
        String tableKey = keyType.createKey(name, parseState().getSource());
        JsonNode knownEntry = index().getNode(tableKey);
        if (knownEntry == null && keyType == Tools5eIndexType.table) {
            SourceAndPage sp = parseState().toSourceAndPage();
            knownEntry = index().findTable(sp, TableFields.getFirstRow(matchTable));
        }
        if (knownEntry != null) {
            // replace with embed
            String link = linkifyType(keyType, tableKey, name);
            return "!" + link;
        }
        return null;
    }

    default ImageRef readImageRef(JsonNode imageNode) {
        try {
            JsonMediaHref mediaHref = mapper().treeToValue(imageNode, JsonMediaHref.class);
            return buildImageRef(index(), mediaHref, getImagePath());
        } catch (JsonProcessingException | IllegalArgumentException e) {
            tui().errorf(e, "Unable to read media reference from %s: %s", imageNode, e.toString());
        }
        return null;
    }

    default JsonMediaHref readLink(JsonNode linkNode) {
        try {
            return mapper().treeToValue(linkNode, JsonMediaHref.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            tui().errorf(e, "Unable to read link from %s: %s", linkNode, e.toString());
        }
        return null;
    }

    default boolean useCompendium() {
        return getSources().getType().useCompendiumBase();
    }

    default String getImagePath() {
        Tools5eIndexType type = getSources().getType();
        return Tools5eQuteBase.getRelativePath(type);
    }

    default String asAbilityEnum(JsonNode textNode) {
        return SkillOrAbility.format(textNode.asText());
    }

    default String mapAlignmentToString(String a) {
        return switch (a) {
            case "A" -> "Any alignment";
            case "C" -> "Chaotic";
            case "CE" -> "Chaotic Evil";
            case "CELENE", "LNXCE" -> "Any Evil Alignment";
            case "CG" -> "Chaotic Good";
            case "CGNE" -> "Chaotic Good or Neutral Evil";
            case "CGNYE" -> "Any Chaotic alignment";
            case "CN" -> "Chaotic Neutral";
            case "N", "NX", "NY" -> "Neutral";
            case "NE" -> "Neutral Evil";
            case "NG" -> "Neutral Good";
            case "NGNE", "NENG" -> "Neutral Good or Neutral Evil";
            case "NNXNYN", "NXCGNYE" -> "Any Non-Lawful alignment";
            case "L" -> "Lawful";
            case "LE" -> "Lawful Evil";
            case "LG" -> "Lawful Good";
            case "LN" -> "Lawful Neutral";
            case "LELG" -> "Lawful Evil or Lawful Good";
            case "LELN" -> "Lawful Evil or Lawful Neutral";
            case "LNXCNYE" -> "Any Non-Good alignment";
            case "E" -> "Any Evil alignment";
            case "G" -> "Any Good alignment";
            case "U" -> "Unaligned";
            default -> {
                tui().errorf("What alignment is this? %s (from %s)", a, getSources());
                yield "Unknown";
            }
        };
    }

    default int levelToPb(int level) {
        // 2 + (¼ * (Level – 1))
        return 2 + ((int) (.25 * (level - 1)));
    }

    default String monsterCr(JsonNode monster) {
        if (monster.has("cr")) {
            JsonNode crNode = monster.get("cr");
            if (crNode.isTextual()) {
                return crNode.asText();
            } else if (crNode.has("cr")) {
                return crNode.get("cr").asText();
            } else {
                tui().errorf("Unable to parse cr value from %s", crNode.toPrettyString());
            }
        }
        return null;
    }

    default int crToXp(JsonNode cr) {
        if (cr.has("xp")) {
            return cr.get("xp").asInt();
        }
        if (cr.has("cr")) {
            cr = cr.get("cr");
        }
        String crKey = cr.asText();
        return XP_CHART_ALT.get(crKey);
    }

    default int crToPb(JsonNode cr) {
        if (cr.isTextual()) {
            return crToPb(cr.asText());
        }
        return crToPb(cr.get("cr").asText());
    }

    default int crToPb(String crValue) {
        double crDouble = crToNumber(crValue);
        if (crDouble < 5)
            return 2;
        return (int) Math.ceil(crDouble / 4) + 1;
    }

    default double crToNumber(String crValue) {
        if (crValue.equals("Unknown") || crValue.equals("\u2014")) {
            return CR_UNKNOWN;
        }
        String[] parts = crValue.trim().split("/");
        try {
            if (parts.length == 1) {
                return Double.parseDouble(parts[0]);
            } else if (parts.length == 2) {
                return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
            }
        } catch (NumberFormatException nfe) {
            return CR_CUSTOM;
        }
        return 0;
    }

    default String getSize(JsonNode value) {
        JsonNode size = value.get("size");
        if (size == null) {
            throw new IllegalArgumentException("Missing size attribute from " + getSources());
        }
        try {
            if (size.isTextual()) {
                return sizeToString(size.asText());
            } else if (size.isArray()) {
                String merged = streamOf(size).map(JsonNode::asText).collect(Collectors.joining());
                return sizeToString(merged);
            }
        } catch (IllegalArgumentException ignored) {
        }
        tui().errorf("Unable to parse size for %s from %s", getSources(), size.toPrettyString());
        return "Unknown";
    }

    default String sizeToString(String size) {
        return switch (size) {
            case "F" -> "Fine";
            case "D" -> "Diminutive";
            case "T" -> "Tiny";
            case "S" -> "Small";
            case "M" -> "Medium";
            case "L" -> "Large";
            case "H" -> "Huge";
            case "G" -> "Gargantuan";
            case "C" -> "Colossal";
            case "V" -> "Varies";
            case "SM" -> "Small or Medium";
            default -> "Unknown";
        };
    }

    default String getSpeed(JsonNode value) {
        JsonNode speed = value.get("speed");
        try {
            if (speed == null) {
                return "30 ft.";
            } else if (speed.isTextual()) {
                return speed.asText();
            } else if (speed.isIntegralNumber()) {
                return speed.asText() + " ft.";
            } else if (speed.isObject()) {
                List<String> list = new ArrayList<>();
                speed.fields().forEachRemaining(f -> {
                    if (f.getValue().isIntegralNumber()) {
                        list.add(String.format("%s: %s ft.",
                                f.getKey(), f.getValue().asText()));
                    } else if (f.getValue().isBoolean()) {
                        list.add(f.getKey() + " equal to your walking speed");
                    }
                });
                return String.join("; ", list);
            }
        } catch (IllegalArgumentException ignored) {
        }
        tui().errorf("Unable to parse speed for %s from %s", getSources(), speed);
        return "30 ft.";
    }

    default String raceToText(JsonNode race) {
        StringBuilder str = new StringBuilder();
        str.append(race.get("name").asText());
        if (race.has("subrace")) {
            str.append(" (").append(race.get("subrace").asText()).append(")");
        }
        return str.toString();
    }

    default String levelToText(JsonNode levelNode) {
        if (levelNode.isObject()) {
            List<String> levelText = new ArrayList<>();
            levelText.add(levelToText(levelNode.get("level").asText()));
            if (levelNode.has("class") || levelNode.has("subclass")) {
                JsonNode classNode = levelNode.get("class");
                if (classNode == null) {
                    classNode = levelNode.get("subclass");
                }
                boolean visible = !classNode.has("visible") || classNode.get("visible").asBoolean();
                JsonNode source = classNode.get("source");
                boolean included = source == null || index().sourceIncluded(source.asText());
                if (visible && included) {
                    levelText.add(classNode.get("name").asText());
                }
            }
            return String.join(" ", levelText);
        } else {
            return levelToText(levelNode.asText());
        }
    }

    default String levelToText(String level) {
        return switch (level) {
            case "0" -> "cantrip";
            case "1" -> "1st-level";
            case "2" -> "2nd-level";
            case "3" -> "3rd-level";
            default -> level + "th-level";
        };
    }

    static String levelToString(int level) {
        return switch (level) {
            case 1 -> "1st";
            case 2 -> "2nd";
            case 3 -> "3rd";
            default -> level + "th";
        };
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    class JsonMediaHref {
        public String type;
        public JsonHref href;
        public String title;
        public Integer width;
        public Integer height;
        public String altText;
        public String credit;
        public String text;
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    class JsonHref {
        public String type;
        public String path;
        public String url;
    }

    Map<String, Integer> XP_CHART_ALT = Map.ofEntries(
            entry("0", 10),
            entry("1/8", 25),
            entry("1/4", 50),
            entry("1/2", 100),
            entry("1", 200),
            entry("2", 450),
            entry("3", 700),
            entry("4", 1100),
            entry("5", 1800),
            entry("6", 2300),
            entry("7", 2900),
            entry("8", 3900),
            entry("9", 5000),
            entry("10", 5900),
            entry("11", 7200),
            entry("12", 8400),
            entry("13", 10000),
            entry("14", 11500),
            entry("15", 13000),
            entry("16", 15000),
            entry("17", 18000),
            entry("18", 20000),
            entry("19", 22000),
            entry("20", 25000),
            entry("21", 33000),
            entry("22", 41000),
            entry("23", 50000),
            entry("24", 62000),
            entry("25", 75000),
            entry("26", 90000),
            entry("27", 105000),
            entry("28", 120000),
            entry("29", 135000),
            entry("30", 155000));

    enum Tools5eFields implements JsonNodeReader {
        abbreviation,
        additionalEntries,
        appliesTo,
        attributes,
        className,
        classSource,
        featureType,
        group,
        optionalfeature,
        style,
        tables, // for optfeature types
        text,
        typeLookup,
    }

    enum TableFields implements JsonNodeReader {
        tables,
        caption,
        colLabels,
        colLabelGroups,
        colStyles,
        rowLabels,
        rows,
        row,
        footnotes,
        intro,
        outro,
        type;

        static String getFirstRow(JsonNode tableNode) {
            JsonNode rowData = rows.getFrom(tableNode);
            if (rowData == null) {
                return "";
            }
            return rowData.get(0).toString();
        }
    }

    enum RollFields implements JsonNodeReader {
        roll,
        exact,
        min,
        max
    }

    enum AppendTypeValue implements JsonNodeReader.FieldValue {
        // recursive
        entries,
        entry,
        inset,
        insetReadaloud,
        list,
        optfeature,
        options,
        quote,
        section,
        table,
        tableGroup,
        variant,
        variantInner,
        variantSub,

        // block
        abilityAttackMod,
        abilityDc,
        abilityGeneric,

        // inline
        inline,
        inlineBlock,
        link,

        // list items
        item,
        itemSpell,
        itemSub,

        // images
        image,
        gallery,

        // flowchart
        flowchart,
        // TODO: flowBlock,

        // embedded entities
        statblock,
        // TODO: statblockInline,

        refClassFeature,
        refOptionalfeature,
        refSubclassFeature,

        // homebrew changes
        homebrew,

        // misc
        hr;

        @Override
        public String value() {
            return this.name();
        }

        static AppendTypeValue valueFrom(JsonNode source, JsonNodeReader field) {
            String textOrNull = field.getTextOrNull(source);
            if (textOrNull == null) {
                return null;
            }
            return Stream.of(AppendTypeValue.values())
                    .filter((t) -> t.matches(textOrNull))
                    .findFirst().orElse(null);
        }
    }
}
