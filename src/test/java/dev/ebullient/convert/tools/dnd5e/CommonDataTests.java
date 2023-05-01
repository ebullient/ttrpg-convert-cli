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
import dev.ebullient.convert.tools.dnd5e.qute.QuteSource;
import io.quarkus.arc.Arc;

public class CommonDataTests {
    protected final Tui tui;
    protected final Configurator configurator;
    protected final Templates templates;
    protected Tools5eIndex index;

    public CommonDataTests(boolean useSources) throws Exception {
        tui = Arc.container().instance(Tui.class).get();
        tui.init(null, true, false);

        templates = Arc.container().instance(Templates.class).get();
        tui.setTemplates(templates);

        TtrpgConfig.init(tui, Datasource.tools5e);
        configurator = new Configurator(tui);

        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            index = new Tools5eIndex(TtrpgConfig.getConfig());
            templates.setCustomTemplates(TtrpgConfig.getConfig());

            if (useSources) {
                // use default: compendium/ and rules/
                configurator.readConfiguration(TestUtils.TEST_SOURCES_JSON_5E);
            } else {
                configurator.setSources(List.of("*"));
                // use default: / and rules/
                configurator.readConfiguration(TestUtils.TEST_FLAT_PATH_JSON);
            }

            for (String x : List.of("adventures.json", "books.json",
                    "adventure/adventure-wdh.json", "adventure/adventure-pota.json",
                    "book/book-vgm.json", "book/book-phb.json")) {
                tui.readFile(TestUtils.TOOLS_PATH_5E.resolve(x), index::importTree);
            }
            tui.readToolsDir(TestUtils.TOOLS_PATH_5E, index::importTree);
            index.prepare();
        }
    }

    public void cleanup() {
        tui.close();
        configurator.setAlwaysUseDiceRoller(false);
        templates.setCustomTemplates(TtrpgConfig.getConfig());
    }

    public void testKeyIndex(Path outputPath) throws Exception {
        tui.setOutputPath(outputPath);
        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
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
        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path featDir = outputPath.resolve(index.compendiumFilePath()).resolve(QuteSource.FEATS_PATH);
            TestUtils.deleteDir(featDir);

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(Tools5eIndexType.feat);

            TestUtils.assertDirectoryContents(featDir, tui);
        }
    }

    public void testBackgroundList(Path outputPath) {
        tui.setOutputPath(outputPath);
        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path backgroundDir = outputPath.resolve(index.compendiumFilePath()).resolve(QuteSource.BACKGROUND_PATH);
            TestUtils.deleteDir(backgroundDir);

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(Tools5eIndexType.background)
                    .writeNotesAndTables();

            TestUtils.assertDirectoryContents(backgroundDir, tui);
        }
    }

    public void testSpellList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path spellDir = outputPath.resolve(index.compendiumFilePath()).resolve(QuteSource.SPELLS_PATH);
            TestUtils.deleteDir(spellDir);

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(Tools5eIndexType.spell);

            TestUtils.assertDirectoryContents(spellDir, tui);
        }
    }

    public void testRaceList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path raceDir = outputPath.resolve(index.compendiumFilePath()).resolve(QuteSource.RACES_PATH);
            TestUtils.deleteDir(raceDir);

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(Tools5eIndexType.race);

            TestUtils.assertDirectoryContents(raceDir, tui);
        }
    }

    public void testClassList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path classDir = outputPath.resolve(index.compendiumFilePath()).resolve(QuteSource.CLASSES_PATH);
            TestUtils.deleteDir(classDir);

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(Tools5eIndexType.classtype);

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

        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path deitiesDir = outputPath.resolve(index.compendiumFilePath()).resolve(QuteSource.DEITIES_PATH);
            TestUtils.deleteDir(deitiesDir);

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(Tools5eIndexType.deity);

            Path imageDir = deitiesDir.resolve("img");
            assertThat(imageDir.toFile()).exists();

            TestUtils.assertDirectoryContents(deitiesDir, tui);
        }
    }

    public void testItemList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path itemDir = outputPath.resolve(index.compendiumFilePath()).resolve(QuteSource.ITEMS_PATH);
            TestUtils.deleteDir(itemDir);

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(Tools5eIndexType.item);

            TestUtils.assertDirectoryContents(itemDir, tui);
        }
    }

    public void testMonsterList(Path outputPath) {
        tui.setOutputPath(outputPath);
        configurator.setAlwaysUseDiceRoller(true);

        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path bestiaryDir = outputPath.resolve(index.compendiumFilePath()).resolve(QuteSource.MONSTERS_BASE_PATH);
            TestUtils.deleteDir(bestiaryDir);

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(Tools5eIndexType.monster);

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
        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path out = outputPath.resolve("alt-scores");
            TestUtils.deleteDir(out);

            tui.setOutputPath(out);

            CompendiumConfig testConfig = ConfiguratorUtil.testCustomTemplate("monster",
                    TestUtils.PROJECT_PATH.resolve("examples/templates/tools5e/monster2md-scores.txt"));
            templates.setCustomTemplates(testConfig);

            MarkdownWriter writer = new MarkdownWriter(out, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(Tools5eIndexType.monster);
        }
    }

    public void testMonsterYamlHeader(Path outputPath) {
        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path out = outputPath.resolve("yaml-header");
            TestUtils.deleteDir(out);

            tui.setOutputPath(out);

            CompendiumConfig testConfig = ConfiguratorUtil.testCustomTemplate("monster",
                    TestUtils.PROJECT_PATH.resolve("examples/templates/tools5e/monster2md-yamlStatblock-header.txt"));
            templates.setCustomTemplates(testConfig);

            MarkdownWriter writer = new MarkdownWriter(out, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(Tools5eIndexType.monster);

        }
    }

    public void testMonsterYamlBody(Path outputPath) {
        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path out = outputPath.resolve("yaml-body");
            TestUtils.deleteDir(out);
            tui.setOutputPath(out);

            CompendiumConfig testConfig = ConfiguratorUtil.testCustomTemplate("monster",
                    TestUtils.PROJECT_PATH.resolve("examples/templates/tools5e/monster2md-yamlStatblock-body.txt"));
            templates.setCustomTemplates(testConfig);

            MarkdownWriter writer = new MarkdownWriter(out, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(Tools5eIndexType.monster);

            Path undead = out.resolve(index.compendiumFilePath()).resolve(QuteSource.monsterPath(false, "undead"));
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
        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeNotesAndTables();

            TestUtils.assertDirectoryContents(outputPath.resolve(index.rulesFilePath()), tui);
        }
    }

    public Path compendiumFilePath() {
        return index.compendiumFilePath();
    }
}
