package dev.ebullient.json5e.tools5e;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.json5e.io.MarkdownWriter;
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

            if (nodeType != type) {
                continue;
            }

            if (type == IndexType.classtype) {
                Json2QuteClass jsonClass = new Json2QuteClass(index, type, jsonSource);
                QuteSource converted = jsonClass.build();
                if (converted != null) {
                    nodes.add(converted);
                    nodes.addAll(jsonClass.buildSubclasses());
                }
            } else {
                QuteSource converted = json2qute(type, jsonSource);
                if (converted != null) {
                    nodes.add(converted);
                }
            }
        }

        try {
            writer.writeFiles(nodes);
        } catch (IOException e) {
            index.tui().error("Exception: " + e.getCause().getMessage());
        }
        return this;
    }

    private QuteSource json2qute(IndexType type, JsonNode jsonNode) {
        switch (type) {
            case background:
                return new Json2QuteBackground(index, type, jsonNode).build();
            case feat:
                return new Json2QuteFeat(index, type, jsonNode).build();
            case item:
                return new Json2QuteItem(index, type, jsonNode).build();
            case monster:
                return new Json2QuteMonster(index, type, jsonNode).build();
            case namelist:
                return new Json2QuteName(index, jsonNode).build();
            case race:
                return new Json2QuteRace(index, type, jsonNode).build();
            case spell:
                return new Json2QuteSpell(index, type, jsonNode).build();
            default:
                throw new IllegalArgumentException("Unsupported type " + type);
        }
    }

    public Json2MarkdownConverter writeRulesAndTables() {
        List<QuteNote> nodes = new ArrayList<>();
        List<JsonNode> tableToRule = new ArrayList<>();

        JsonNode table = index.getRules("table");
        if (table != null) {
            table.forEach(t -> {
                if (t.get("name").asText().equals("Damage Types")) {
                    tableToRule.add(t);
                    return;
                }
                nodes.add(new Table2QuteNote(index, t).build());
            });
        }

        addLootGroup(nodes, index.getRules("magicItems"), "Magic Item Tables");
        addLootGroup(nodes, index.getRules("artObjects"), "Art Objects");
        addLootGroup(nodes, index.getRules("gems"), "Gems");

        try {
            writer.writeNotes(index.compendiumRoot() + "tables/", nodes);
        } catch (IOException e) {
            index.tui().error("Exception: " + e.getCause().getMessage());
        }
        nodes.clear();

        tableToRule.forEach(t -> nodes.add(new Table2QuteNote(index, t).buildRules()));

        addRule(nodes, index.getRules("disease"), "Diseases");
        addRule(nodes, index.getRules("skill"), "Skills");
        addRule(nodes, index.getRules("sense"), "Senses");
        addRule(nodes, index.getRules("condition"), "Conditions");
        addRule(nodes, index.getRules("status"), "Status");
        addItemProperties(nodes, index.getRules("itemProperty"));

        try {
            writer.writeNotes(index.rulesRoot(), nodes);
        } catch (IOException e) {
            index.tui().error("Exception: " + e.getCause().getMessage());
        }

        return this;
    }

    private void addLootGroup(List<QuteNote> nodes, JsonNode element, String title) {
        if (element == null || element.isNull()) {
            return;
        }
        QuteNote note = new Sourceless2QuteNote(index, element, title).buildLoot();
        if (note != null) {
            nodes.add(note);
        }
    }

    private void addRule(List<QuteNote> nodes, JsonNode element, String title) {
        if (element == null || element.isNull()) {
            return;
        }
        QuteNote note = new Sourceless2QuteNote(index, element, title).build();
        if (note != null) {
            nodes.add(note);
        }
    }

    private void addItemProperties(List<QuteNote> nodes, JsonNode element) {
        if (element == null || element.isNull()) {
            return;
        }
        QuteNote note = new Sourceless2QuteNote(index, element, "Item Properties").buildItemProperties();
        if (note != null) {
            nodes.add(note);
        }
    }
}
