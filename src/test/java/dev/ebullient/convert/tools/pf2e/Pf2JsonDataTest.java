package dev.ebullient.convert.tools.pf2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.TestUtils;
import dev.ebullient.convert.config.CompendiumConfig.Configurator;
import dev.ebullient.convert.config.Datasource;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.io.Templates;
import dev.ebullient.convert.io.Tui;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class Pf2JsonDataTest {
    protected static Templates templates;
    protected static Tui tui;

    protected static Pf2eIndex index;
    protected static Path outputPath;
    protected static TestInput variant;

    enum TestInput {
        all,
        partial,
        none;
    }

    // Cache reading/indexing data
    protected static void configureIndex() {
        Configurator configurator = new Configurator(tui);
        configurator.setSources(List.of("*"));

        variant = TestInput.all;
        outputPath = TestUtils.OUTPUT_ROOT_PF2.resolve(variant.name());
        outputPath.toFile().mkdirs();
        tui.setOutputPath(outputPath);
    }

    @BeforeAll
    public static void prepare() {
        tui = Arc.container().instance(Tui.class).get();
        tui.init(null, true, false);

        TtrpgConfig.init(tui, Datasource.toolsPf2e);

        configureIndex();

        templates = Arc.container().instance(Templates.class).get();
        templates.setCustomTemplates(TtrpgConfig.getConfig());
        tui.setTemplates(templates);

        index = new Pf2eIndex(TtrpgConfig.getConfig());

        if (TestUtils.TOOLS_PATH_PF2E.toFile().exists()) {
            for (String x : List.of("books.json",
                    "book/book-crb.json", "book/book-gmg.json")) {
                tui.readFile(TestUtils.TOOLS_PATH_PF2E.resolve(x), index::importTree);
            }

            tui.readPf2eTools(TestUtils.TOOLS_PATH_PF2E, index::importTree);
            index.prepare();
        }
    }

    @AfterAll
    public static void cleanup() {
        tui.close();
    }

    @Test
    public void testDataIndex_pf2e() throws Exception {
        if (TestUtils.TOOLS_PATH_PF2E.toFile().exists()) {
            Path full = outputPath.resolve("allIndex.json");
            index.writeFullIndex(full);

            Path filtered = outputPath.resolve("allSourceIndex.json");
            index.writeFilteredIndex(filtered);

            assertThat(full).exists();
            JsonNode fullIndex = Tui.MAPPER.readTree(full.toFile());
            ArrayNode fullIndexKeys = fullIndex.withArray("keys");
            assertThat(fullIndexKeys).isNotNull();
            assertThat(fullIndexKeys).isNotEmpty();
            assertThat(fullIndex.has("ability|activate an item|crb"));

            assertThat(filtered).exists();
            JsonNode filteredIndex = Tui.MAPPER.readTree(filtered.toFile());
            ArrayNode filteredIndexKeys = filteredIndex.withArray("keys");
            assertThat(filteredIndexKeys).isNotNull();
            assertThat(filteredIndexKeys).isNotEmpty();

            if (variant == TestInput.all) {
                assertThat(fullIndexKeys).isEqualTo(filteredIndexKeys);
            } else {
                assertThat(fullIndexKeys.size()).isGreaterThan(filteredIndexKeys.size());
            }
        }
    }

    Path generateNotesForType(Pf2eIndexType type) {
        return generateNotesForType(List.of(type)).values().iterator().next();
    }

    Map<Pf2eIndexType, Path> generateNotesForType(List<Pf2eIndexType> types) {
        Map<Pf2eIndexType, Path> map = new HashMap<>();
        Set<Path> paths = new HashSet<>();
        types.forEach(t -> {
            Path p = outputPath.resolve(t.getBasePath(index))
                    .resolve(t.relativePath());
            map.put(t, p);
            paths.add(p);
        });

        if (TestUtils.TOOLS_PATH_PF2E.toFile().exists()) {
            paths.forEach(p -> TestUtils.deleteDir(p));

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(types);

            paths.forEach(p -> TestUtils.assertDirectoryContents(p, tui));
        }
        return map;
    }

    @Test
    public void testAction_p2fe() throws Exception {
        generateNotesForType(Pf2eIndexType.action);
    }

    @Test
    public void testAffliction_p2fe() throws Exception {
        generateNotesForType(List.of(Pf2eIndexType.curse, Pf2eIndexType.disease));
    }

    @Test
    public void testFeat_p2fe() throws Exception {
        generateNotesForType(Pf2eIndexType.feat);
    }

    @Test
    public void testRitual_p2fe() throws Exception {
        generateNotesForType(Pf2eIndexType.ritual);
    }

    @Test
    public void testSpell_p2fe() throws Exception {
        generateNotesForType(Pf2eIndexType.spell);
    }

    @Test
    public void testTable_p2fe() throws Exception {
        generateNotesForType(Pf2eIndexType.table);
    }

    @Test
    public void testTrait_p2fe() throws Exception {
        generateNotesForType(Pf2eIndexType.trait);
    }

    @Test
    public void testNotes_p2fe() throws Exception {
        Path rulesDir = outputPath.resolve(index.rulesPath());
        Path compendiumDir = outputPath.resolve(index.compendiumPath());

        if (TestUtils.TOOLS_PATH_PF2E.toFile().exists()) {
            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeNotesAndTables();

            TestUtils.assertDirectoryContents(rulesDir, tui);
            assertThat(rulesDir.resolve("conditions.md")).exists();
            assertThat(compendiumDir.resolve("skills.md")).exists();
        }
    }
}
