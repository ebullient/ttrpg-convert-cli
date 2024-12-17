package dev.ebullient.convert.tools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.Datasource;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndex;
import dev.ebullient.convert.tools.pf2e.Pf2eIndex;

public interface ToolsIndex {
    // Special one-offs for accounting/tracking
    enum TtrpgValue implements JsonNodeReader {
        indexBaseItem,
        indexFluffKey,
        indexInputType,
        indexKey,
        indexParentKey,
        indexVersionKeys,
        isHomebrew,
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

    default boolean resolveSources(Path toolsPath) {
        TtrpgConfig.setToolsPath(toolsPath);
        var allOk = true;
        for (String adventure : cfg().resolveAdventures()) {
            allOk &= cfg().readSource(toolsPath.resolve(adventure), TtrpgConfig.getFixes(adventure), this::importTree);
        }
        for (String book : cfg().resolveBooks()) {
            allOk &= cfg().readSource(toolsPath.resolve(book), TtrpgConfig.getFixes(book), this::importTree);
        }
        // Include additional standalone files from config (relative to current directory)
        for (String brew : cfg().resolveHomebrew()) {
            allOk &= cfg().readSource(Path.of(brew), TtrpgConfig.getFixes(brew), this::importTree);
        }
        return allOk;
    }

    void prepare();

    boolean notPrepared();

    ToolsIndex importTree(String filename, JsonNode node);

    MarkdownConverter markdownConverter(MarkdownWriter writer);

    void writeFullIndex(Path resolve) throws IOException;

    void writeFilteredIndex(Path resolve) throws IOException;

    JsonNode getBook(String b);

    JsonNode getAdventure(String a);
}
