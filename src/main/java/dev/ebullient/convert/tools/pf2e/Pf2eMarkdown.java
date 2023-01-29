package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.QuteNote;
import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.MarkdownConverter;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteNote;

public class Pf2eMarkdown implements MarkdownConverter {
    final Pf2eIndex index;
    final MarkdownWriter writer;
    final Map<String, String> fallbackPaths;

    public Pf2eMarkdown(Pf2eIndex index, MarkdownWriter writer, Map<String, String> imageFallbackPaths) {
        this.index = index;
        this.writer = writer;
        this.fallbackPaths = imageFallbackPaths;
    }

    @Override
    public Pf2eMarkdown writeAll() {
        return writeFiles(Stream.of(Pf2eIndexType.values())
                .collect(Collectors.toList()));
    }

    @Override
    public Pf2eMarkdown writeFiles(IndexType type) {
        return writeFiles(List.of(type));
    }

    @Override
    public Pf2eMarkdown writeFiles(List<? extends IndexType> types) {
        if (types == null) {
        } else {
            writePf2eQuteBase(types.stream()
                    .filter(x -> !((Pf2eIndexType) x).useQuteNote())
                    .collect(Collectors.toList()));
            writeNotesAndTables(types.stream()
                    .filter(x -> ((Pf2eIndexType) x).useQuteNote())
                    .collect(Collectors.toList()));
        }
        return this;
    }

    private Pf2eMarkdown writePf2eQuteBase(List<? extends IndexType> types) {
        if (types != null && types.isEmpty()) {
            return this;
        }

        List<Pf2eQuteBase> compendium = new ArrayList<>();
        List<Pf2eQuteBase> rules = new ArrayList<>();

        for (Entry<String, JsonNode> e : index.filteredEntries()) {
            final String key = e.getKey();
            final JsonNode node = e.getValue();

            final Pf2eIndexType type = Pf2eIndexType.getTypeFromKey(key);
            if (types != null && !types.contains(type)) {
                continue;
            }

            // Moved to index type -- also used by embedded rendering
            Pf2eQuteBase converted = type.convertJson2QuteBase(index, node);
            if (converted != null) {
                append(type, converted, compendium, rules);
            }
        }

        writer.writeFiles(index.compendiumFilePath(), compendium);
        writer.writeFiles(index.rulesFilePath(), rules);

        index.tui().copyImages(Pf2eSources.getImages(), fallbackPaths);
        return this;
    }

    @Override
    public Pf2eMarkdown writeNotesAndTables() {
        return writeNotesAndTables(null);
    }

    private Pf2eMarkdown writeNotesAndTables(List<? extends IndexType> types) {
        if (types != null && types.isEmpty()) {
            return this;
        }

        List<QuteNote> compendium = new ArrayList<>();
        List<QuteNote> rules = new ArrayList<>();

        Map<Pf2eIndexType, Json2QuteBase> combinedDocs = new HashMap<>();
        for (Entry<String, JsonNode> e : index.filteredEntries()) {
            final String key = e.getKey();
            final JsonNode node = e.getValue();

            final Pf2eIndexType type = Pf2eIndexType.getTypeFromKey(key);
            if (types != null && !types.contains(type)) {
                continue;
            }

            switch (type) {
                case ability:
                    Pf2eQuteNote ability = Pf2eTypeAbility.createAbility(node, index, false);
                    rules.add(ability);
                    break;
                case book:
                    index.tui().warnf("Looking at book: %s", e.getKey());
                    JsonNode data = index.getIncludedNode(key.replace("book|", "data|"));
                    if (data == null) {
                        index.tui().errorf("No data for %s", key);
                    } else {
                        List<Pf2eQuteNote> pages = new Json2QuteBook(index, type, node, data).buildBook();
                        rules.addAll(pages);
                    }
                    break;
                case condition:
                    Json2QuteCompose conditions = (Json2QuteCompose) combinedDocs.computeIfAbsent(type,
                            t -> new Json2QuteCompose(type, index, "Conditions"));
                    conditions.add(node);
                    break;
                case domain:
                    Json2QuteCompose domains = (Json2QuteCompose) combinedDocs.computeIfAbsent(type,
                            t -> new Json2QuteCompose(type, index, "Domains"));
                    domains.add(node);
                    break;
                case skill:
                    Json2QuteCompose skills = (Json2QuteCompose) combinedDocs.computeIfAbsent(type,
                            t -> new Json2QuteCompose(type, index, "Skills"));
                    skills.add(node);
                    break;
                case table:
                    Pf2eQuteNote table = new Json2QuteTable(index, node).buildNote();
                    rules.add(table);
                    break;
                default:
                    break;
            }
        }

        for (Json2QuteBase value : combinedDocs.values()) {
            append(value.type, value.buildNote(), compendium, rules);
        }

        // Custom indices
        append(Pf2eIndexType.trait, Json2QuteTrait.buildIndex(index), compendium, rules);

        writer.writeNotes(index.compendiumFilePath(), compendium);
        writer.writeNotes(index.rulesFilePath(), rules);

        List<ImageRef> images = rules.stream()
                .flatMap(s -> s.images().stream()).collect(Collectors.toList());
        index.tui().copyImages(images, fallbackPaths);
        return this;
    }

    <T> void append(Pf2eIndexType type, T note, List<T> compendium, List<T> rules) {
        if (note != null) {
            if (type.useCompendiumBase()) {
                compendium.add(note);
            } else {
                rules.add(note);
            }
        }
    }
}
