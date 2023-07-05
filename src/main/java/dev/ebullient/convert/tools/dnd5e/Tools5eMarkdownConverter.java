package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.qute.QuteNote;
import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.MarkdownConverter;
import dev.ebullient.convert.tools.dnd5e.qute.QuteName;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteNote;

public class Tools5eMarkdownConverter implements MarkdownConverter {
    final Tools5eIndex index;
    final MarkdownWriter writer;
    final Map<String, String> fallbackPaths;

    public Tools5eMarkdownConverter(Tools5eIndex index, MarkdownWriter writer, Map<String, String> fallbackPaths) {
        this.index = index;
        this.writer = writer;
        this.fallbackPaths = fallbackPaths;
    }

    public Tools5eMarkdownConverter writeAll() {
        return writeFiles(Stream.of(Tools5eIndexType.values())
                .filter(x -> x.writeFile())
                .collect(Collectors.toList()));
    }

    public Tools5eMarkdownConverter writeNotesAndTables() {
        return writeFiles(Stream.of(Tools5eIndexType.values())
                .filter(x -> !x.writeFile())
                .filter(x -> x.useQuteNote())
                .collect(Collectors.toList()));
    }

    public Tools5eMarkdownConverter writeImages() {
        index.tui().copyImages(Tools5eSources.getImages(), fallbackPaths);
        return this;
    }

    public Tools5eMarkdownConverter writeFiles(IndexType type) {
        return writeFiles(List.of(type));
    }

    public Tools5eMarkdownConverter writeFiles(List<? extends IndexType> types) {
        if (index.notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing files");
        }
        if (types == null) {
        } else {
            writeQuteBaseFiles(types.stream()
                    .filter(x -> !((Tools5eIndexType) x).useQuteNote())
                    .collect(Collectors.toList()));
            writeQuteNoteFiles(types.stream()
                    .filter(x -> ((Tools5eIndexType) x).useQuteNote())
                    .collect(Collectors.toList()));
        }
        return this;
    }

    private Tools5eMarkdownConverter writeQuteBaseFiles(List<? extends IndexType> types) {
        if (types.isEmpty()) {
            return this;
        }

        List<QuteBase> compendium = new ArrayList<>();
        List<QuteBase> rules = new ArrayList<>();

        for (Entry<String, JsonNode> e : index.includedEntries()) {
            final String key = e.getKey();
            final JsonNode jsonSource = e.getValue();

            Tools5eIndexType nodeType = Tools5eIndexType.getTypeFromKey(key);
            if (types.contains(Tools5eIndexType.race) && nodeType == Tools5eIndexType.subrace) {
                // include these, too
            } else if (!types.contains(nodeType)) {
                continue;
            }

            if (nodeType == Tools5eIndexType.classtype) {
                Json2QuteClass jsonClass = new Json2QuteClass(index, nodeType, jsonSource);
                QuteBase converted = jsonClass.build();
                if (converted != null) {
                    compendium.add(converted);
                    compendium.addAll(jsonClass.buildSubclasses());
                }
            } else {
                QuteBase converted = json2qute(nodeType, jsonSource);
                if (converted != null) {
                    append(nodeType, converted, compendium, rules);
                }
            }
        }

        writer.writeFiles(index.compendiumFilePath(), compendium);
        writer.writeFiles(index.rulesFilePath(), rules);
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
            case race:
                return new Json2QuteRace(index, type, jsonSource).build();
            case spell:
                return new Json2QuteSpell(index, type, jsonSource).build();
            case subrace:
                return new Json2QuteRace(index, type, jsonSource).build();
            default:
                throw new IllegalArgumentException("Unsupported type " + type);
        }
    }

    private Tools5eMarkdownConverter writeQuteNoteFiles(List<? extends IndexType> types) {

        List<QuteNote> compendium = new ArrayList<>();
        List<QuteNote> rules = new ArrayList<>();
        List<QuteName> names = new ArrayList<>();

        Map<Tools5eIndexType, Json2QuteCommon> combinedDocs = new HashMap<>();

        for (Entry<String, JsonNode> e : index.includedEntries()) {
            final String key = e.getKey();
            final JsonNode node = e.getValue();

            Tools5eIndexType nodeType = Tools5eIndexType.getTypeFromKey(key);
            if (!types.contains(nodeType)) {
                continue;
            }

            switch (nodeType) {
                case action:
                    Json2QuteCompose action = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                            t -> new Json2QuteCompose(nodeType, index, "Actions"));
                    action.add(node);
                    break;
                case adventureData:
                case bookData: {
                    String metadataKey = key.replace("data|", "|");
                    JsonNode metadata = index.getOrigin(metadataKey);
                    if (!node.has("data")) {
                        index.tui().errorf("No data for %s", key);
                    } else if (metadata == null) {
                        index.tui().errorf("Unable to find metadata (%s) for %s", metadataKey, key);
                    } else if (index.isIncluded(metadataKey)) {
                        compendium.addAll(new Json2QuteBook(index, nodeType, metadata, node).buildBook());
                    } else {
                        index.tui().debugf("%s is excluded", metadataKey);
                    }
                    break;
                }
                case artObjects:
                    Json2QuteCompose artObjects = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                            t -> new Json2QuteCompose(nodeType, index, "Art Objects", Tools5eQuteBase.TABLES_PATH));
                    artObjects.add(node);
                    break;
                case condition:
                    Json2QuteCompose conditions = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                            t -> new Json2QuteCompose(nodeType, index, "Conditions"));
                    conditions.add(node);
                    break;
                case disease:
                    Json2QuteCompose disease = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                            t -> new Json2QuteCompose(nodeType, index, "Diseases"));
                    disease.add(node);
                    break;
                case gems:
                    Json2QuteCompose gems = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                            t -> new Json2QuteCompose(nodeType, index, "Gems", Tools5eQuteBase.TABLES_PATH));
                    gems.add(node);
                    break;
                case itemProperty:
                    Json2QuteCompose itemProperty = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                            t -> new Json2QuteCompose(nodeType, index, "Item Properties"));
                    itemProperty.add(node);
                    break;
                case itemType:
                    Json2QuteCompose itemTypes = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                            t -> new Json2QuteCompose(nodeType, index, "Item Types"));
                    itemTypes.add(node);
                    break;
                case magicItems:
                    Json2QuteCompose magicItems = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                            t -> new Json2QuteCompose(nodeType, index, "Magic Item Tables", Tools5eQuteBase.TABLES_PATH));
                    magicItems.add(node);
                    break;
                case nametable:
                    QuteName nameTable = new Json2QuteName(index, node).buildNames();
                    if (nameTable != null) {
                        names.add(nameTable);
                    }
                    break;
                case sense:
                    Json2QuteCompose sense = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                            t -> new Json2QuteCompose(nodeType, index, "Senses"));
                    sense.add(node);
                    break;
                case skill:
                    Json2QuteCompose skill = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                            t -> new Json2QuteCompose(nodeType, index, "Skills"));
                    skill.add(node);
                    break;
                case status:
                    Json2QuteCompose status = (Json2QuteCompose) combinedDocs.computeIfAbsent(nodeType,
                            t -> new Json2QuteCompose(nodeType, index, "Status"));
                    status.add(node);
                    break;
                case table:
                    Tools5eQuteNote tableNote = new Json2QuteTable(index, node).buildNote();
                    if (tableNote.getName().equals("Damage Types")) {
                        rules.add(tableNote);
                    } else {
                        compendium.add(tableNote);
                    }
                    break;
                case variantrule:
                    append(nodeType,
                            new Json2QuteNote(index, nodeType, node)
                                    .useSuffix(true)
                                    .withImagePath(Tools5eQuteBase.VR_PATH)
                                    .buildNote()
                                    .withTargetPath(Tools5eQuteBase.VR_PATH),
                            compendium, rules);
                    break;
                default:
                    break; // skip it
            }
        }

        for (Json2QuteCommon value : combinedDocs.values()) {
            append(value.type, value.buildNote(), compendium, rules);
        }

        if (!Json2QuteBackground.traits.isEmpty()) {
            compendium.addAll(new BackgroundTraits2Note(index).buildNotes());
        }

        writer.writeNames(index.compendiumFilePath().resolve(Tools5eQuteBase.TABLES_PATH), names);
        writer.writeNotes(index.compendiumFilePath(), compendium, true);
        writer.writeNotes(index.rulesFilePath(), rules, false);

        return this;
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
