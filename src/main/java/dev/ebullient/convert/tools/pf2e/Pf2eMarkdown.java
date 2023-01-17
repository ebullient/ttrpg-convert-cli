package dev.ebullient.convert.tools.pf2e;

import java.nio.file.Path;
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
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.qute.QuteNote;
import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.MarkdownConverter;

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
        List<QuteBase> sources = new ArrayList<>();

        for (Entry<String, JsonNode> e : index.filteredEntries()) {
            final String key = e.getKey();
            final JsonNode node = e.getValue();
            final Pf2eIndexType type = Pf2eIndexType.getTypeFromKey(key);

            if (!types.contains(type)) {
                continue;
            }

            QuteBase converted = null;
            switch (type) {
                case action:
                    converted = new Json2QuteAction(index, type, node).build();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported type " + type);
            }
            if (converted != null) {
                sources.add(converted);
            }
        }

        writer.writeFiles(sources, index.compendiumPath());

        List<ImageRef> images = sources.stream()
                .flatMap(s -> s.images().stream()).collect(Collectors.toList());

        index.tui().copyImages(images, fallbackPaths);
        return this;
    }

    @Override
    public Pf2eMarkdown writeRulesAndTables() {
        List<QuteNote> rules = new ArrayList<>();
        Map<Pf2eIndexType, Json2QuteBase> combinedDocs = new HashMap<>();

        for (Entry<String, JsonNode> e : index.filteredEntries()) {
            final String key = e.getKey();
            final JsonNode node = e.getValue();
            final Pf2eIndexType type = Pf2eIndexType.getTypeFromKey(key);

            switch (type) {
                case skill:
                    Json2QuteSkills skills = (Json2QuteSkills) combinedDocs.computeIfAbsent(type,
                            t -> new Json2QuteSkills(index));
                    skills.add(node);
                    break;
                default:
                    break;
            }
        }

        for (Json2QuteBase value : combinedDocs.values()) {
            QuteBase note = value.build();
            if (note != null) {
                rules.add((QuteNote) note);
            }
        }

        Path rulesPath = index.rulesPath();
        writer.writeNotes(rulesPath, rules);

        List<ImageRef> images = rules.stream()
                .flatMap(s -> s.images().stream()).collect(Collectors.toList());
        index.tui().copyImages(images, fallbackPaths);
        return this;
    }
}
