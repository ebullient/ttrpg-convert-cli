package dev.ebullient.convert.tools.pf2e;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.tools.MarkdownConverter;
import dev.ebullient.convert.tools.ToolsIndex;

public class ToolsPf2eIndex implements ToolsIndex, JsonSource {

    final CompendiumConfig config;

    private final Map<String, JsonNode> imported = new HashMap<>();

    private final Map<String, JsonNode> variantIndex = new HashMap<>();
    private final Map<String, JsonNode> filteredIndex = new HashMap<>();
    private final Map<String, String> aliases = new HashMap<>();

    final JsonSourceCopier copier = new JsonSourceCopier(this);

    public ToolsPf2eIndex(CompendiumConfig config) {
        this.config = config;
    }

    @Override
    public boolean notPrepared() {
        return variantIndex.isEmpty();
    }

    @Override
    public ToolsPf2eIndex importTree(String filename, JsonNode node) {
        if (!node.isObject()) {
            return this;
        }

        // user configuration
        config.readConfigurationIfPresent(node);



        return null;
    }

    @Override
    public void prepare() {
        // TODO Auto-generated method stub

    }

    public boolean sourceIncluded(String asText) {
        return false;
    }

    public JsonNode getOrigin(ToolsPf2eIndexType type, JsonNode _copy) {
        return null;
    }

    public String getKey(ToolsPf2eIndexType type, JsonNode jsonSource) {
        return null;
    }

    public ToolsPf2eSources constructSources(ToolsPf2eIndexType race, JsonNode jsonNode) {
        return null;
    }

    public JsonNode getOrigin(ToolsPf2eIndexType parentType, String parentName, String parentSource) {
        return null;
    }

    public JsonNode getOrigin(String key) {
        return null;
    }

    public boolean isIncluded(String key) {
        return false;
    }

    public String createSimpleKey(ToolsPf2eIndexType type, String string, String source) {
        return null;
    }

    public String getAliasOrDefault(Object createSimpleKey) {
        return null;
    }

    public boolean isExcluded(String key) {
        return false;
    }

    public JsonNode getNode(String aliasKey) {
        return null;
    }

    // --------- Write indexes ---------

    @Override
    public MarkdownConverter markdownConverter(MarkdownWriter writer, Map<String, String> imageFallbackPaths) {
        return new ToolsPf2eMarkdown(this, writer, imageFallbackPaths);
    }

    @Override
    public void writeFullIndex(Path outputFile) throws IOException {
        if (notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing indexes");
        }
        Map<String, Object> allKeys = new HashMap<>();
        List<String> keys = new ArrayList<>(variantIndex.keySet());
        Collections.sort(keys);
        allKeys.put("keys", keys);
        allKeys.put("mapping", aliases);
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
    public ToolsPf2eIndex index() {
        return this;
    }

    @Override
    public ToolsPf2eSources getSources() {
        return null;
    }
}
