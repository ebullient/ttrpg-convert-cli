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
import dev.ebullient.convert.tools.dnd5e.Tools5eIndex.OptionalFeatureType;
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
                .filter(Tools5eIndexType::writeFile)
                .collect(Collectors.toList()));
    }

    public Tools5eMarkdownConverter writeNotesAndTables() {
        return writeFiles(Stream.of(Tools5eIndexType.values())
                .filter(x -> !x.writeFile())
                .filter(Tools5eIndexType::useQuteNote)
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
        if (types != null) {
            writeQuteBaseFiles(types.stream()
                    .filter(x -> !((Tools5eIndexType) x).useQuteNote())
                    .toList());
            writeQuteNoteFiles(types.stream()
                    .filter(x -> ((Tools5eIndexType) x).useQuteNote())
                    .toList());
        }
        return this;
    }

    private void writeQuteBaseFiles(List<? extends IndexType> types) {
        if (types.isEmpty()) {
            return;
        }

        List<QuteBase> compendium = new ArrayList<>();
        List<QuteBase> rules = new ArrayList<>();

        for (Entry<String, JsonNode> e : index.includedEntries()) {
            final String key = e.getKey();
            final JsonNode jsonSource = e.getValue();

            Tools5eIndexType nodeType = Tools5eIndexType.getTypeFromKey(key);
            if (types.contains(Tools5eIndexType.race) && nodeType == Tools5eIndexType.subrace) {
                // include subrace with race
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
    }

    private QuteBase json2qute(Tools5eIndexType type, JsonNode jsonSource) {
        return switch (type) {
            case background -> new Json2QuteBackground(index, type, jsonSource).build();
            case deity -> new Json2QuteDeity(index, type, jsonSource).build();
            case feat -> new Json2QuteFeat(index, type, jsonSource).build();
            case hazard, trap -> new Json2QuteHazard(index, type, jsonSource).build();
            case item -> new Json2QuteItem(index, type, jsonSource).build();
            case monster -> new Json2QuteMonster(index, type, jsonSource).build();
            case object -> new Json2QuteObject(index, type, jsonSource).build();
            case optionalfeature -> new Json2QuteOptionalFeature(index, type, jsonSource).build();
            case psionic -> new Json2QutePsionicTalent(index, type, jsonSource).build();
            case race, subrace -> new Json2QuteRace(index, type, jsonSource).build();
            case reward -> new Json2QuteReward(index, type, jsonSource).build();
            case spell -> new Json2QuteSpell(index, type, jsonSource).build();
            case vehicle -> new Json2QuteVehicle(index, type, jsonSource).build();
            default -> throw new IllegalArgumentException("Unsupported type " + type);
        };
    }

    private void writeQuteNoteFiles(List<? extends IndexType> types) {
        final String vrDir = Tools5eIndexType.variantrule.getRelativePath();

        List<QuteNote> compendium = new ArrayList<>();
        List<QuteNote> rules = new ArrayList<>();

        Map<Tools5eIndexType, Json2QuteCommon> combinedDocs = new HashMap<>();

        for (Entry<String, JsonNode> e : index.includedEntries()) {
            final String key = e.getKey();
            final JsonNode node = e.getValue();

            Tools5eIndexType nodeType = Tools5eIndexType.getTypeFromKey(key);
            if (!types.contains(nodeType)) {
                continue;
            }

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
                        compendium.addAll(new Json2QuteBook(index, nodeType, metadata, node).buildBook());
                    } else {
                        index.tui().debugf("%s is excluded", metadataKey);
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
                case optionalFeatureTypes -> {
                    OptionalFeatureType oft = index.getOptionalFeatureTypes(node);
                    QuteNote converted = new Json2QuteOptionalFeatureType(index, node, oft).buildNote();
                    if (converted != null) {
                        compendium.add(converted);
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
                        rules.add(tableNote);
                    } else {
                        compendium.add(tableNote);
                    }
                }
                case variantrule -> append(nodeType,
                        new Json2QuteNote(index, nodeType, node)
                                .useSuffix(true)
                                .withImagePath(vrDir)
                                .buildNote()
                                .withTargetPath(vrDir),
                        compendium, rules);
                default -> {
                    // skip it
                }
            }
        }

        for (Json2QuteCommon value : combinedDocs.values()) {
            append(value.type, value.buildNote(), compendium, rules);
        }

        if (!Json2QuteBackground.traits.isEmpty()) {
            compendium.addAll(new BackgroundTraits2Note(index).buildNotes());
        }

        writer.writeNotes(index.compendiumFilePath(), compendium, true);
        writer.writeNotes(index.rulesFilePath(), rules, false);
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
