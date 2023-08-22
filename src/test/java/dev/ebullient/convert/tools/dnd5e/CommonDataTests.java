package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.TestUtils;
import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.CompendiumConfig.Configurator;
import dev.ebullient.convert.config.ConfiguratorUtil;
import dev.ebullient.convert.config.Datasource;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.io.Templates;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;
import io.quarkus.arc.Arc;

public class CommonDataTests {
    protected final Tui tui;
    protected final Configurator configurator;
    protected final Templates templates;
    protected Tools5eIndex index;
    protected TestInput variant;

    enum TestInput {
        all,
        subset,
        none;
    }

    public CommonDataTests(TestInput variant) throws Exception {
        this.variant = variant;

        tui = Arc.container().instance(Tui.class).get();
        tui.init(null, true, false);

        templates = Arc.container().instance(Templates.class).get();
        tui.setTemplates(templates);

        TtrpgConfig.init(tui, Datasource.tools5e);
        configurator = new Configurator(tui);

        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            index = new Tools5eIndex(TtrpgConfig.getConfig());
            templates.setCustomTemplates(TtrpgConfig.getConfig());

            switch (variant) {
                case none:
                    // do nothing. SRD!
                    break;
                case subset:
                    // use default: compendium/ and rules/
                    configurator.readConfiguration(TestUtils.TEST_RESOURCES.resolve("sources.json"));
                    break;
                case all:
                    configurator.setSources(List.of("*"));
                    // use default: / and rules/
                    configurator.readConfiguration(TestUtils.TEST_RESOURCES.resolve("paths.json"));
                    break;
            }

            for (String x : List.of("adventures.json", "books.json",
                    "adventure/adventure-wdh.json", "adventure/adventure-pota.json",
                    "book/book-vgm.json", "book/book-phb.json")) {
                tui.readFile(TestUtils.TOOLS_PATH_5E.resolve(x), TtrpgConfig.getFixes(x), index::importTree);
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

            assertThat(p1Full).exists();
            JsonNode fullIndex = Tui.MAPPER.readTree(p1Full.toFile());
            ArrayNode fullIndexKeys = fullIndex.withArray("keys");
            assertThat(fullIndexKeys).isNotNull();
            assertThat(fullIndexKeys).isNotEmpty();

            assertThat(p1Source).exists();
            JsonNode filteredIndex = Tui.MAPPER.readTree(p1Source.toFile());
            ArrayNode filteredIndexKeys = filteredIndex.withArray("keys");
            assertThat(filteredIndexKeys).isNotNull();
            assertThat(filteredIndexKeys).isNotEmpty();
        }
    }

    public void testBackgroundList(Path outputPath) {
        tui.setOutputPath(outputPath);
        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path backgroundDir = deleteDir(Tools5eIndexType.background, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(Tools5eIndexType.background)
                    .writeNotesAndTables()
                    .writeImages();

            TestUtils.assertDirectoryContents(backgroundDir, tui);
        }
    }

    public void testClassList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path classDir = deleteDir(Tools5eIndexType.classtype, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(Tools5eIndexType.classtype)
                    .writeImages();

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
            Path deitiesDir = deleteDir(Tools5eIndexType.deity, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(Tools5eIndexType.deity)
                    .writeImages();

            if (variant != TestInput.none) {
                Path imageDir = deitiesDir.resolve("img");
                assertThat(imageDir.toFile()).exists();
            }

            TestUtils.assertDirectoryContents(deitiesDir, tui);
        }
    }

    public void testFeatList(Path outputPath) {
        tui.setOutputPath(outputPath);
        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path featDir = deleteDir(Tools5eIndexType.feat, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(Tools5eIndexType.feat)
                    .writeImages();

            TestUtils.assertDirectoryContents(featDir, tui);
        }
    }

    public void testItemList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path itemDir = deleteDir(Tools5eIndexType.item, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(Tools5eIndexType.item)
                    .writeImages();

            TestUtils.assertDirectoryContents(itemDir, tui);
        }
    }

    public void testMonsterList(Path outputPath) {
        tui.setOutputPath(outputPath);
        configurator.setAlwaysUseDiceRoller(true);

        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path bestiaryDir = deleteDir(Tools5eIndexType.monster, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(Tools5eIndexType.monster)
                    .writeImages();

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

            Path undead = out.resolve(index.compendiumFilePath()).resolve(Tools5eQuteBase.monsterPath(false, "undead"));
            assertThat(undead.toFile()).exists();

            TestUtils.assertDirectoryContents(undead, tui, (p, content) -> {
                List<String> errors = new ArrayList<>();
                boolean found = false;
                boolean index = false;
                List<String> frontmatter = new ArrayList<>();

                if (!content.get(0).equals("---")) {
                    errors.add(String.format("File %s did not contain frontmatter", p));
                    return errors;
                }

                for (String l : content.subList(1, content.size())) {
                    if (l.equals("cssclass: json5e-note")) {
                        index = true;
                    } else if (l.equals("statblock: true")) {
                        found = true;
                    } else if (l.equals("---")) {
                        break;
                    } else {
                        frontmatter.add(l);
                        if (l.contains("*")) {
                            errors.add(String.format("Found '*' in %s: %s", p, l));
                        }
                        TestUtils.commonTests(p, l, errors);
                    }
                }

                try {
                    Tui.plainYaml().load(String.join("\n", frontmatter));
                } catch (Exception e) {
                    errors.add(String.format("File %s contains invalid yaml: %s", p, e));
                }
                if (!found && !index) {
                    errors.add(String.format("File %s did not contain a statblock in the frontmatter", p));
                }
                return errors;
            });
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

            Path undead = out.resolve(index.compendiumFilePath()).resolve(Tools5eQuteBase.monsterPath(false, "undead"));
            assertThat(undead.toFile()).exists();

            TestUtils.assertDirectoryContents(undead, tui, (p, content) -> {
                List<String> errors = new ArrayList<>();
                boolean found = false;
                boolean yaml = false;
                boolean index = false;
                List<String> statblock = new ArrayList<>();

                for (String l : content) {
                    if (l.startsWith("# Index ")) {
                        index = true;
                    } else if (l.equals("```statblock")) {
                        found = yaml = true; // start yaml block
                    } else if (l.equals("```")) {
                        yaml = false; // end yaml block
                    } else if (yaml) {
                        statblock.add(l);
                        if (l.contains("*")) {
                            errors.add(String.format("Found '*' in %s: %s", p, l));
                        }
                        if (l.contains("\"desc\": \"\"")) {
                            errors.add(String.format("Found empty description in %s: %s", p, l));
                        }
                    }
                    TestUtils.commonTests(p, l, errors);
                }

                try {
                    Tui.quotedYaml().load(String.join("\n", statblock));
                } catch (Exception e) {
                    errors.add(String.format("File %s contains invalid yaml: %s", p, e));
                }
                if (!found && !index) {
                    errors.add(String.format("File %s did not contain a yaml statblock", p));
                }
                return errors;
            });
        }
    }

    public void testObjectList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path outDir = deleteDir(Tools5eIndexType.object, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(List.of(
                            Tools5eIndexType.object))
                    .writeImages();

            TestUtils.assertDirectoryContents(outDir, tui);
        }
    }

    public void testOptionalFeatureList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path ofDir = deleteDir(Tools5eIndexType.optionalFeatureTypes, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(List.of(
                            Tools5eIndexType.optionalFeatureTypes,
                            Tools5eIndexType.optionalfeature))
                    .writeImages();

            TestUtils.assertDirectoryContents(ofDir, tui);
        }
    }

    public void testRaceList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path raceDir = deleteDir(Tools5eIndexType.race, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(Tools5eIndexType.race)
                    .writeImages();

            TestUtils.assertDirectoryContents(raceDir, tui);
        }
    }

    public void testRewardList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path rewardDir = deleteDir(Tools5eIndexType.reward, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(Tools5eIndexType.reward)
                    .writeImages();

            if (variant == TestInput.none) {
                assertThat(rewardDir.toFile()).doesNotExist();
            } else {
                TestUtils.assertDirectoryContents(rewardDir, tui);
            }
        }
    }

    public void testRules(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            deleteDir(Tools5eIndexType.adventureData, outputPath, index.compendiumFilePath());
            deleteDir(Tools5eIndexType.bookData, outputPath, index.compendiumFilePath());
            deleteDir(Tools5eIndexType.table, outputPath, index.compendiumFilePath());
            TestUtils.deleteDir(index.rulesFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeNotesAndTables()
                    .writeImages();

            TestUtils.assertDirectoryContents(outputPath.resolve(index.rulesFilePath()), tui);
        }
    }

    public void testSpellList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path spellDir = deleteDir(Tools5eIndexType.spell, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(Tools5eIndexType.spell)
                    .writeImages();

            TestUtils.assertDirectoryContents(spellDir, tui);
        }
    }

    public void testTrapsHazardsList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path trapsDir = deleteDir(Tools5eIndexType.trap, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(List.of(Tools5eIndexType.trap, Tools5eIndexType.hazard))
                    .writeImages();

            TestUtils.assertDirectoryContents(trapsDir, tui);
        }
    }

    public void testVehicleList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            Path outDir = deleteDir(Tools5eIndexType.vehicle, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer, TtrpgConfig.imageFallbackPaths())
                    .writeFiles(List.of(Tools5eIndexType.vehicle))
                    .writeImages();

            TestUtils.assertDirectoryContents(outDir, tui);
        }
    }

    public Path compendiumFilePath() {
        return index.compendiumFilePath();
    }

    Path deleteDir(Tools5eIndexType type, Path outputPath, Path vaultPath) {
        final String relative = type.getRelativePath();
        final Path typeDir = outputPath.resolve(vaultPath).resolve(relative);
        TestUtils.deleteDir(typeDir);
        return typeDir;
    }
}
