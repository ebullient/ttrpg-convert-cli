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
    public Pf2eMarkdown writeFiles(List<IndexType> types) {
        List<Pf2eQuteBase> compendium = new ArrayList<>();
        List<Pf2eQuteBase> rules = new ArrayList<>();

        for (Entry<String, JsonNode> e : index.filteredEntries()) {
            final String key = e.getKey();
            final JsonNode node = e.getValue();
            final Pf2eIndexType type = Pf2eIndexType.getTypeFromKey(key);

            if (!types.contains(type)) {
                continue;
            }

            Pf2eQuteBase converted = null;
            switch (type) {
                case action:
                    converted = new Json2QuteAction(index, type, node).build();
                    break;
                case trait:
                    converted = new Json2QuteTrait(index, type, node).build();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported type " + type);
            }
            append(type, converted, compendium, rules);
        }

        writer.writeFiles(index.compendiumPath(), compendium);
        writer.writeFiles(index.rulesPath(), rules);

        List<ImageRef> images = Stream.concat(compendium.stream(), rules.stream())
                .flatMap(s -> s.images().stream()).collect(Collectors.toList());

        index.tui().copyImages(images, fallbackPaths);
        return this;
    }

    @Override
    public Pf2eMarkdown writeNotesAndTables() {
        List<QuteNote> compendium = new ArrayList<>();
        List<QuteNote> rules = new ArrayList<>();

        Map<Pf2eIndexType, Json2QuteBase> combinedDocs = new HashMap<>();
        for (Entry<String, JsonNode> e : index.filteredEntries()) {
            final String key = e.getKey();
            final JsonNode node = e.getValue();
            final Pf2eIndexType type = Pf2eIndexType.getTypeFromKey(key);

            switch (type) {
                case skill:
                    Json2QuteCompose skills = (Json2QuteCompose) combinedDocs.computeIfAbsent(type,
                            t -> new Json2QuteCompose(type, index, "Skills"));
                    skills.add(node);
                    break;
                case condition:
                    Json2QuteCompose conditions = (Json2QuteCompose) combinedDocs.computeIfAbsent(type,
                            t -> new Json2QuteCompose(type, index, "Conditions"));
                    conditions.add(node);
                    break;
                default:
                    break;
            }
        }

        for (Json2QuteBase value : combinedDocs.values()) {
            append(value.type, (QuteNote) value.build(), compendium, rules);
        }

        // Custom indices
        append(Pf2eIndexType.trait, Json2QuteTrait.buildIndex(index), compendium, rules);

        writer.writeNotes(index.compendiumPath(), compendium);
        writer.writeNotes(index.rulesPath(), rules);

        List<ImageRef> images = rules.stream()
                .flatMap(s -> s.images().stream()).collect(Collectors.toList());
        index.tui().copyImages(images, fallbackPaths);
        return this;
    }

    <T> void append(Pf2eIndexType type, T note, List<T> compendium, List<T> rules) {
        if (note != null) {
            if (type.useCompendiumPath()) {
                compendium.add(note);
            } else {
                rules.add(note);
            }
        }
    }
}
