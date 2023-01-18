package dev.ebullient.convert.tools.pf2e;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.tools.MarkdownConverter;
import dev.ebullient.convert.tools.ToolsIndex;

public class Pf2eIndex implements ToolsIndex, JsonSource {

    final CompendiumConfig config;

    private final Map<String, JsonNode> imported = new HashMap<>();
    private final Map<String, JsonNode> filteredIndex = new HashMap<>();

    final JsonSourceCopier copier = new JsonSourceCopier(this);

    public Pf2eIndex(CompendiumConfig config) {
        this.config = config;
    }

    @Override
    public boolean notPrepared() {
        return filteredIndex.isEmpty();
    }

    @Override
    public Pf2eIndex importTree(String filename, JsonNode node) {
        if (!node.isObject()) {
            return this;
        }

        // user configuration
        config.readConfigurationIfPresent(node);

        // data ingest. Minimal processing.
        Pf2eIndexType.action.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.condition.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.skill.withArrayFrom(node, this::addToIndex);

        return this;
    }

    void addToIndex(Pf2eIndexType type, JsonNode node) {
        // TODO: Variants? Reprints?

        String key = type.createKey(node);
        TtrpgValue.indexKey.addToNode(node, key); // backlink
        imported.put(key, node);
    }

    @Override
    public void prepare() {
        if (!this.filteredIndex.isEmpty()) {
            return;
        }

        imported.forEach((key, node) -> {
            Pf2eIndexType type = Pf2eIndexType.getTypeFromKey(key);
            // check for / manage copies first (creatures, fluff)
            node = copier.handleCopy(type, node);
            Pf2eSources.constructSources(node); // pre-construct sources
        });

        imported.entrySet().stream()
                .filter(e -> keyIsIncluded(e.getKey(), e.getValue()))
                .forEach(e -> filteredIndex.put(e.getKey(), e.getValue()));
    }

    boolean keyIsIncluded(String key, JsonNode node) {
        // Check against include/exclude rules (srdKeys allowed when there are no sources)
        Optional<Boolean> rulesAllow = config.keyIsIncluded(key, node);
        if (rulesAllow.isPresent()) {
            return rulesAllow.get();
        }
        Pf2eSources sources = Pf2eSources.findSources(key);
        if (config.noSources()) {
            return sources.fromDefaultSource();
        }
        return sources.getBookSources().stream().anyMatch((s) -> config.sourceIncluded(s));
    }

    public boolean isIncluded(String key) {
        return filteredIndex.containsKey(key);
    }

    public boolean isExcluded(String key) {
        return !isIncluded(key);
    }

    // --------- Write indexes ---------

    @Override
    public MarkdownConverter markdownConverter(MarkdownWriter writer, Map<String, String> imageFallbackPaths) {
        return new Pf2eMarkdown(this, writer, imageFallbackPaths);
    }

    @Override
    public void writeFullIndex(Path outputFile) throws IOException {
        if (notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing indexes");
        }
        Map<String, Object> allKeys = new HashMap<>();
        List<String> keys = new ArrayList<>(filteredIndex.keySet());
        Collections.sort(keys);
        allKeys.put("keys", keys);
        tui().writeJsonFile(outputFile, allKeys);
    }

    @Override
    public void writeFilteredIndex(Path outputFile) throws IOException {
        if (notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing files");
        }
        List<String> keys = new ArrayList<>(filteredIndex.keySet());
        Collections.sort(keys);
        tui().writeJsonFile(outputFile, Map.of("keys", keys));
    }

    // ---- JsonSource overrides ------

    @Override
    public CompendiumConfig cfg() {
        return config;
    }

    @Override
    public Pf2eIndex index() {
        return this;
    }

    @Override
    public Pf2eSources getSources() {
        return null;
    }

    public Set<Map.Entry<String, JsonNode>> filteredEntries() {
        if (notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing indexes");
        }
        return filteredIndex.entrySet();
    }
}
