package dev.ebullient.convert.tools.pf2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.TestUtils;
import dev.ebullient.convert.config.CompendiumConfig.Configurator;
import dev.ebullient.convert.config.CompendiumConfig.DiceRoller;
import dev.ebullient.convert.config.Datasource;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.io.Templates;
import dev.ebullient.convert.io.Tui;
import io.quarkus.arc.Arc;

public class CommonDataTests {
    protected final Tui tui;
    protected final Configurator configurator;
    protected final Templates templates;
    protected Pf2eIndex index;

    protected final Path outputPath;
    protected final TestInput variant;

    enum TestInput {
        all,
        partial,
        none;
    }

    public CommonDataTests(TestInput variant) throws Exception {
        tui = Arc.container().instance(Tui.class).get();
        tui.init(null, !TestUtils.USING_MAVEN, true, true);

        templates = Arc.container().instance(Templates.class).get();
        tui.setTemplates(templates);

        this.variant = variant;
        this.outputPath = TestUtils.OUTPUT_ROOT_PF2.resolve(variant.name());
        tui.setOutputPath(outputPath);
        outputPath.toFile().mkdirs();

        TtrpgConfig.init(tui, Datasource.toolsPf2e);
        TtrpgConfig.setToolsPath(TestUtils.PATH_PF2E_TOOLS_DATA);

        configurator = new Configurator(tui);

        index = new Pf2eIndex(TtrpgConfig.getConfig());
        templates.setCustomTemplates(TtrpgConfig.getConfig());

        if (TestUtils.PATH_PF2E_TOOLS_DATA.toFile().exists()) {
            switch (variant) {
                case all:
                    configurator.addSources(List.of("*"));
                    break;
                case partial:
                    configurator.readConfiguration(TestUtils.TEST_RESOURCES.resolve("pf2e.json"));
                    break;
                case none:
                    // should be default (CRD)
                    break;
            }

            for (String x : List.of("books.json",
                    "book/book-crb.json", "book/book-gmg.json")) {
                tui.readFile(TestUtils.PATH_PF2E_TOOLS_DATA.resolve(x), TtrpgConfig.getFixes(x), index::importTree);
            }
            tui.readToolsDir(TestUtils.PATH_PF2E_TOOLS_DATA, index::importTree);
            index.prepare();
        }
    }

    public void cleanup() throws Exception {
        configurator.setUseDiceRoller(DiceRoller.disabled);
        templates.setCustomTemplates(TtrpgConfig.getConfig());
    }

    public void done() throws IOException {
        tui.close();
        Path logFile = Path.of("ttrpg-convert.out.txt");
        if (Files.exists(logFile)) {
            Path newFile = outputPath.resolve(logFile);
            ;
            Files.move(logFile, newFile, StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println("Done.");
    }

    public void testDataIndex_pf2e() throws Exception {
        if (TestUtils.PATH_PF2E_TOOLS_DATA.toFile().exists()) {
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

    public void testNotes_p2fe() throws Exception {
        Path rulesDir = outputPath.resolve(index.rulesFilePath());
        Path compendiumDir = outputPath.resolve(index.compendiumFilePath());

        if (TestUtils.PATH_PF2E_TOOLS_DATA.toFile().exists()) {
            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(Stream.of(Pf2eIndexType.values())
                            .filter(x -> x.isOutputType() && x.useQuteNote())
                            .toList())
                    .writeImages();

            TestUtils.assertDirectoryContents(rulesDir, tui);
            assertThat(rulesDir.resolve("conditions.md")).exists();
            assertThat(compendiumDir.resolve("skills.md")).exists();
        }
    }

    Path generateNotesForType(Pf2eIndexType type) {
        return generateNotesForType(List.of(type)).values().iterator().next();
    }

    Map<Pf2eIndexType, Path> generateNotesForType(List<Pf2eIndexType> types) {
        Map<Pf2eIndexType, Path> map = new HashMap<>();
        Set<Path> paths = new HashSet<>();
        types.forEach(t -> {
            Path p = outputPath.resolve(t.getFilePath(index))
                    .resolve(t.relativePath());
            map.put(t, p);
            paths.add(p);
        });

        if (TestUtils.PATH_PF2E_TOOLS_DATA.toFile().exists()) {
            paths.forEach(p -> TestUtils.deleteDir(p));

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(types);

            paths.forEach(p -> TestUtils.assertDirectoryContents(p, tui));
        }
        return map;
    }

}
