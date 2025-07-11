package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.TestUtils;
import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.CompendiumConfig.Configurator;
import dev.ebullient.convert.config.CompendiumConfig.DiceRoller;
import dev.ebullient.convert.config.ConfiguratorUtil;
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
    protected final Path toolsData;

    public final boolean dataPresent;

    public final Tools5eIndex index;
    public final TestInput variant;
    public final CompendiumConfig config;

    enum TestInput {
        all,
        allNewest,
        none,
        srdEdition,
        srd2014,
        srd2024,
        subset2014,
        subset2024,
        subsetMixed,
        ;
    }

    public CommonDataTests(TestInput variant, String config, Path toolsData) throws Exception {
        this.toolsData = toolsData;
        dataPresent = toolsData.toFile().exists();

        this.variant = variant;

        tui = Arc.container().instance(Tui.class).get();
        tui.init(null, !TestUtils.USING_MAVEN, true, true);

        templates = Arc.container().instance(Templates.class).get();
        tui.setTemplates(templates);

        TtrpgConfig.init(tui, Datasource.tools5e);
        TtrpgConfig.setToolsPath(TestUtils.PATH_5E_TOOLS_DATA);

        configurator = new Configurator(tui);
        configurator.readConfiguration(TestUtils.TEST_RESOURCES.resolve("5e/images-remote.json"));

        index = new Tools5eIndex(TtrpgConfig.getConfig());

        if (dataPresent) {
            templates.setCustomTemplates(TtrpgConfig.getConfig());

            JsonNode configNode = Tui.MAPPER.readTree(config);
            configurator.readConfigIfPresent(configNode);

            // var additional = List.of(
            //         "adventures.json",
            //         "books.json");

            // for (String x : additional) {
            //     tui.readFile(toolsData.resolve(x), TtrpgConfig.getFixes(x), index::importTree);
            // }
            tui.readToolsDir(toolsData, index::importTree);
            index.resolveSources(toolsData);
            index.prepare();
        }
        this.config = TtrpgConfig.getConfig();
    }

    public void afterEach() throws Exception {
        configurator.setUseDiceRoller(DiceRoller.disabled);
        templates.setCustomTemplates(TtrpgConfig.getConfig());
        TestUtils.cleanupReferences();
    }

    public void afterAll(Path outputPath) throws IOException {
        index.cleanup();

        assertThat(Tools5eIndex.instance()).isNull();
        tui.close();
        Path logFile = Path.of("ttrpg-convert.out.txt");
        if (Files.exists(logFile)) {
            Path newFile = outputPath.resolve(logFile);
            Files.move(logFile, newFile, StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println("Done.");
    }

    public void testKeyIndex(Path outputPath) throws Exception {
        tui.setOutputPath(outputPath);
        if (dataPresent) {
            Path p1Full = outputPath.resolve("allIndex.json");
            index.writeFullIndex(p1Full);

            Path p1Source = outputPath.resolve("allSourceIndex.json");
            index.writeFilteredIndex(p1Source);

            assertThat(p1Full).exists();
            assertThat(p1Source).exists();
        }
    }

    public void testAdventures(Path outputPath) {
        tui.setOutputPath(outputPath);
        if (dataPresent) {
            Path testDir = deleteDir(Tools5eIndexType.adventureData, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(Tools5eIndexType.adventureData);

            TestUtils.assertDirectoryContents(testDir, tui);
        }
    }

    public void testBackgroundList(Path outputPath) {
        tui.setOutputPath(outputPath);
        if (dataPresent) {
            Path backgroundDir = deleteDir(Tools5eIndexType.background, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(Tools5eIndexType.background);

            TestUtils.assertDirectoryContents(backgroundDir, tui);
        }
    }

    public void testBookList(Path outputPath) {
        tui.setOutputPath(outputPath);
        if (dataPresent) {
            Path testDir = deleteDir(Tools5eIndexType.bookData, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(Tools5eIndexType.bookData);

            TestUtils.assertDirectoryContents(testDir, tui);
        }
    }

    public void testClassList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (dataPresent) {
            Path classDir = deleteDir(Tools5eIndexType.classtype, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer)
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

    public void testDeckList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (dataPresent) {
            Path outDir = deleteDir(Tools5eIndexType.deck, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(Tools5eIndexType.deck);

            TestUtils.assertDirectoryContents(outDir, tui);
        }
    }

    public void testDeityList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (dataPresent) {
            Path outDir = deleteDir(Tools5eIndexType.deity, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(Tools5eIndexType.deity);

            TestUtils.assertDirectoryContents(outDir, tui);
        }
    }

    public void testFacilityList(Path outputPath) {
        tui.setOutputPath(outputPath);
        if (dataPresent) {
            Path outDir = deleteDir(Tools5eIndexType.facility, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(Tools5eIndexType.facility);

            TestUtils.assertDirectoryContents(outDir, tui);
        }
    }

    public void testFeatList(Path outputPath) {
        tui.setOutputPath(outputPath);
        if (dataPresent) {
            Path featDir = deleteDir(Tools5eIndexType.feat, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(Tools5eIndexType.feat);

            TestUtils.assertDirectoryContents(featDir, tui);
        }
    }

    public void testItemList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (dataPresent) {
            Path itemDir = deleteDir(Tools5eIndexType.item, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(List.of(Tools5eIndexType.item, Tools5eIndexType.itemGroup));

            TestUtils.assertDirectoryContents(itemDir, tui);
        }
    }

    public void testMonsterList(Path outputPath) {
        tui.setOutputPath(outputPath);
        configurator.setUseDiceRoller(DiceRoller.disabled);

        if (dataPresent) {
            Path bestiaryDir = deleteDir(Tools5eIndexType.monster, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(List.of(Tools5eIndexType.monster, Tools5eIndexType.legendaryGroup));

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
        if (dataPresent) {
            Path out = outputPath.resolve("alt-scores");
            TestUtils.deleteDir(out);
            tui.setOutputPath(out);

            CompendiumConfig testConfig = ConfiguratorUtil.testCustomTemplate("monster",
                    TestUtils.PROJECT_PATH.resolve("examples/templates/tools5e/monster2md-scores.txt"));
            templates.setCustomTemplates(testConfig);

            MarkdownWriter writer = new MarkdownWriter(out, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(List.of(Tools5eIndexType.monster, Tools5eIndexType.legendaryGroup));
        }
    }

    public void testMonsterYamlHeader(Path outputPath) {
        if (dataPresent) {
            Path out = outputPath.resolve("yaml-header");
            TestUtils.deleteDir(out);
            tui.setOutputPath(out);
            configurator.setUseDiceRoller(DiceRoller.enabled);

            CompendiumConfig testConfig = ConfiguratorUtil.testCustomTemplate("monster",
                    TestUtils.PROJECT_PATH.resolve("examples/templates/tools5e/monster2md-yamlStatblock-header.txt"));
            templates.setCustomTemplates(testConfig);

            MarkdownWriter writer = new MarkdownWriter(out, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(List.of(Tools5eIndexType.monster, Tools5eIndexType.legendaryGroup));

            Path undead = out.resolve(index.compendiumFilePath()).resolve(linkifier().monsterPath(false, "undead"));
            assertThat(undead).exists();

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
                    if (l.equals("cssclasses: json5e-note")) {
                        index = true;
                    } else if (l.equals("statblock: true")) {
                        found = true;
                    } else if (l.equals("---")) {
                        break;
                    } else {
                        frontmatter.add(l);
                        TestUtils.commonTests(p, l, errors);
                    }
                }

                try {
                    String yamlContent = String.join("\n", frontmatter);
                    Tui.quotedYaml().load(yamlContent);
                    if (yamlContent.contains("Allip") && yamlContent.matches("\"Allip \\(.*?\\)\"")) {
                        errors.add(String.format("resource.5eInitiativeYamlNoSource contains source with the name", p));
                    }
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
        if (dataPresent) {
            Path out = outputPath.resolve("yaml-body");
            TestUtils.deleteDir(out);
            tui.setOutputPath(out);
            configurator.setUseDiceRoller(DiceRoller.enabledUsingFS);

            CompendiumConfig testConfig = ConfiguratorUtil.testCustomTemplate("monster",
                    TestUtils.PROJECT_PATH.resolve("examples/templates/tools5e/monster2md-yamlStatblock-body.txt"));
            templates.setCustomTemplates(testConfig);

            MarkdownWriter writer = new MarkdownWriter(out, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(List.of(Tools5eIndexType.monster, Tools5eIndexType.legendaryGroup));

            Path undead = out.resolve(index.compendiumFilePath()).resolve(linkifier().monsterPath(false, "undead"));
            assertThat(undead).exists();

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
                        if (l.contains("\"desc\": \"\"")) {
                            errors.add(String.format("Found empty description in %s: %s", p, l));
                        }
                    }
                    TestUtils.commonTests(p, l, errors);
                }

                try {
                    String yamlContent = String.join("\n", statblock);
                    Tui.quotedYaml().load(yamlContent);
                    if (yamlContent.contains("Allip") && yamlContent.contains("\"Allip\"")) {
                        errors.add(String.format("resource.5eStatblockYaml does not contain source with the name", p));
                    }
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

    public void testMonster2024(Path outputPath) {
        if (dataPresent) {
            Path out = outputPath.resolve("monster-2024");
            TestUtils.deleteDir(out);
            tui.setOutputPath(out);
            configurator.setUseDiceRoller(DiceRoller.enabledUsingFS);

            CompendiumConfig testConfig = ConfiguratorUtil.testCustomTemplate("monster",
                    TestUtils.PROJECT_PATH.resolve("examples/templates/tools5e/monster2md-2024.txt"));
            templates.setCustomTemplates(testConfig);

            MarkdownWriter writer = new MarkdownWriter(out, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(List.of(Tools5eIndexType.monster, Tools5eIndexType.legendaryGroup));

            TestUtils.assertDirectoryContents(out, tui);
        }
    }

    public void testObjectList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (dataPresent) {
            Path outDir = deleteDir(Tools5eIndexType.object, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(List.of(Tools5eIndexType.object));

            TestUtils.assertDirectoryContents(outDir, tui);
        }
    }

    public void testOptionalFeatureList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (dataPresent) {
            Path ofDir = deleteDir(Tools5eIndexType.optionalFeatureTypes, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(List.of(
                            Tools5eIndexType.optionalFeatureTypes,
                            Tools5eIndexType.optfeature));

            TestUtils.assertDirectoryContents(ofDir, tui);
        }
    }

    public void testPsionicList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (dataPresent) {
            Path outDir = deleteDir(Tools5eIndexType.psionic, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(Tools5eIndexType.psionic);

            TestUtils.assertDirectoryContents(outDir, tui);
        }
    }

    public void testRaceList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (dataPresent) {
            Path raceDir = deleteDir(Tools5eIndexType.race, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(Tools5eIndexType.race);

            TestUtils.assertDirectoryContents(raceDir, tui);
        }
    }

    public void testRewardList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (dataPresent) {
            Path rewardDir = deleteDir(Tools5eIndexType.reward, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(Tools5eIndexType.reward);

            if (rewardDir.toFile().exists()) {
                TestUtils.assertDirectoryContents(rewardDir, tui);
            }
        }
    }

    public void testRules(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (dataPresent) {
            deleteDir(Tools5eIndexType.adventureData, outputPath, index.compendiumFilePath());
            deleteDir(Tools5eIndexType.bookData, outputPath, index.compendiumFilePath());
            deleteDir(Tools5eIndexType.table, outputPath, index.compendiumFilePath());
            TestUtils.deleteDir(index.rulesFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer).writeFiles(Stream.of(Tools5eIndexType.values())
                    .filter(x -> x.isOutputType() && !x.useCompendiumBase())
                    .toList());

            TestUtils.assertDirectoryContents(outputPath.resolve(index.rulesFilePath()), tui);
        }
    }

    public void testSpellList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (dataPresent) {
            Path spellDir = deleteDir(Tools5eIndexType.spell, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(List.of(Tools5eIndexType.spell, Tools5eIndexType.spellIndex));

            TestUtils.assertDirectoryContents(spellDir, tui);
        }
    }

    public void testTrapsHazardsList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (dataPresent) {
            Path trapsDir = deleteDir(Tools5eIndexType.trap, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(List.of(Tools5eIndexType.trap, Tools5eIndexType.hazard));

            TestUtils.assertDirectoryContents(trapsDir, tui);
        }
    }

    public void testVehicleList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (dataPresent) {
            Path outDir = deleteDir(Tools5eIndexType.vehicle, outputPath, index.compendiumFilePath());

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            index.markdownConverter(writer)
                    .writeFiles(List.of(Tools5eIndexType.vehicle));

            if (outDir.toFile().exists()) {
                TestUtils.assertDirectoryContents(outDir, tui);
            }
        }
    }

    public Path compendiumFilePath() {
        return index.compendiumFilePath();
    }

    Path deleteDir(Tools5eIndexType type, Path outputPath, Path vaultPath) {
        final String relative = linkifier().getRelativePath(type);
        final Path typeDir = outputPath.resolve(vaultPath).resolve(relative).normalize();
        TestUtils.deleteDir(typeDir);
        return typeDir;
    }

    public void assert_Present(String key) {
        assertOrigin(key);
        assertThat(index.getNode(key))
                .describedAs(variant.name() + " should contain " + key)
                .isNotNull();
    }

    public void assert_MISSING(String key) {
        assertOrigin(key);
        assertThat(index.getNode(key))
                .describedAs(variant.name() + " should not contain " + key)
                .isNull();
    }

    public void assertOrigin(String key) {
        if (key.contains("level spell")) {
            key = key.replaceAll(" \\(.*?level spell\\)", "").trim();
        }
        assertThat(index.getOrigin(key))
                .describedAs("Origin should contain " + key)
                .isNotNull();
    }

    private static Tools5eLinkifier linkifier() {
        return Tools5eLinkifier.instance();
    }
}
