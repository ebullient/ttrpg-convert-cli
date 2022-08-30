package dev.ebullient.json5e;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import dev.ebullient.json5e.io.Json5eTui;
import dev.ebullient.json5e.io.MarkdownWriter;
import dev.ebullient.json5e.io.TemplatePaths;
import dev.ebullient.json5e.io.Templates;
import dev.ebullient.json5e.tools5e.IndexType;
import dev.ebullient.json5e.tools5e.Json2MarkdownConverter;
import dev.ebullient.json5e.tools5e.JsonIndex;
import io.quarkus.arc.Arc;

public class CommonDataTests {
    final static Path PROJECT_PATH = Paths.get(System.getProperty("user.dir")).toAbsolutePath();

    // for compile/test purposes. Must clone/sync separately.
    final static Path TOOLS_PATH = PROJECT_PATH.resolve("5etools-mirror-1.github.io/data");
    final static Path TEST_PATH_JSON = PROJECT_PATH.resolve("src/test/resources/paths.json");
    final static Path OUTPUT_ROOT = CommonDataTests.PROJECT_PATH.resolve("target/test-data");

    protected final Json5eTui tui;
    protected final Templates templates;
    protected JsonIndex index;

    public CommonDataTests(List<String> sources) throws Exception {
        tui = Arc.container().instance(Json5eTui.class).get();
        templates = Arc.container().instance(Templates.class).get();
        tui.init(null, true, true);

        if (TOOLS_PATH.toFile().exists()) {
            index = new JsonIndex(sources, tui)
                    .importTree(TestUtils.doParse(TEST_PATH_JSON));
            TestUtils.fullIndex(index, TOOLS_PATH, tui);
            index.prepare();
        }
    }

    public void cleanup() {
        templates.setCustomTemplates(null);
        tui.close();
    }

    public void testKeyIndex(Path outputPath) throws Exception {
        if (TOOLS_PATH.toFile().exists()) {
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

    public void testNameList(Path outputPath) {
        if (TOOLS_PATH.toFile().exists()) {
            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.namelist);
        }
    }

    public void testFeatList(Path outputPath) {
        if (TOOLS_PATH.toFile().exists()) {
            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.feat);
        }
    }

    public void testBackgroundList(Path outputPath) {
        if (TOOLS_PATH.toFile().exists()) {
            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.background);
        }
    }

    public void testSpellList(Path outputPath) {
        if (TOOLS_PATH.toFile().exists()) {
            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.spell);
        }
    }

    public void testRaceList(Path outputPath) {
        if (TOOLS_PATH.toFile().exists()) {
            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.race);
        }
    }

    public void testClassList(Path outputPath) {
        if (TOOLS_PATH.toFile().exists()) {
            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.classtype);
        }
    }

    public void testItemList(Path outputPath) {
        if (TOOLS_PATH.toFile().exists()) {
            // TODO: objects, vehicles, magicvariants

            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.item);
        }
    }

    public void testMonsterList(Path outputPath) {
        if (TOOLS_PATH.toFile().exists()) {
            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.monster);
        }
    }

    public void testMonsterYamlHeader(Path outputPath) {
        if (TOOLS_PATH.toFile().exists()) {
            TemplatePaths templatePaths = new TemplatePaths();
            templatePaths.setCustomTemplate("monster2md.txt",
                    PROJECT_PATH.resolve("src/main/resources/templates/monster2md-yamlStatblock-header.txt"));
            templates.setCustomTemplates(templatePaths);

            MarkdownWriter writer = new MarkdownWriter(outputPath.resolve("yaml-header"), templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.monster);
        }
    }

    public void testMonsterYamlBody(Path outputPath) {
        if (TOOLS_PATH.toFile().exists()) {
            TemplatePaths templatePaths = new TemplatePaths();
            templatePaths.setCustomTemplate("monster2md.txt",
                    PROJECT_PATH.resolve("src/main/resources/templates/monster2md-yamlStatblock-body.txt"));
            templates.setCustomTemplates(templatePaths);

            MarkdownWriter writer = new MarkdownWriter(outputPath.resolve("yaml-body"), templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.monster);
        }
    }

    public void testRules(Path outputPath) {
        if (TOOLS_PATH.toFile().exists()) {
            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeRulesAndTables();
        }
    }
}
