package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.joinConjunct;
import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.SourceAndPage;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.JsonSourceCopier.MetaFields;
import dev.ebullient.convert.tools.JsonTextConverter;
import dev.ebullient.convert.tools.ParseState;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.dnd5e.Json2QuteClass.ClassFeature;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;
import io.quarkus.runtime.annotations.RegisterForReflection;

public interface JsonSource extends JsonTextReplacement {
    int CR_UNKNOWN = 100001;
    int CR_CUSTOM = 100000;
    Pattern leadingNumber = Pattern.compile("(\\d+)(.*)");

    default String getName() {
        return getSources() == null ? null : getSources().getName();
    }

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

    default int intOrThrow(JsonNode source, String key) {
        JsonNode result = source.get(key);
        if (result == null || !result.canConvertToInt()) {
            tui().errorf("Missing required field, or field is not a number. Key: %s; value: %s; from %s: %s",
                    key, result, getSources(), source);
            return -999;
        }
        return result.asInt();
    }

    default String getSourceText(JsonNode node) {
        return getSourceText(Tools5eSources.findOrTemporary(node));
    }

    default String getSourceText(ParseState parseState) {
        return parseState().longSourcePageString("_Source: %s_");
    }

    default String getSourceText(Tools5eSources currentSource) {
        return currentSource.getSourceText();
    }

    default String getLabeledSource(JsonNode node) {
        return getLabeledSource(Tools5eSources.findOrTemporary(node));
    }

    default String getLabeledSource(Tools5eSources currentSource) {
        return "_Source: " + getSourceText(currentSource) + "_";
    }

    default ImageRef buildImageRef(JsonMediaHref mediaHref, String imageBasePath) {
        return getSources().buildImageRef(index(), mediaHref, imageBasePath, useCompendium());
    }

    default String getFileName() {
        return linkifier().getTargetFileName(getName(), getSources());
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
            if (node == null || node.isNull()) {
                // do nothing
            } else if (node.isTextual()) {
                text.add(replaceText(node.asText()));
            } else if (node.isNumber() || node.isBoolean()) {
                text.add(node.asText());
            } else if (node.isArray()) {
                for (JsonNode f : iterableElements(node)) {
                    maybeAddBlankLine(text);
                    appendToText(text, f, heading);
                }
            } else if (node.isObject()) {
                appendObjectToText(text, node, heading);
            } else {
                tui().debugf(Msg.UNKNOWN, "Unknown entry type in %s: %s", getSources(), node.toPrettyString());
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
            if (!getSources().includedByConfig()) {
                return;
            }
        }

        boolean pushed = parseState().push(node);
        try {
            if (type != null) {
                switch (type) {
                    case attack -> appendAttack(text, node);
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
                    case inset, insetReadaloud -> appendInset(type, text, node);
                    case link -> appendLink(text, node);
                    case list -> {
                        String style = Tools5eFields.style.getTextOrEmpty(node);
                        if ("list-no-bullets".equals(style)) {
                            if (node.has("columns")) {
                                maybeAddBlankLine(text);
                                appendToText(text, SourceField.items.readArrayFrom(node), heading);
                            } else {
                                appendList(text, SourceField.items.readArrayFrom(node), ListType.unstyled);
                            }
                        } else {
                            appendList(text, SourceField.items.readArrayFrom(node), ListType.unordered);
                        }
                    }
                    case optfeature -> appendOptionalFeature(text, node, heading);
                    case options -> appendOptions(text, node);
                    case quote -> appendQuote(text, node);
                    case refClassFeature -> appendClassFeatureRef(text, node, Tools5eIndexType.classfeature, "classFeature");
                    case refOptionalfeature -> appendOptionalFeatureRef(text, node);
                    case refSubclassFeature -> appendClassFeatureRef(text, node, Tools5eIndexType.subclassFeature,
                            "subclassFeature");
                    case statblock -> appendStatblock(text, node, heading);
                    case statblockInline -> appendStatblockInline(text, node, heading);
                    case table -> appendTable(text, node);
                    case tableGroup -> appendTableGroup(text, node, heading);
                    case variant -> appendCallout("danger", "Variant", text, node);
                    case variantInner, variantSub -> appendCallout("example", "Variant", text, node);
                    default -> tui().debugf(Msg.UNKNOWN, "Unknown entry object type %s from %s: %s",
                            type, getSources(), node.toPrettyString());
                }
                // any entry/entries handled by type...
                return;
            }

            appendToText(text, SourceField.entry.getFrom(node), heading);
            appendToText(text, SourceField.entries.getFrom(node), heading);

            JsonNode additionalEntries = Tools5eFields.additionalEntries.getFrom(node);
            if (additionalEntries != null) {
                for (JsonNode entry : iterableElements(additionalEntries)) {
                    String entrySource = SourceField.source.getTextOrNull(entry);
                    if (entrySource != null && !cfg().sourceIncluded(getSources())) {
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
        List<String> abilities = Tools5eFields.attributes.streamFrom(entry)
                .map(this::asAbilityEnum)
                .toList();
        String ability = joinConjunct(" or ", abilities);

        if (type == AppendTypeValue.abilityDc) {
            text.add(spanWrap("abilityDc",
                    "**Spell save DC**: 8 + your proficiency bonus + your %s modifier"
                            .formatted(ability)));
        } else if (type == AppendTypeValue.abilityAttackMod) {
            text.add(spanWrap("abilityAttackMod",
                    "**Spell attack modifier**: your proficiency bonus + your %s modifier"
                            .formatted(ability)));
        } else { // abilityGeneric
            List<String> inner = new ArrayList<>();
            String name = SourceField.name.replaceTextFrom(entry, this);
            if (isPresent(name)) {
                inner.add("**" + name + ".**");
            }
            if (Tools5eFields.text.existsIn(entry)) {
                Tools5eFields.text.replaceTextFrom(entry, this);
            }
            if (!abilities.isEmpty()) {
                inner.add(ability + " modifier");
            }

            maybeAddBlankLine(text);
            text.add(spanWrap("abilityGeneric", String.join(" ", inner)));
            maybeAddBlankLine(text);
        }
    }

    default void appendAttack(List<String> text, JsonNode entry) {
        String name = SourceField.name.replaceTextFrom(entry, this);
        String attackType = AttackFields.attackType.getTextOrDefault(entry, "MW");
        String atkString = flattenToString(AttackFields.attackEntries.getFrom(entry), " ");
        String hitString = flattenToString(AttackFields.hitEntries.getFrom(entry), " ");

        text.add(spanWrap("attack",
                "%s*%s:* %s *Hit:* %s".formatted(
                        isPresent(name) ? "***" + name + ".*** " : "",
                        "MW".equals(attackType) ? "Melee Weapon Attack" : "Ranged Weapon Attack",
                        atkString, hitString)));
    }

    default void appendCallout(String callout, String title, List<String> text, JsonNode entry) {
        List<String> inner = new ArrayList<>();
        appendToText(inner, SourceField.entries.getFrom(entry), null);

        maybeAddBlankLine(text);
        text.add("> [!" + callout + "] " + replaceText(SourceField.name.getTextOrDefault(entry, title)));
        inner.forEach(x -> text.add("> " + x));
    }

    default void appendClassFeatureRef(List<String> text, JsonNode entry, Tools5eIndexType featureType, String fieldName) {
        ClassFeature cf = Json2QuteClass.findClassFeature(this, featureType, entry, fieldName);
        if (cf == null) {
            return; // skipped or not found
        }
        if (parseState().featureTypeDepth() > 2) {
            tui().errorf("Cycle in class or subclass features found in %s", cf.cfSources());
            // this is within an existing feature description. Emit as a link
            cf.appendLink(this, text, parseState().getSource(featureType));
        } else if (parseState().inList()) {
            // emit within an existing list item
            cf.appendListItemText(this, text, parseState().getSource(featureType));
        } else {
            // emit inline as proper section
            cf.appendText(this, text, parseState().getSource(featureType));
        }
    }

    default void appendEntriesToText(List<String> text, JsonNode entryNode, String heading) {
        String name = SourceField.name.replaceTextFrom(entryNode, this);
        if (heading == null) {
            List<String> inner = new ArrayList<>();
            appendToText(inner, SourceField.entries.getFrom(entryNode), null);
            if (prependField(name, inner)) {
                maybeAddBlankLine(text);
            }
            text.addAll(inner);
        } else if (isPresent(name)) {
            maybeAddBlankLine(text);
            // strip links from heading titles. Cross-referencing headers with links is hard
            text.add(heading + " " + name.replaceAll("\\[(.*?)\\]\\(.*?\\)", "$1"));
            if (!parseState().sourcePageString().isBlank() && index().differentSource(getSources(), parseState().getSource())) {
                text.add(getSourceText(parseState()));
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
        if (SourceField.name.existsIn(itemNode) && prependField(itemNode, SourceField.name, inner)) {
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

    enum ListType {
        unordered("- "),
        unstyled("");

        final String marker;

        ListType(String marker) {
            this.marker = marker;
        }
    }

    default void appendList(List<String> text, ArrayNode itemArray, ListType listType) {
        String indent = parseState().getListIndent();
        boolean pushed = parseState().indentList();
        try {
            maybeAddBlankLine(text);
            itemArray.forEach(e -> {
                List<String> item = new ArrayList<>();
                appendToText(item, e, null);
                if (item.size() > 0) {
                    text.add(indent + listType.marker + item.get(0) + "  ");
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
        text.add(heading + " " + SourceField.name.replaceTextFrom(entry, this));
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
            tui().warnf(Msg.UNRESOLVED, "Optional Feature not found in %s", entry);
            return; // skipped or not found
        }
        String[] parts = lookup.split("\\|");
        String nodeSource = parts.length > 1 && !parts[1].isBlank() ? parts[1]
                : Tools5eIndexType.optfeature.defaultSourceString();
        String key = Tools5eIndexType.optfeature.createKey(lookup, nodeSource);
        if (index().isIncluded(key)) {
            if (parseState().inList()) {
                text.add(linkify(Tools5eIndexType.optfeature, lookup));
            } else {
                tui().errorf("TODO refOptionalfeature %s -> %s",
                        lookup, Tools5eIndexType.optfeature.fromTagReference(lookup));
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

    default void appendInset(AppendTypeValue type, List<String> text, JsonNode entry) {
        List<String> insetText = new ArrayList<>();
        appendToText(insetText, SourceField.entries.getFrom(entry), null);
        if (insetText.isEmpty()) {
            return; // nothing to do (empty content)
        }

        String title = null;
        String id = null;
        if (entry.has("name")) {
            title = SourceField.name.getTextOrEmpty(entry);
            id = title;
        } else if (getSources().getType() == Tools5eIndexType.race) {
            title = insetText.remove(0);
            id = title;
        } else if (entry.has("id")) {
            id = SourceField.id.getTextOrEmpty(entry);
        }

        maybeAddBlankLine(text);
        if (!insetText.isEmpty() && insetText.get(0).startsWith("> ")) {
            // do not wrap empty or already inset content in another inset
            text.addAll(insetText);
        } else {
            if (id != null) {
                String admonition = type == AppendTypeValue.insetReadaloud ? "[!readaloud] " : "[!note] ";
                insetText.add(0, "");
                insetText.add(0, admonition + (isPresent(title) ? title : ""));
            }
            insetText.forEach(x -> text.add("> " + x));
        }

        if (isPresent(id)) {
            text.add("^" + slugify(id));
        }
    }

    default void appendFlowchart(List<String> text, JsonNode entry, String heading) {
        if (entry.has("name")) {
            maybeAddBlankLine(text);
            text.add(heading + " " + SourceField.name.replaceTextFrom(entry, this));
        }

        for (JsonNode n : entry.withArray("blocks")) {
            maybeAddBlankLine(text);
            text.add("> [!flowchart] " + SourceField.name.replaceTextFrom(n, this));
            for (JsonNode e : n.withArray("entries")) {
                text.add("> " + replaceText(e.asText()));
            }
            text.add("%% %%");
        }
    }

    default void appendQuote(List<String> text, JsonNode entry) {
        List<String> quoteText = new ArrayList<>();
        if (entry.has("by")) {
            String by = replaceText(Tools5eFields.by.getTextOrEmpty(entry));
            quoteText.add("[!quote] A quote from " + by + "  ");
        } else {
            quoteText.add("[!quote]  ");
        }
        appendToText(quoteText, SourceField.entries.getFrom(entry), null);

        maybeAddBlankLine(text);
        quoteText.forEach(x -> text.add("> " + x));
        maybeAddBlankLine(text);
    }

    default void appendStatblock(List<String> text, JsonNode entry, String heading) {
        // Most use "tag", except for subclass, which uses "prop"
        String tagPropText = Tools5eFields.tag.getTextOrDefault(entry, Tools5eFields.prop.getTextOrEmpty(entry));
        Tools5eIndexType type = Tools5eIndexType.fromText(tagPropText);
        if (type == null) {
            tui().debugf(Msg.SOMEDAY, "Unrecognized statblock type in %s", entry);
            return;
        }
        embedReference(text, entry, type, heading);
    }

    default void appendStatblockInline(List<String> text, JsonNode entry, String heading) {
        // For inline statblocks, we start with the dataType
        Tools5eIndexType type = Tools5eIndexType.fromText(Tools5eFields.dataType.getTextOrEmpty(entry));
        if (type == null) {
            tui().debugf(Msg.SOMEDAY, "Unrecognized statblock dataType in %s", entry);
            return;
        }
        JsonNode data = Tools5eFields.data.getFrom(entry);
        if (data == null) {
            tui().errorf("No data found in %s", entry);
            return;
        }
        // Replace text in embedded data node w/ trimmed name (ensure keys match)
        String name = SourceField.name.replaceTextFrom(data, this);
        ((ObjectNode) data).set("name", new TextNode(name));

        String source = SourceField.source.getTextOrEmpty(data);
        String finalKey = type.createKey(data);

        JsonNode existingNode = index().getNode(finalKey);
        TtrpgValue.indexKey.setIn(data, finalKey);
        TtrpgValue.indexInputType.setIn(data, type.name());

        // TODO: Remove me.
        JsonNode copy = MetaFields._copy.getFrom(data);
        if (copy != null) {
            String copyName = SourceField.name.getTextOrEmpty(copy).strip();
            String copySource = SourceField.source.getTextOrEmpty(copy).strip();
            if (name.equals(copyName) && source.equals(copySource)) {
                embedReference(text, data, type, heading); // embed note that will be present in the final output
                return;
            }
            Tools5eJsonSourceCopier copier = new Tools5eJsonSourceCopier(index());
            data = copier.handleCopy(type, data);
            existingNode = null; // this is a modified node, ignore existing.
        } else if (equivalentNode(data, existingNode) && index().isIncluded(finalKey)) {
            embedReference(text, data, type, heading); // embed note that will be present in the final output
            return;
        } else if (existingNode == null) {
            Tools5eSources.constructSources(finalKey, data);
        }

        Tools5eQuteBase qs = null;
        switch (type) {
            case item -> qs = new Json2QuteItem(index(), type, data).build();
            case monster -> qs = new Json2QuteMonster(index(), type, data).build();
            case object -> qs = new Json2QuteObject(index(), type, data).build();
            case spell -> qs = new Json2QuteSpell(index(), type, data).build();
            default -> tui().errorf("Not ready for statblock dataType in %s", entry);
        }
        if (qs != null) {
            if (type == Tools5eIndexType.monster) {
                // Create a new monster document (header for initiative tracker)
                String embedFileName = Tui.slugify(String.format("%s-%s-%s", getSources().getName(), type.name(), name));
                String relativePath = linkifier().getRelativePath(getSources());
                String vaultRoot = linkifier().vaultRoot(getSources());

                maybeAddBlankLine(text);
                text.add("> [!embed-monster]- " + name);
                text.add(String.format("> ![%s](%s%s/%s#^statblock)", name, vaultRoot, relativePath, embedFileName));

                // remember the file that should be created later.
                // use the relative path for the containing note (not the bestiary)
                getSources().addInlineNote(qs
                        .withTargetFile(embedFileName)
                        .withTargetPath(relativePath));
            } else {
                List<String> prepend = new ArrayList<>(List.of(
                        "title: " + name,
                        "collapse: closed",
                        existingNode == null ? "" : "%% See " + type.linkify(this, data) + " %%"));
                renderEmbeddedTemplate(text, qs, type.name(), prepend);
            }
        }
    }

    default boolean equivalentNode(JsonNode dataNode, JsonNode existingNode) {
        if (existingNode == null || dataNode == null || dataNode.has("_copy")) {
            return false;
        }
        for (Entry<String, JsonNode> field : iterableFields(dataNode)) {
            JsonNode existingField = existingNode.get(field.getKey());
            if (existingField == null || !field.getValue().equals(existingField)) {
                return false;
            }
        }
        return true;
    }

    default void embedReference(List<String> text, JsonNode entry, Tools5eIndexType type, String heading) {
        String name = SourceField.name.getTextOrEmpty(entry);
        String key = index().getAliasOrDefault(type.createKey(entry));
        Tools5eSources sources = Tools5eSources.findSources(key);
        if (key == null || sources == null) {
            tui().debugf(Msg.UNKNOWN, "unable to find statblock target %s from %s in %s", key, entry, getSources());
            return;
        }

        if (type == Tools5eIndexType.charoption) {
            // charoption is not a linkable type.
            tui().debugf(Msg.SOMEDAY, "charoption is not yet an embeddable type: %s", entry);
            return;
        } else if (type.isFluffType()) {
            // Fluff is not a linkable type, and is never added to the filtered index,
            // so we need to check if the material is included in other ways
            JsonNode fluffNode = index().getOrigin(key);
            if (!sources.includedByConfig() || fluffNode == null) {
                // do nothing if the source isn't included
                return;
            }
            List<ImageRef> images = new ArrayList<>();
            unpackFluffNode(type, fluffNode, text, null, images);
            maybeAddBlankLine(text);
            return;
        } else if (type == Tools5eIndexType.reference) {
            // reference is not a linkable type.
            tui().debugf(Msg.SOMEDAY, "reference is not yet an embeddable type: %s", entry);
            return;
        }

        String link = type.linkify(this, entry);

        if (link.matches("\\[.*]\\(.*\\)")) {
            maybeAddBlankLine(text);
            text.add("> [!embed-" + type.name() + "]- " + name);
            if (type == Tools5eIndexType.monster) {
                text.add("> !" + link.replaceAll("\\)$", "#^statblock)"));
            } else {
                text.add("> !" + link);
            }
        } else {
            text.add(link);
        }
        // 🔸 WARN| 🫣 unable to find statblock target {"type":"statblock","prop":"subclass","source":"XUA2023PlayersHandbookP7","name":"Aberrant","className":"Sorcerer","classSource":"XUA2023PlayersHandbookP7","collapsed":true,"displayName":"Aberrant Sorcery","indexInputType":"reference","indexKey":"reference|aberrant|xua2023playershandbookp7"} from sources[book|book-xua2023playershandbookp7]
        // 🔸 WARN| 🫣 unable to find statblock target {"type":"statblock","prop":"subclass","source":"XUA2023PlayersHandbookP7","name":"Clockwork","className":"Sorcerer","classSource":"XUA2023PlayersHandbookP7","collapsed":true,"displayName":"Clockwork Sorcery","indexInputType":"reference","indexKey":"reference|clockwork|xua2023playershandbookp7"} from sources[book|book-xua2023playershandbookp7]
        // 🔸 WARN| 🫣 unable to find statblock target {"type":"statblock","name":"Shove","source":"PHB","page":206,"tag":"action","indexInputType":"reference","indexKey":"reference|shove|phb"} from sources[itemgroup|honor's last stand|tdcsr]
        // 🔸 WARN| 🫣 unable to find statblock target {"type":"statblock","tag":"variantrule","source":"ESK","name":"Sidekicks","page":66,"indexInputType":"reference","indexKey":"reference|sidekicks|esk"} from sources[adventure|adventure-dip]
    }

    default void appendTable(List<String> text, JsonNode tableNode) {
        boolean pushed = parseState().push(tableNode);
        try {
            List<String> table = new ArrayList<>();

            String header;
            String caption = TableFields.caption.getTextOrEmpty(tableNode);
            String blockid = caption.isBlank()
                    ? ""
                    : "^" + slugify(caption);

            String known = findTable(Tools5eIndexType.table, tableNode);
            if (known != null) {
                maybeAddBlankLine(text);
                text.add(known);
                return;
            }

            boolean pushTable = parseState().pushMarkdownTable(true);
            try {
                if (TableFields.colLabels.existsIn(tableNode)) {
                    List<String> labels = TableFields.colLabels.getListOfStrings(tableNode, tui());
                    header = String.join(" | ", labels.stream()
                            .map(x -> tableHeader(x))
                            .toList());

                    if (blockid.isEmpty()) {
                        blockid = "^" + slugify(header
                                .replaceAll("dice: ", "")
                                .replaceAll("d\\d+", "")
                                .replaceAll("</?span.*?>", "")
                                .replace("|", "")
                                .replaceAll("\\s+", " ")
                                .trim());
                    }
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

                final boolean cards = header.contains("Card | ");
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
                                    String result = "";
                                    if (RollFields.exact.existsIn(roll)) {
                                        result = RollFields.exact.getFrom(roll).asText();
                                    } else {
                                        result = RollFields.min.getTextOrEmpty(roll) + "-"
                                                + RollFields.max.getTextOrEmpty(roll);
                                    }
                                    if (cards) {
                                        result += " | " + SourceField.entry.getTextOrEmpty(x);
                                    }
                                    return new TextNode(result);
                                }
                                return x;
                            })
                            .map(x -> flattenToString(x).replace("\n", "<br />"))
                            .collect(Collectors.joining(" | ")) + " |";
                    table.add(row);
                }

                header = "| " + header + " |";
                table.add(0, header.replaceAll("[^|]", "-"));
                table.add(0, header);

                if (!blockid.isBlank()) {
                    table.add(blockid);
                }
                if (header.matches(JsonTextConverter.DICE_TABLE_HEADER) && !blockid.isBlank()) {
                    // prepend a dice roller
                    String targetFile = getFileName();
                    table.add(0, String.format("`dice: [](%s.md#%s)`", targetFile, blockid));
                    table.add(1, "");
                }
                if (!caption.isBlank()) {
                    table.add(0, "");
                    table.add(0, "**" + replaceText(caption) + "**");
                }
            } finally {
                parseState().pop(pushTable);
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
                boolean pushF = parseState().pushFootnotes(true);
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
            String link = keyType.linkify(this, knownEntry);
            return link.matches("\\[.+]\\(.+\\)") ? "!" + link : null;
        }
        return null;
    }

    default void unpackFluffNode(Tools5eIndexType fluffType, JsonNode fluffNode, List<String> text, String heading,
            List<ImageRef> images) {

        boolean pushed = parseState().push(getSources(), fluffNode);
        try {
            if (fluffNode.isArray()) {
                appendToText(text, fluffNode, heading);
            } else {
                appendToText(text, SourceField.entries.getFrom(fluffNode), heading);
            }
        } finally {
            parseState().pop(pushed);
        }

        if (Tools5eFields.images.existsIn(fluffNode)) {
            getImages(Tools5eFields.images.getFrom(fluffNode), images);
        } else if (Tools5eFields.hasFluffImages.booleanOrDefault(fluffNode, false)) {
            String fluffKey = fluffType.createKey(fluffNode);
            fluffNode = index().getOrigin(fluffKey);
            if (fluffNode != null) {
                getImages(Tools5eFields.images.getFrom(fluffNode), images);
            }
        }
    }

    default void getImages(JsonNode imageNode, List<ImageRef> images) {
        if (imageNode != null && imageNode.isArray()) {
            for (Iterator<JsonNode> i = imageNode.elements(); i.hasNext();) {
                ImageRef ir = readImageRef(i.next());
                if (ir != null) {
                    images.add(ir);
                }
            }
        }
    }

    default ImageRef readImageRef(JsonNode imageNode) {
        try {
            JsonMediaHref mediaHref = mapper().treeToValue(imageNode, JsonMediaHref.class);
            return buildImageRef(mediaHref, getImagePath());
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

    default JsonHref readHref(JsonNode href) {
        try {
            return mapper().treeToValue(href, JsonHref.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            tui().errorf(e, "Unable to read href from %s: %s", href, e.toString());
        }
        return null;
    }

    default String asAbilityEnum(JsonNode textNode) {
        return SkillOrAbility.format(textNode.asText(), index(), getSources());
    }

    default String toAlignmentCharacters(String src) {
        return src.replaceAll("\"[A-Z]*[a-z ]+\"", "") // remove notes
                .replaceAll("[^LCNEGAUXY]", ""); // keep only alignment characters
    }

    default String alignmentListToFull(JsonNode alignmentList) {
        if (alignmentList == null) {
            return "";
        }
        boolean allText = streamOf(alignmentList).allMatch(JsonNode::isTextual);
        boolean allObject = streamOf(alignmentList).allMatch(JsonNode::isObject);

        if (allText) {
            return mapAlignmentToString(toAlignmentCharacters(alignmentList.toString()));
        } else if (allObject) {
            return streamOf(alignmentList)
                    .filter(x -> AlignmentFields.alignment.existsIn(x))
                    .map(x -> {
                        if (AlignmentFields.special.existsIn(x)
                                || AlignmentFields.chance.existsIn(x)
                                || AlignmentFields.note.existsIn(x)) {
                            return alignmentObjToFull(x);
                        } else {
                            return alignmentListToFull(x.get("alignment"));
                        }
                    })
                    .collect(Collectors.joining(" or "));

        } else {
            tui().errorf("Unable to parse alignment list from %s", alignmentList);
        }
        return "";
    }

    default String alignmentObjToFull(JsonNode alignmentNode) {
        if (alignmentNode == null) {
            return null;
        }
        if (alignmentNode.isObject()) {
            if (AlignmentFields.special.existsIn(alignmentNode)) {
                return AlignmentFields.special.replaceTextFrom(alignmentNode, index());
            } else {
                String chance = "";
                String note = "";
                if (AlignmentFields.chance.existsIn(alignmentNode)) {
                    chance = String.format(" (%s%%)", AlignmentFields.chance.getFrom(alignmentNode));
                }
                if (AlignmentFields.note.existsIn(alignmentNode)) {
                    note = " (" + AlignmentFields.note.replaceTextFrom(alignmentNode, index()) + ")";
                }
                return String.format("%s%s%s",
                        alignmentObjToFull(AlignmentFields.alignment.getFrom(alignmentNode)),
                        chance, note);
            }
        }
        return mapAlignmentToString(alignmentNode.asText().toUpperCase());
    }

    default String mapAlignmentToString(String a) {
        return switch (a.toUpperCase()) {
            case "A" -> "Any alignment";
            case "C" -> "Chaotic";
            case "CE" -> "Chaotic Evil";
            case "CG" -> "Chaotic Good";
            case "CECG", "CGCE" -> "Chaotic Evil or Chaotic Good";
            case "CGCN" -> "Chaotic Good or Chaotic Neutral";
            case "CGNE" -> "Chaotic Good or Neutral Evil";
            case "CECN" -> "Chaotic Evil or Chaotic Neutral";
            case "CGNYE" -> "Any Chaotic alignment";
            case "CN" -> "Chaotic Neutral";
            case "CENE", "NECE" -> "Chaotic Evil or Neutral Evil";
            case "L" -> "Lawful";
            case "LE" -> "Lawful Evil";
            case "LG" -> "Lawful Good";
            case "LN" -> "Lawful Neutral";
            case "LNCE" -> "Lawful Neutral or Chaotic Evil";
            case "LELG" -> "Lawful Evil or Lawful Good";
            case "LELN", "LNLE" -> "Lawful Evil or Lawful Neutral";
            case "N", "NXNY", "NXNYN", "NNXNYN" -> "Neutral";
            case "NX" -> "Neutral (law/chaos axis)";
            case "NY" -> "Neutral (good/evil axis)";
            case "NE" -> "Neutral Evil";
            case "NG" -> "Neutral Good";
            case "NGNE", "NENG" -> "Neutral Good or Neutral Evil";
            case "G", "LNXCG" -> "Any Good alignment";
            case "E", "CELENE", "LNXCE" -> "Any Evil alignment";
            case "NELE", "LENE" -> "Neutral Evil or Lawful Evil";
            case "LGNYE" -> "Any Non-Chaotic alignment";
            case "LNXCNYE" -> "Any Non-Good alignment";
            case "NXCGNYE" -> "Any Non-Lawful alignment";
            case "NXLGNYE" -> "Any Non-Chaotic alignment";
            case "LNXCNYG", "LNYNXCG" -> "Any Non-Evil alignment";
            case "U" -> "Unaligned";
            default -> {
                tui().errorf("What alignment is this? %s (from %s)", a, getSources());
                yield "Unknown";
            }
        };
    }

    static int levelToPb(int level) {
        // 2 + (¼ * (Level – 1))
        return 2 + ((int) (.25 * (level - 1)));
    }

    default String monsterCr(JsonNode monster) {
        if (monster.has("cr")) {
            JsonNode crNode = Tools5eFields.cr.getFrom(monster);
            if (crNode.isTextual()) {
                return crNode.asText();
            } else if (crNode.has("cr")) {
                return Tools5eFields.cr.getFrom(crNode).asText();
            } else {
                tui().errorf("Unable to parse cr value from %s", crNode.toPrettyString());
            }
        }
        return null;
    }

    default double crToXp(JsonNode cr) {
        if (Tools5eFields.xp.existsIn(cr)) {
            return Tools5eFields.xp.getFrom(cr).asDouble();
        }
        if (Tools5eFields.cr.existsIn(cr)) {
            cr = Tools5eFields.cr.getFrom(cr);
        }
        return XP_CHART_ALT.get(cr.asText());
    }

    default int crToPb(JsonNode cr) {
        if (cr.isTextual()) {
            return crToPb(cr.asText());
        }
        return crToPb(Tools5eFields.cr.getFrom(cr).asText());
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
        JsonNode size = Tools5eFields.size.getFrom(value);
        if (size != null) {
            try {
                if (size.isTextual()) {
                    return sizeToString(size.asText());
                } else if (size.isArray()) {
                    String merged = streamOf(size).map(JsonNode::asText).collect(Collectors.joining());
                    return sizeToString(merged);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        tui().errorf("Unable to parse size for %s from %s", getSources(), size);
        return "Unknown";
    }

    default String spanWrap(String cssClass, String text) {
        return parseState().inTrait()
                ? text
                : "<span class='%s'>%s</span>".formatted(cssClass, text);
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

    static String crToTagValue(String cr) {
        return switch (cr) {
            case "1/8" -> "⅛";
            case "1/4" -> "¼";
            case "1/2" -> "½";
            default -> cr;
        };
    }

    default String asModifier(double value) {
        return (value >= 0 ? "+" : "") + value;
    }

    default String asModifier(int value) {
        return (value >= 0 ? "+" : "") + value;
    }

    default String articleFor(String value) {
        value = leadingNumber.matcher(value).replaceAll((m) -> {
            return numberToText(Integer.parseInt(m.group(1))) + m.group(2);
        });

        return switch (value.toLowerCase().charAt(0)) {
            case 'a', 'e', 'i', 'o', 'u' -> "an";
            default -> "a";
        };
    }

    default String numberToText(int value) {
        int abs = Math.abs(value);
        if (abs >= 100) {
            return "" + value;
        }
        String strValue = switch (abs) {
            case 0 -> "zero";
            case 1 -> "one";
            case 2 -> "two";
            case 3 -> "three";
            case 4 -> "four";
            case 5 -> "five";
            case 6 -> "six";
            case 7 -> "seven";
            case 8 -> "eight";
            case 9 -> "nine";
            case 10 -> "ten";
            case 11 -> "eleven";
            case 12 -> "twelve";
            case 13 -> "thirteen";
            case 14 -> "fourteen";
            case 15 -> "fifteen";
            case 16 -> "sixteen";
            case 17 -> "seventeen";
            case 18 -> "eighteen";
            case 19 -> "nineteen";
            case 20 -> "twenty";
            case 30 -> "thirty";
            case 40 -> "forty";
            case 50 -> "fifty";
            case 60 -> "sixty";
            case 70 -> "seventy";
            case 80 -> "eighty";
            case 90 -> "ninety";
            default -> {
                yield null;
            }
        };

        return strValue == null
                ? String.valueOf(value)
                : String.format("%s%s",
                        value < 0 ? "negative " : "",
                        strValue);
    }

    default String damageTypeToFull(String dmgType) {
        if (!isPresent(dmgType)) {
            return "";
        }
        return switch (dmgType.toUpperCase()) {
            case "A" -> "acid";
            case "B" -> "bludgeoning";
            case "C" -> "cold";
            case "F" -> "fire";
            case "O" -> "force";
            case "L" -> "lightning";
            case "N" -> "necrotic";
            case "P" -> "piercing";
            case "I" -> "poison";
            case "Y" -> "psychic";
            case "R" -> "radiant";
            case "S" -> "slashing";
            case "T" -> "thunder";
            default -> dmgType;
        };
    };

    default String convertCurrency(int cp) {
        List<String> result = new ArrayList<>();
        int gp = cp / 100;
        cp %= 100;
        if (gp > 0) {
            result.add(String.format("%,d gp", gp));
        }
        int sp = cp / 10;
        cp %= 10;
        if (sp > 0) {
            result.add(String.format("%,d sp", sp));
        }
        if (cp > 0) {
            result.add(String.format("%,d cp", cp));
        }
        return String.join(", ", result);
    }

    public static String spellLevelToText(String level) {
        return switch (level) {
            case "0", "c" -> "cantrip";
            case "1" -> "1st-level";
            case "2" -> "2nd-level";
            case "3" -> "3rd-level";
            default -> level + "th-level";
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
        _monsterFluff,
        abbreviation,
        additionalEntries,
        additionalSources,
        alternate,
        amount,
        appliesTo,
        attributes,
        basicRules,
        by,
        className,
        classSource,
        condition, // speed, ac
        count,
        cr,
        data, // statblock, statblockInline
        dataType, // statblockInline
        deck,
        edition,
        entriesTemplate,
        familiar,
        featureType,
        fluff,
        group,
        hasFluff,
        hasFluffImages,
        hasToken,
        id,
        images,
        lairActions, // legendary group
        level,
        number, // speed
        optionalfeature,
        otherSources,
        parentSource,
        prop, // statblock
        race,
        regionalEffects, // legendary group
        shortName,
        size,
        sort, // monsters, vehicles (sorted traits)
        speed,
        srd,
        style,
        subclass,
        subrace,
        tables, // for optfeature types
        tag, // statblock
        template,
        text,
        tokenHref,
        tokenUrl,
        traitTags,
        visible,
        xp,
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
            if (rowData == null || rowData.isNull() || rowData.size() == 0) {
                return "";
            }
            return rowData.get(0).toString();
        }
    }

    enum AlignmentFields implements JsonNodeReader {
        alignment,
        chance,
        note,
        special
    }

    enum AttackFields implements JsonNodeReader {
        attackType,
        attackEntries,
        hitEntries,
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
        statblockInline,

        refClassFeature,
        refOptionalfeature,
        refSubclassFeature,

        // homebrew changes
        homebrew,

        // attack / action entries
        attack,

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
