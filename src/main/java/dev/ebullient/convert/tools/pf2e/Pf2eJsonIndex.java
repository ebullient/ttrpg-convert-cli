package dev.ebullient.convert.tools.pf2e;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.tools.MarkdownConverter;
import dev.ebullient.convert.tools.ToolsIndex;
import dev.ebullient.convert.tools.dnd5e.JsonIndex;

public class Pf2eJsonIndex implements ToolsIndex {

    final CompendiumConfig config;

    public Pf2eJsonIndex(CompendiumConfig config) {
        this.config = config;
    }

    @Override
    public CompendiumConfig cfg() {
        // TODO Auto-generated method stub
        return null;
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

}
