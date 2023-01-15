package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import dev.ebullient.convert.TestUtils;
import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.CompendiumConfig.Configurator;
import dev.ebullient.convert.config.ConfiguratorUtil;
import dev.ebullient.convert.config.Datasource;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.io.Templates;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.QuteSource;
import dev.ebullient.convert.tools.IndexType;
import io.quarkus.arc.Arc;

public class CommonDataTests {
    protected final Tui tui;
    protected final Configurator configurator;
    protected final TtrpgConfig ttrpgConfig;
    protected final Templates templates;
    protected JsonIndex index;

    public CommonDataTests(boolean useSources) throws Exception {
        ttrpgConfig = Arc.container().instance(TtrpgConfig.class).get();
        templates = Arc.container().instance(Templates.class).get();

        tui = Arc.container().instance(Tui.class).get();
        tui.init(null, true, false);

        configurator = new Configurator(ttrpgConfig, tui, Datasource.tools5e);

        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            index = new JsonIndex(ttrpgConfig.getConfig());

            configurator.readConfiguration(TestUtils.TEST_PATH_JSON);
            if (useSources) {
                configurator.readConfiguration(TestUtils.TEST_SOURCES_JSON);
            } else {
                configurator.setSources(List.of("*"));
            }

            for (String x : List.of("adventures.json", "books.json", "adventure/adventure-wdh.json",
                    "book/book-vgm.json", "book/book-phb.json")) {
                tui.readFile(TestUtils.TOOLS_PATH.resolve(x), index::importTree);
            }
            tui.read5eTools(TestUtils.TOOLS_PATH, index::importTree);
            index.prepare();
        }
    }

    public void cleanup() {
        tui.close();
    }

    public void testKeyIndex(Path outputPath) throws Exception {
        tui.setOutputPath(outputPath);
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path p1Full = outputPath.resolve("allIndex.json");
            index.writeFullIndex(p1Full);
            Path p1Source = outputPath.resolve("allSourceIndex.json");
            index.writeFilteredIndex(p1Source);

            // JsonIndex index2 = new JsonIndex(List.of("PHB", "DMG", "XGE"), tui);
            // TestUtils.fullIndex(index2, TOOLS_PATH, tui);

            // Path p2Full = OUTPUT_PATH.resolve("some-Index.json");
            // index2.writeIndex(p2Full);
            // Path p2Source = OUTPUT_PATH.resolve("some-SourceIndex.json");
            // index2.writeSourceIndex(p2Source);

            // TestUtils.assertContents(p2Full, p2Source, false); // filtered
        }
    }

    public void testFeatList(Path outputPath) {
        tui.setOutputPath(outputPath);
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path featDir = outputPath.resolve(index.compendiumPath()).resolve(QuteSource.FEATS_PATH);
            TestUtils.deleteDir(featDir);

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, ttrpgConfig.imageFallbackPaths())
                    .writeFiles(IndexType.feat);

            TestUtils.assertDirectoryContents(featDir, tui);
        }
    }

    public void testBackgroundList(Path outputPath) {
        tui.setOutputPath(outputPath);
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path backgroundDir = outputPath.resolve(index.compendiumPath()).resolve(QuteSource.BACKGROUND_PATH);
            TestUtils.deleteDir(backgroundDir);

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, ttrpgConfig.imageFallbackPaths())
                    .writeFiles(IndexType.background)
                    .writeRulesAndTables();

            TestUtils.assertDirectoryContents(backgroundDir, tui);
        }
    }

    public void testSpellList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path spellDir = outputPath.resolve(index.compendiumPath()).resolve(QuteSource.SPELLS_PATH);
            TestUtils.deleteDir(spellDir);

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, ttrpgConfig.imageFallbackPaths())
                    .writeFiles(IndexType.spell);

            TestUtils.assertDirectoryContents(spellDir, tui);
        }
    }

    public void testRaceList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path raceDir = outputPath.resolve(index.compendiumPath()).resolve(QuteSource.RACES_PATH);
            TestUtils.deleteDir(raceDir);

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, ttrpgConfig.imageFallbackPaths())
                    .writeFiles(IndexType.race);

            TestUtils.assertDirectoryContents(raceDir, tui);
        }
    }

    public void testClassList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path classDir = outputPath.resolve(index.compendiumPath()).resolve(QuteSource.CLASSES_PATH);
            TestUtils.deleteDir(classDir);

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, ttrpgConfig.imageFallbackPaths())
                    .writeFiles(IndexType.classtype);

            TestUtils.assertDirectoryContents(classDir, tui, (p, content) -> {
                List<String> e = new ArrayList<>();
                boolean found = false;
                boolean index = false;

                for (String l : content) {
                    if (l.startsWith("# Index ")) {
                        index = true;
                    } else if (l.startsWith("## ")) {
                        found = true; // Found class features
                    }
                    TestUtils.commonTests(p, l, e);
                }

                if (!found && !index) {
                    e.add(String.format("File %s did not contain class features", p));
                }
                return e;
            });
        }
    }

    public void testDeityList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path deitiesDir = outputPath.resolve(index.compendiumPath()).resolve(QuteSource.DEITIES_PATH);
            TestUtils.deleteDir(deitiesDir);

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, ttrpgConfig.imageFallbackPaths())
                    .writeFiles(IndexType.deity);

            Path imageDir = deitiesDir.resolve("img");
            assertThat(imageDir.toFile()).exists();
        }
    }

    public void testItemList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path itemDir = outputPath.resolve(index.compendiumPath()).resolve(QuteSource.ITEMS_PATH);
            TestUtils.deleteDir(itemDir);

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, ttrpgConfig.imageFallbackPaths())
                    .writeFiles(IndexType.item);

            TestUtils.assertDirectoryContents(itemDir, tui);
        }
    }

    public void testMonsterList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path bestiaryDir = outputPath.resolve(index.compendiumPath()).resolve(QuteSource.MONSTERS_BASE_PATH);
            TestUtils.deleteDir(bestiaryDir);

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, ttrpgConfig.imageFallbackPaths())
                    .writeFiles(IndexType.monster);

            Path tokenDir = bestiaryDir.resolve("undead/token");
            assertThat(tokenDir.toFile()).exists();

            try (Stream<Path> paths = Files.list(bestiaryDir)) {
                paths.forEach(p -> {
                    if (p.toFile().isDirectory()) {
                        TestUtils.assertDirectoryContents(p, tui);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void testMonsterAlternateScores(Path outputPath) {
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path out = outputPath.resolve("alt-scores");
            TestUtils.deleteDir(out);

            tui.setOutputPath(out);

            CompendiumConfig testConfig = ConfiguratorUtil.testCustomTemplate(ttrpgConfig, "monster",
                    TestUtils.PROJECT_PATH.resolve("src/main/resources/templates/monster2md-scores.txt"));
            templates.setCustomTemplates(testConfig);

            MarkdownWriter writer = new MarkdownWriter(out, templates, tui);
            index.markdownConverter(writer, ttrpgConfig.imageFallbackPaths())
                    .writeFiles(IndexType.monster);
        }
    }

    public void testMonsterYamlHeader(Path outputPath) {
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path out = outputPath.resolve("yaml-header");
            TestUtils.deleteDir(out);

            tui.setOutputPath(out);

            CompendiumConfig testConfig = ConfiguratorUtil.testCustomTemplate(ttrpgConfig, "monster",
                    TestUtils.PROJECT_PATH.resolve("src/main/resources/templates/monster2md-yamlStatblock-header.txt"));
            templates.setCustomTemplates(testConfig);

            MarkdownWriter writer = new MarkdownWriter(out, templates, tui);
            index.markdownConverter(writer, ttrpgConfig.imageFallbackPaths())
                    .writeFiles(IndexType.monster);

        }
    }

    public void testMonsterYamlBody(Path outputPath) {
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path out = outputPath.resolve("yaml-body");
            TestUtils.deleteDir(out);
            tui.setOutputPath(out);

            CompendiumConfig testConfig = ConfiguratorUtil.testCustomTemplate(ttrpgConfig, "monster",
                    TestUtils.PROJECT_PATH.resolve("src/main/resources/templates/monster2md-yamlStatblock-body.txt"));
            templates.setCustomTemplates(testConfig);

            MarkdownWriter writer = new MarkdownWriter(out, templates, tui);
            index.markdownConverter(writer, ttrpgConfig.imageFallbackPaths())
                    .writeFiles(IndexType.monster);

            Path undead = out.resolve(index.compendiumPath()).resolve(QuteSource.monsterPath(false, "undead"));
            assertThat(undead.toFile()).exists();

            TestUtils.assertDirectoryContents(undead, tui, (p, content) -> {
                List<String> e = new ArrayList<>();
                boolean found = false;
                boolean yaml = false;
                boolean index = false;

                for (String l : content) {
                    if (l.startsWith("# Index ")) {
                        index = true;
                    } else if (l.equals("```statblock")) {
                        found = yaml = true; // start yaml block
                    } else if (l.equals("```")) {
                        yaml = false; // end yaml block
                    } else if (yaml && l.contains("*")) {
                        e.add(String.format("Found '*' in %s: %s", p.toString(), l));
                    }
                    TestUtils.commonTests(p, l, e);
                }

                if (!found && !index) {
                    e.add(String.format("File %s did not contain a yaml statblock", p));
                }
                return e;
            });
        }
    }

    public void testRules(Path outputPath) {
        tui.setOutputPath(outputPath);
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, ttrpgConfig.imageFallbackPaths())
                    .writeRulesAndTables();
        }
    }
}
