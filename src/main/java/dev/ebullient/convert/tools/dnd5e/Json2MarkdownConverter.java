package dev.ebullient.convert.tools.dnd5e;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.qute.QuteNote;
import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.MarkdownConverter;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.dnd5e.qute.QuteName;

public class Json2MarkdownConverter implements MarkdownConverter {
    final Tools5eIndex index;
    final MarkdownWriter writer;
    final Map<String, String> fallbackPaths;

    public Json2MarkdownConverter(Tools5eIndex index, MarkdownWriter writer, Map<String, String> fallbackPaths) {
        this.index = index;
        this.writer = writer;
        this.fallbackPaths = fallbackPaths;
    }

    public Json2MarkdownConverter writeAll() {
        return writeFiles(List.of(
                Tools5eIndexType.background,
                Tools5eIndexType.classtype,
                Tools5eIndexType.deity,
                Tools5eIndexType.feat,
                Tools5eIndexType.item,
                Tools5eIndexType.monster,
                Tools5eIndexType.race,
                Tools5eIndexType.spell));
    }

    public Json2MarkdownConverter writeImages() {
        index.tui().copyImages(Tools5eSources.getImages(), fallbackPaths);
        return this;
    }

    public Json2MarkdownConverter writeFiles(IndexType type) {
        return writeFiles(List.of(type));
    }

    public Json2MarkdownConverter writeFiles(List<? extends IndexType> types) {
        if (index.notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing files");
        }

        List<QuteBase> sources = new ArrayList<>();
        for (Entry<String, JsonNode> e : index.includedEntries()) {
            Tools5eIndexType nodeType = Tools5eIndexType.getTypeFromKey(e.getKey());
            JsonNode jsonSource = e.getValue();

            if (types.contains(Tools5eIndexType.race) && nodeType == Tools5eIndexType.subrace) {
                // include these, too
            } else if (!types.contains(nodeType)) {
                continue;
            }

            if (nodeType == Tools5eIndexType.classtype) {
                Json2QuteClass jsonClass = new Json2QuteClass(index, nodeType, jsonSource);
                QuteBase converted = jsonClass.build();
                if (converted != null) {
                    sources.add(converted);
                    sources.addAll(jsonClass.buildSubclasses());
                }
            } else if (nodeType == Tools5eIndexType.race || nodeType == Tools5eIndexType.subrace) {
                QuteBase converted = new Json2QuteRace(index, nodeType, jsonSource).build();
                if (converted != null) {
                    sources.add(converted);
                }
            } else {
                QuteBase converted = json2qute(nodeType, jsonSource);
                if (converted != null) {
                    sources.add(converted);
                }
            }
        }

        writer.writeFiles(index.compendiumFilePath(), sources);
        return this;
    }

    private QuteBase json2qute(Tools5eIndexType type, JsonNode jsonSource) {
        switch (type) {
            case background:
                return new Json2QuteBackground(index, type, jsonSource).build();
            case deity:
                return new Json2QuteDeity(index, type, jsonSource).build();
            case feat:
                return new Json2QuteFeat(index, type, jsonSource).build();
            case item:
                return new Json2QuteItem(index, type, jsonSource).build();
            case monster:
                return new Json2QuteMonster(index, type, jsonSource).build();
            case spell:
                return new Json2QuteSpell(index, type, jsonSource).build();
            default:
                throw new IllegalArgumentException("Unsupported type " + type);
        }
    }

    public Json2MarkdownConverter writeNotesAndTables() {
        Map<String, QuteNote> adventures = new HashMap<>();
        Map<String, QuteNote> books = new HashMap<>();
        List<QuteName> names = new ArrayList<>();
        List<QuteNote> rules = new ArrayList<>();
        List<QuteNote> tables = new ArrayList<>();
        List<QuteNote> variants = new ArrayList<>();

        final Map<String, JsonNode> ruleIndex = index.getRules();
        final JsonNode srdEntries = TtrpgConfig.activeGlobalConfig("srdEntries");

        for (Entry<String, JsonNode> entry : ruleIndex.entrySet()) {
            String key = entry.getKey();
            JsonNode node = entry.getValue();
            if (node.isNull()) {
                continue;
            }
            if (key.startsWith("book-")) { // bookdata
                addReference(books, node);
            } else if (key.startsWith("adventure-")) { // adventuredata
                addReference(adventures, node);
            } else if (key.startsWith("names-")) {
                addNames(names, key, node);
            } else if (key.equals("table")) {
                for (JsonNode t : index.iterableElements(node)) {
                    if (t.get("name").asText().equals("Damage Types")) {
                        rules.add(new Table2QuteNote(index, t).buildRules());
                        continue;
                    }
                    addTable(tables, t);
                }
            } else if (key.equals("variantrule")) {
                for (JsonNode vr : index.iterableElements(node)) {
                    String title = index.replaceText(vr.get("name").asText());
                    QuteNote note = new Sourceless2QuteNote(index, vr, title).buildVariant();
                    if (note != null) {
                        variants.add(note);
                    }
                }
            } else {
                switch (key) {
                    case "action":
                        addActions(rules, node);
                        break;
                    case "magicItems":
                        addLootGroup(tables, node, "Magic Item Tables");
                        break;
                    case "artObjects":
                        addLootGroup(tables, node, "Art Objects");
                        break;
                    case "gems":
                        addLootGroup(tables, node, "Gems");
                        break;
                    case "condition":
                        addRule(rules, node, "Conditions");
                        break;
                    case "disease":
                        addRule(rules, node, "Diseases");
                        break;
                    case "sense":
                        addRule(rules, node, "Senses");
                        break;
                    case "skill":
                        addRule(rules, node, "Skills");
                        break;
                    case "status":
                        addRule(rules, node, "Status");
                        break;
                }
            }
        }

        addItemProperties(rules, srdEntries.get("properties"));

        Path rulesPath = index.rulesFilePath();
        Path compendiumPath = index.compendiumFilePath();

        if (!Json2QuteBackground.traits.isEmpty()) {
            List<QuteNote> notes = new BackgroundTraits2Note(index).buildNotes();
            tables.addAll(notes);
        }
        if (!names.isEmpty()) {
            writer.writeNames(compendiumPath.resolve("tables/"), names);
        }
        if (!adventures.isEmpty()) {
            writer.writeNotes(compendiumPath.resolve("adventures/"), adventures);
        }
        if (!books.isEmpty()) {
            writer.writeNotes(compendiumPath.resolve("books/"), books);
        }
        writer.writeNotes(compendiumPath.resolve("tables/"), tables);

        writer.writeNotes(rulesPath, rules);
        writer.writeNotes(rulesPath.resolve("variant-rules/"), variants);
        return this;
    }

    private void addActions(List<QuteNote> notes, JsonNode element) {
        QuteNote note = new Sourceless2QuteNote(index, element, "Actions").buildActions();
        if (note != null) {
            notes.add(note);
        }
    }

    private void addItemProperties(List<QuteNote> notes, JsonNode element) {
        QuteNote note = new Sourceless2QuteNote(index, element, "Item Properties").buildItemProperties();
        if (note != null) {
            notes.add(note);
        }
    }

    private void addLootGroup(List<QuteNote> notes, JsonNode element, String title) {
        QuteNote note = new Sourceless2QuteNote(index, element, title).buildLoot();
        if (note != null) {
            notes.add(note);
        }
    }

    private void addReference(Map<String, QuteNote> notes, JsonNode element) {
        String key = TtrpgValue.indexKey.getFromNode(element);
        if (!element.has("data")) {
            index.tui().errorf("No data for %s", key);
            return;
        }
        String metadataKey = key.replace("data|", "|");
        if (index.isExcluded(metadataKey)) {
            index.tui().debugf("%s is excluded", metadataKey);
            return;
        }
        JsonNode metadata = index.getOrigin(metadataKey);
        if (metadata == null) {
            index.tui().errorf("Unable to find metadata (%s) for %s", metadataKey, key);
            return;
        }
        String title = index.replaceText(metadata.get("name").asText());
        Map<String, QuteNote> contents = new Sourceless2QuteNote(index, metadata, title).buildReference(element.get("data"));
        notes.putAll(contents);
    }

    private void addNames(List<QuteName> names, String key, JsonNode element) {
        QuteName nameTable = new Json2QuteName(index, element).buildNames();
        if (nameTable != null) {
            names.add(nameTable);
        }
    }

    private void addRule(List<QuteNote> notes, JsonNode element, String title) {
        QuteNote note = new Sourceless2QuteNote(index, element, title).buildNote();
        if (note != null) {
            notes.add(note);
        }
    }

    private void addTable(List<QuteNote> notes, JsonNode table) {
        QuteNote n = new Table2QuteNote(index, table).buildNote();
        if (n != null) {
            notes.add(n);
        }
    }
}
