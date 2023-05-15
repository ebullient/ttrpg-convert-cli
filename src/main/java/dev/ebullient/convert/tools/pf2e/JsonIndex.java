package dev.ebullient.convert.tools.pf2e;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.tools.MarkdownConverter;
import dev.ebullient.convert.tools.ToolsIndex;

public class JsonIndex implements ToolsIndex {

    final CompendiumConfig config;

    public JsonIndex(CompendiumConfig config) {
        this.config = config;
    }

    @Override
    public CompendiumConfig cfg() {
        return config;
    }

    @Override
    public void prepare() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean notPrepared() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public JsonIndex importTree(String filename, JsonNode node) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MarkdownConverter markdownConverter(MarkdownWriter writer, Map<String, String> imageFallbackPaths) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void writeFullIndex(Path resolve) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeFilteredIndex(Path resolve) throws IOException {
        // TODO Auto-generated method stub

    }

    public boolean sourceIncluded(String asText) {
        return false;
    }

    public JsonNode getOrigin(IndexType type, JsonNode _copy) {
        return null;
    }

    public String getKey(IndexType type, JsonNode jsonSource) {
        return null;
    }

    public ToolsPf2eSources constructSources(IndexType race, JsonNode jsonNode) {
        return null;
    }

    public JsonNode getOrigin(IndexType parentType, String parentName, String parentSource) {
        return null;
    }

    public JsonNode getOrigin(String key) {
        return null;
    }

    public boolean isIncluded(String key) {
        return false;
    }

    public String createSimpleKey(IndexType type, String string, String source) {
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

}
