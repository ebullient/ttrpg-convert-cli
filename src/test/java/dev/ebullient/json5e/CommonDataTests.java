package dev.ebullient.json5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import dev.ebullient.json5e.io.Json5eTui;
import dev.ebullient.json5e.io.MarkdownWriter;
import dev.ebullient.json5e.io.TemplatePaths;
import dev.ebullient.json5e.io.Templates;
import dev.ebullient.json5e.qute.QuteSource;
import dev.ebullient.json5e.tools5e.IndexType;
import dev.ebullient.json5e.tools5e.Json2MarkdownConverter;
import dev.ebullient.json5e.tools5e.JsonIndex;
import io.quarkus.arc.Arc;

public class CommonDataTests {
    protected final Json5eTui tui;
    protected final Templates templates;
    protected JsonIndex index;

    public CommonDataTests(boolean useSources) throws Exception {
        tui = Arc.container().instance(Json5eTui.class).get();
        templates = Arc.container().instance(Templates.class).get();
        tui.init(null, false, false);

        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            if (useSources) {
                index = new JsonIndex(List.of(), tui);
                tui.readFile(TestUtils.TEST_SOURCES_JSON, index::importTree);
            } else {
                index = new JsonIndex(List.of("*"), tui);
            }
            tui.readFile(TestUtils.TEST_PATH_JSON, index::importTree);

            for (String x : List.of("adventures.json", "books.json", "adventure/adventure-wdh.json",
                    "book/book-vgm.json", "book/book-phb.json")) {
                tui.readFile(TestUtils.TOOLS_PATH.resolve(x), index::importTree);
            }
            tui.read5eTools(TestUtils.TOOLS_PATH, index::importTree);

            index.prepare();
        }
    }

    public void cleanup() {
        templates.setCustomTemplates(null);
        tui.close();
    }

    public void testKeyIndex(Path outputPath) throws Exception {
        tui.setOutputPath(outputPath);
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path p1Full = outputPath.resolve("allIndex.json");
            index.writeIndex(p1Full);
            Path p1Source = outputPath.resolve("allSourceIndex.json");
            index.writeSourceIndex(p1Source);

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
            new Json2MarkdownConverter(index, writer)
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
            new Json2MarkdownConverter(index, writer)
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
            new Json2MarkdownConverter(index, writer)
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
            new Json2MarkdownConverter(index, writer)
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
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.classtype);

            TestUtils.assertDirectoryContents(classDir, tui);
        }
    }

    public void testDeityList(Path outputPath) {
        tui.setOutputPath(outputPath);

        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path deitiesDir = outputPath.resolve(index.compendiumPath()).resolve(QuteSource.DEITIES_PATH);
            TestUtils.deleteDir(deitiesDir);

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            new Json2MarkdownConverter(index, writer)
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
            new Json2MarkdownConverter(index, writer)
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
            new Json2MarkdownConverter(index, writer)
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

            TemplatePaths templatePaths = new TemplatePaths();
            templatePaths.setCustomTemplate("monster",
                    TestUtils.PROJECT_PATH.resolve("src/main/resources/templates/monster2md-scores.txt"));
            templates.setCustomTemplates(templatePaths);

            MarkdownWriter writer = new MarkdownWriter(out, templates, tui);
            new Json2MarkdownConverter(index, writer).writeFiles(IndexType.monster);
        }
    }

    public void testMonsterYamlHeader(Path outputPath) {
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path out = outputPath.resolve("yaml-header");
            TestUtils.deleteDir(out);

            tui.setOutputPath(out);

            TemplatePaths templatePaths = new TemplatePaths();
            templatePaths.setCustomTemplate("monster",
                    TestUtils.PROJECT_PATH.resolve("src/main/resources/templates/monster2md-yamlStatblock-header.txt"));
            templates.setCustomTemplates(templatePaths);

            MarkdownWriter writer = new MarkdownWriter(out, templates, tui);
            new Json2MarkdownConverter(index, writer).writeFiles(IndexType.monster);

        }
    }

    public void testMonsterYamlBody(Path outputPath) {
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            Path out = outputPath.resolve("yaml-body");
            TestUtils.deleteDir(out);
            tui.setOutputPath(out);

            TemplatePaths templatePaths = new TemplatePaths();
            templatePaths.setCustomTemplate("monster",
                    TestUtils.PROJECT_PATH.resolve("src/main/resources/templates/monster2md-yamlStatblock-body.txt"));
            templates.setCustomTemplates(templatePaths);

            MarkdownWriter writer = new MarkdownWriter(out, templates, tui);
            new Json2MarkdownConverter(index, writer).writeFiles(IndexType.monster);

            Path undead = out.resolve(index.compendiumPath()).resolve(QuteSource.monsterPath(false, "undead"));
            assertThat(undead.toFile()).exists();

            TestUtils.assertDirectoryContents(undead, tui, (p, content) -> {
                List<String> e = new ArrayList<>();
                boolean found = false;
                boolean yaml = false;
                boolean index = false;
                TestUtils.checkContents.apply(p, content);

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
            new Json2MarkdownConverter(index, writer)
                    .writeRulesAndTables();
        }
    }
}
