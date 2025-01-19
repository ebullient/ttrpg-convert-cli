package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.qute.QuteNote;
import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.MarkdownConverter;
import dev.ebullient.convert.tools.dnd5e.OptionalFeatureIndex.OptionalFeatureType;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteNote;

public class Tools5eMarkdownConverter implements MarkdownConverter {
    final Tools5eIndex index;
    final MarkdownWriter writer;

    public Tools5eMarkdownConverter(Tools5eIndex index, MarkdownWriter writer) {
        this.index = index;
        this.writer = writer;
    }

    public Tools5eMarkdownConverter writeAll() {
        return writeFiles(List.of(Tools5eIndexType.values()));
    }

    public Tools5eMarkdownConverter writeImages() {
        index.tui().progressf("Writing images and fonts");
        index.tui().copyImages(Tools5eSources.getImages());
        index.tui().copyFonts(Tools5eSources.getFonts());
        return this;
    }

    public Tools5eMarkdownConverter writeFiles(IndexType type) {
        return writeFiles(List.of(type));
    }

    static class WritingQueue {
        List<QuteBase> baseCompendium = new ArrayList<>();
        List<QuteBase> baseRules = new ArrayList<>();
        List<QuteNote> noteCompendium = new ArrayList<>();
        List<QuteNote> noteRules = new ArrayList<>();

        // Some state for combining notes
        Map<Tools5eIndexType, Json2QuteCommon> combinedDocs = new HashMap<>();
    }

    public Tools5eMarkdownConverter writeFiles(List<? extends IndexType> types) {
        if (index.notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing files");
        }
        if (types == null || types.isEmpty()) {
            return this;
        }
        index.tui().progressf("Converting data: %s", types);

        WritingQueue queue = new WritingQueue();
        for (var entry : index.includedEntries()) {
            final String key = entry.getKey();
            final JsonNode jsonSource = entry.getValue();

            Tools5eIndexType nodeType = Tools5eIndexType.getTypeFromKey(key);
            if (types.contains(Tools5eIndexType.race) && nodeType == Tools5eIndexType.subrace) {
                // include subrace with race
            } else if (!types.contains(nodeType)) {
                continue;
            }

            if (nodeType.writeFile()) {
                writeQuteBaseFiles(nodeType, key, jsonSource, queue);
            } else if (nodeType.isOutputType() && nodeType.useQuteNote()) {
                writeQuteNoteFiles(nodeType, key, jsonSource, queue);
            }
        }

        writer.writeFiles(index.compendiumFilePath(), queue.baseCompendium);
        writer.writeFiles(index.rulesFilePath(), queue.baseRules);

        for (Json2QuteCommon value : queue.combinedDocs.values()) {
            append(value.type, value.buildNote(), queue.noteCompendium, queue.noteRules);
        }

        if (!Json2QuteBackground.traits.isEmpty()) {
            queue.noteCompendium.addAll(new BackgroundTraits2Note(index).buildNotes());
        }

        writer.writeNotes(index.compendiumFilePath(), queue.noteCompendium, true);
        writer.writeNotes(index.rulesFilePath(), queue.noteRules, false);

        return this;
    }

    private void writeQuteBaseFiles(Tools5eIndexType type, String key, JsonNode jsonSource, WritingQueue queue) {
        var compendium = queue.baseCompendium;
        var rules = queue.baseRules;
        if (type == Tools5eIndexType.classtype) {
            Json2QuteClass jsonClass = new Json2QuteClass(index, type, jsonSource);
            QuteBase converted = jsonClass.build();
            if (converted != null) {
                compendium.add(converted);
                compendium.addAll(jsonClass.buildSubclasses());
            }
        } else {
            QuteBase converted = switch (type) {
                case background -> new Json2QuteBackground(index, type, jsonSource).build();
                case deck -> new Json2QuteDeck(index, type, jsonSource).build();
                case deity -> new Json2QuteDeity(index, type, jsonSource).build();
                case facility -> new Json2QuteBastion(index, type, jsonSource).build();
                case feat -> new Json2QuteFeat(index, type, jsonSource).build();
                case hazard, trap -> new Json2QuteHazard(index, type, jsonSource).build();
                case item, itemGroup -> new Json2QuteItem(index, type, jsonSource).build();
                case monster -> new Json2QuteMonster(index, type, jsonSource).build();
                case object -> new Json2QuteObject(index, type, jsonSource).build();
                case optfeature -> new Json2QuteOptionalFeature(index, type, jsonSource).build();
                case psionic -> new Json2QutePsionicTalent(index, type, jsonSource).build();
                case race, subrace -> new Json2QuteRace(index, type, jsonSource).build();
                case reward -> new Json2QuteReward(index, type, jsonSource).build();
                case spell -> new Json2QuteSpell(index, type, jsonSource).build();
                case vehicle -> new Json2QuteVehicle(index, type, jsonSource).build();
                default -> throw new IllegalArgumentException("Unsupported type " + type);
            };
            if (converted != null) {
                append(type, converted, compendium, rules);
            }
        }
    }

    private void writeQuteNoteFiles(Tools5eIndexType nodeType, String key, JsonNode node, WritingQueue queue) {
        var compendiumDocs = queue.noteCompendium;
        var ruleDocs = queue.noteRules;
        var combinedDocs = queue.combinedDocs;
        final var vrDir = Tools5eIndexType.variantrule.getRelativePath();

        switch (nodeType) {
            case action -> {
                Json2QuteCompose action = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                        t -> new Json2QuteCompose(nodeType, index, "Actions"));
                action.add(node);
            }
            case adventureData, bookData -> {
                String metadataKey = key.replace("data|", "|");
                JsonNode metadata = index.getOrigin(metadataKey);
                if (!node.has("data")) {
                    index.tui().errorf("No data for %s", key);
                } else if (metadata == null) {
                    index.tui().errorf("Unable to find metadata (%s) for %s", metadataKey, key);
                } else if (index.isIncluded(metadataKey)) {
                    compendiumDocs.addAll(new Json2QuteBook(index, nodeType, metadata, node).buildBook());
                } else {
                    index.tui().debugf(Msg.FILTER, "%s is excluded", metadataKey);
                }
            }
            case status, condition -> {
                Json2QuteCompose conditions = (Json2QuteCompose) combinedDocs.computeIfAbsent(
                        Tools5eIndexType.condition,
                        t -> new Json2QuteCompose(nodeType, index, "Conditions"));
                conditions.add(node);
            }
            case disease -> {
                Json2QuteCompose disease = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                        t -> new Json2QuteCompose(nodeType, index, "Diseases"));
                disease.add(node);
            }
            case itemMastery -> {
                Json2QuteCompose itemMastery = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                        t -> new Json2QuteCompose(nodeType, index, "Item Mastery"));
                itemMastery.add(node);
            }
            case itemProperty -> {
                Json2QuteCompose itemProperty = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                        t -> new Json2QuteCompose(nodeType, index, "Item Properties"));
                itemProperty.add(node);
            }
            case itemType -> {
                Json2QuteCompose itemTypes = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                        t -> new Json2QuteCompose(nodeType, index, "Item Types"));
                itemTypes.add(node);
            }
            case legendaryGroup -> {
                QuteNote converted = new Json2QuteLegendaryGroup(index, nodeType, node).buildNote();
                if (converted != null) {
                    compendiumDocs.add(converted);
                }
            }
            case optionalFeatureTypes -> {
                OptionalFeatureType oft = index.getOptionalFeatureType(node);
                if (oft == null) {
                    index.tui().errorf("Unable to find optional feature type for %s", key);
                    return;
                }
                QuteNote converted = new Json2QuteOptionalFeatureType(index, node, oft).buildNote();
                if (converted != null) {
                    compendiumDocs.add(converted);
                }
            }
            case sense -> {
                Json2QuteCompose sense = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                        t -> new Json2QuteCompose(nodeType, index, "Senses"));
                sense.add(node);
            }
            case skill -> {
                Json2QuteCompose skill = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                        t -> new Json2QuteCompose(nodeType, index, "Skills"));
                skill.add(node);
            }
            case table, tableGroup -> {
                Tools5eQuteNote tableNote = new Json2QuteTable(index, nodeType, node).buildNote();
                if (tableNote.getName().equals("Damage Types")) {
                    ruleDocs.add(tableNote);
                } else {
                    compendiumDocs.add(tableNote);
                }
            }
            case variantrule -> append(nodeType,
                    new Json2QuteNote(index, nodeType, node)
                            .useSuffix(true)
                            .withImagePath(vrDir)
                            .buildNote()
                            .withTargetPath(vrDir),
                    compendiumDocs, ruleDocs);
            default -> {
                // skip it
            }
        }
    }

    <T extends QuteBase> void append(Tools5eIndexType type, T note, List<T> compendium, List<T> rules) {
        if (note != null) {
            if (type.useCompendiumBase()) {
                compendium.add(note);
            } else {
                rules.add(note);
            }
        }
    }
}
