package dev.ebullient.convert.tools.pf2e;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.MarkdownConverter;
import dev.ebullient.convert.tools.ToolsIndex;

public class Pf2eIndex implements ToolsIndex, JsonSource {
    static final String CORE_RULES = "book-crb.json";
    final CompendiumConfig config;

    private final Map<String, JsonNode> imported = new HashMap<>();
    private final Map<String, JsonNode> filteredIndex = new HashMap<>();

    private final Map<String, String> traitToTag = new HashMap<>();
    private final Map<String, Collection<String>> categoryToTraits = new TreeMap<>();

    final JsonSourceCopier copier = new JsonSourceCopier(this);
    boolean coreRulesIncluded = false;

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
        coreRulesIncluded |= filename.endsWith(CORE_RULES);

        // data ingest. Minimal processing.
        Pf2eIndexType.ability.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.action.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.curse.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.condition.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.disease.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.feat.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.ritual.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.skill.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.spell.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.table.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.trait.withArrayFrom(node, this::addToIndex);

        Pf2eIndexType.adventure.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.book.withArrayFrom(node, this::addToIndex);

        addDataToIndex(Pf2eIndexType.data.getFrom(node), filename);

        return this;
    }

    void addToIndex(Pf2eIndexType type, JsonNode node) {
        // TODO: Variants? Reprints?
        String key = type.createKey(node);
        TtrpgValue.indexKey.addToNode(node, key); // backlink
        imported.put(key, node);

        // Precreate tag index/lookup
        if (type == Pf2eIndexType.trait) {
            String trait = Field.name.getTextOrEmpty(node);
            String traitTag = cfg().traitTagOf(trait);
            traitToTag.put(trait.toLowerCase(), traitTag);

            Field.categories.getListOfStrings(node, tui()).stream()
                    .filter(c -> !c.equals("_alignAbv"))
                    .forEach(c -> {
                        String categoryTag = cfg().traitCategoryTagOf(c);
                        categoryToTraits.computeIfAbsent(categoryTag, k -> new TreeSet<>())
                                .add(traitTag);
                    });
        }
    }

    void addDataToIndex(JsonNode data, String filename) {
        if (data == null || filename.isEmpty()) {
            return;
        }
        int slash = filename.indexOf('/');
        int dot = filename.indexOf('.');
        String name = filename.substring(slash < 0 ? 0 : slash + 1, dot < 0 ? filename.length() : dot);
        String key = Pf2eIndexType.data.createKey(name, null); // e.g. data|book-crb

        // synthetic node
        ObjectNode newNode = Tui.MAPPER.createObjectNode();
        newNode.put("name", name);
        newNode.put("filename", filename);
        newNode.set("data", data);

        int dash = name.lastIndexOf("-");
        if (dash >= 0) {
            newNode.put("source", name.substring(dash + 1));
        }
        TtrpgValue.indexKey.addToNode(newNode, key); // backlink
        imported.put(key, newNode);
    }

    @Override
    public void prepare() {
        if (!this.filteredIndex.isEmpty()) {
            return;
        }

        if (!coreRulesIncluded) {
            tui().warn("The core rules were not found or included. Some references may be missing");
        }

        imported.forEach((key, node) -> {
            Pf2eIndexType type = Pf2eIndexType.getTypeFromKey(key);
            if (type.checkCopiesAndReprints()) {
                // check for / manage copies first (creatures, fluff)
                node = copier.handleCopy(type, node);
            }
            Pf2eSources.constructSources(type, node); // pre-construct sources
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

    // public String getTagForTrait(String trait) {
    //     String traitTag = traitToTag.get(trait.toLowerCase());
    //     if (traitTag == null && trait.contains("<")) {
    //         String[] pieces = trait.split(" ");
    //         return cfg().traitTagOf(pieces);
    //     }
    //     if (traitTag == null) {
    //         tui().warnf("Unknown trait %s, not in index", trait);
    //         traitTag = cfg().traitTagOf(trait);
    //     }
    //     return traitTag;
    // }

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

    public Set<Map.Entry<String, JsonNode>> filteredEntries() {
        if (notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing indexes");
        }
        return filteredIndex.entrySet();
    }

    public Map<String, Collection<String>> categoryTraitMap() {
        return categoryToTraits;
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

}
