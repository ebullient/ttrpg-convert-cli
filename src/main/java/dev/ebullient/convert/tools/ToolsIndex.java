package dev.ebullient.convert.tools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.Datasource;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndex;
import dev.ebullient.convert.tools.pf2e.Pf2eIndex;

public interface ToolsIndex {
    // Special one-offs for accounting/tracking
    enum TtrpgValue implements JsonNodeReader {
        indexKey,
        indexInputType;

        public void addToNode(JsonNode node, String value) {
            ((ObjectNode) node).put(this.name(), value);
        }

        public String getFromNode(JsonNode node) {
            if (node == null) {
                return null;
            }
            return this.getTextOrNull(node);
        }
    }

    static ToolsIndex createIndex() {
        CompendiumConfig config = TtrpgConfig.getConfig();
        return createIndex(config.datasource(), config);
    }

    static ToolsIndex createIndex(Datasource game, CompendiumConfig config) {
        if (Objects.requireNonNull(game) == Datasource.toolsPf2e) {
            return new Pf2eIndex(config);
        }
        return new Tools5eIndex(config);
    }

    CompendiumConfig cfg();

    default String rulesVaultRoot() {
        return cfg().rulesVaultRoot();
    }

    default String compendiumVaultRoot() {
        return cfg().compendiumVaultRoot();
    }

    default Path rulesFilePath() {
        return cfg().rulesFilePath();
    }

    default Path compendiumFilePath() {
        return cfg().compendiumFilePath();
    }

    void prepare();

    boolean notPrepared();

    ToolsIndex importTree(String filename, JsonNode node);

    MarkdownConverter markdownConverter(MarkdownWriter writer, Map<String, String> imageFallbackPaths);

    void writeFullIndex(Path resolve) throws IOException;

    void writeFilteredIndex(Path resolve) throws IOException;

    JsonNode getBook(String b);

    JsonNode getAdventure(String a);
}
