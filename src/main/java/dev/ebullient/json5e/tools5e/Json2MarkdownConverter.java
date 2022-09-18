package dev.ebullient.json5e.tools5e;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.json5e.io.MarkdownWriter;
import dev.ebullient.json5e.qute.QuteName;
import dev.ebullient.json5e.qute.QuteNote;
import dev.ebullient.json5e.qute.QuteSource;

public class Json2MarkdownConverter {
    final JsonIndex index;
    final MarkdownWriter writer;

    public Json2MarkdownConverter(JsonIndex index, MarkdownWriter writer) {
        this.index = index;
        this.writer = writer;
    }

    public Json2MarkdownConverter writeFiles(IndexType type) {
        if (index.notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing files");
        }

        List<QuteSource> nodes = new ArrayList<>();
        for (Entry<String, JsonNode> e : index.includedEntries()) {
            IndexType nodeType = IndexType.getTypeFromKey(e.getKey());
            JsonNode jsonSource = e.getValue();

            if (type == IndexType.race && nodeType == IndexType.subrace) {
                // include these, too
            } else if (nodeType != type) {
                continue;
            }

            if (type == IndexType.classtype) {
                Json2QuteClass jsonClass = new Json2QuteClass(index, type, jsonSource);
                QuteSource converted = jsonClass.build();
                if (converted != null) {
                    nodes.add(converted);
                    nodes.addAll(jsonClass.buildSubclasses());
                }
            } else if (type == IndexType.race || type == IndexType.subrace) {
                QuteSource converted = new Json2QuteRace(index, type, jsonSource).build();
                if (converted != null) {
                    nodes.add(converted);
                }
            } else {
                QuteSource converted = json2qute(type, jsonSource);
                if (converted != null) {
                    nodes.add(converted);
                }
            }
        }

        writer.writeFiles(nodes, type.toString(), index.compendiumPath());
        return this;
    }

    private QuteSource json2qute(IndexType type, JsonNode jsonSource) {
        switch (type) {
            case background:
                return new Json2QuteBackground(index, type, jsonSource).build();
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

    public Json2MarkdownConverter writeRulesAndTables() {
        Map<String, QuteNote> adventures = new HashMap<>();
        Map<String, QuteNote> books = new HashMap<>();
        List<QuteName> names = new ArrayList<>();
        List<QuteNote> rules = new ArrayList<>();
        List<QuteNote> tables = new ArrayList<>();
        List<QuteNote> variants = new ArrayList<>();

        for (Entry<String, JsonNode> entry : index.getRules().entrySet()) {
            String key = entry.getKey();
            JsonNode node = entry.getValue();
            if (node.isNull()) {
                continue;
            }
            if (key.startsWith("book-")) {
                addReference(books, key, node);
            } else if (key.startsWith("adventure-")) {
                addReference(adventures, key, node);
            } else if (key.startsWith("names-")) {
                addNames(names, key, node);
            } else if (key.equals("table")) {
                node.forEach(t -> {
                    if (t.get("name").asText().equals("Damage Types")) {
                        rules.add(new Table2QuteNote(index, t).buildRules());
                        return;
                    }
                    addTable(tables, t);
                });
            } else if (key.equals("variantrule")) {
                node.forEach(vr -> {
                    String title = index.replaceText(vr.get("name").asText());
                    QuteNote note = new Sourceless2QuteNote(index, vr, title).buildVariant();
                    if (note != null) {
                        variants.add(note);
                    }
                });
            } else {
                switch (key) {
                    case "action":
                        addActions(rules, node);
                        break;
                    case "itemProperty":
                        addItemProperties(rules, node);
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

        Path rulesPath = index.rulesPath();
        Path compendiumPath = index.compendiumPath();

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

    private void addReference(Map<String, QuteNote> notes, String key, JsonNode element) {
        if (!element.has("data")) {
            index.tui().errorf("No data for %s", key);
            return;
        }
        String indexKey = index.getDataKey(key);
        if (index.isExcluded(indexKey)) {
            index.tui().debugf("%s is excluded", indexKey);
            return;
        }
        JsonNode metadata = index.getOrigin(indexKey);
        if (metadata == null) {
            index.tui().errorf("Unable to find metadata for %s", indexKey);
            return;
        }
        String title = index.replaceText(metadata.get("name").asText());
        Map<String, QuteNote> contents = new Sourceless2QuteNote(index, metadata, title).buildReference(element.get("data"));
        notes.putAll(contents);
    }

    private void addNames(List<QuteName> names, String key, JsonNode element) {
        QuteName nameTable = new Json2QuteName(index, element).build();
        if (nameTable != null) {
            names.add(nameTable);
        }
    }

    private void addRule(List<QuteNote> notes, JsonNode element, String title) {
        QuteNote note = new Sourceless2QuteNote(index, element, title).build();
        if (note != null) {
            notes.add(note);
        }
    }

    private void addTable(List<QuteNote> notes, JsonNode table) {
        QuteNote n = new Table2QuteNote(index, table).build();
        if (n != null) {
            notes.add(n);
        }
    }

}
