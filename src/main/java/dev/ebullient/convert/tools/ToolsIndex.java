package dev.ebullient.convert.tools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.Datasource;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndex;
import dev.ebullient.convert.tools.pf2e.Pf2eIndex;

public interface ToolsIndex {

    static ToolsIndex createIndex() {
        CompendiumConfig config = TtrpgConfig.getConfig();
        return createIndex(config.datasource(), config);
    }

    static ToolsIndex createIndex(Datasource game, CompendiumConfig config) {
        switch (game) {
            case toolsPf2e:
                return new Pf2eIndex(config);
            default:
                return new Tools5eIndex(config);
        }
    }

    CompendiumConfig cfg();

    default String rulesRoot() {
        return cfg().rulesRoot();
    }

    default String compendiumRoot() {
        return cfg().compendiumRoot();
    }

    default Path rulesPath() {
        return cfg().rulesPath();
    }

    default Path compendiumPath() {
        return cfg().compendiumPath();
    }

    void prepare();

    boolean notPrepared();

    ToolsIndex importTree(String filename, JsonNode node);

    MarkdownConverter markdownConverter(MarkdownWriter writer, Map<String, String> imageFallbackPaths);

    void writeFullIndex(Path resolve) throws IOException;

    void writeFilteredIndex(Path resolve) throws IOException;
}
