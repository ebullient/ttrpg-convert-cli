package dev.ebullient.json5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
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
    protected final Json5eTui tui;
    protected final Templates templates;
    protected JsonIndex index;

    public CommonDataTests(boolean useSources) throws Exception {
        tui = Arc.container().instance(Json5eTui.class).get();
        templates = Arc.container().instance(Templates.class).get();
        tui.init(null, true, true);

        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            if (useSources) {
                index = new JsonIndex(List.of(), tui);
                tui.readFile(TestUtils.TEST_SOURCES_JSON, index.importFile());
            } else {
                index = new JsonIndex(List.of("*"), tui);
            }
            tui.readFile(TestUtils.TEST_PATH_JSON, index.importFile());

            for (String x : List.of("adventures.json", "books.json", "adventure/adventure-wdh.json",
                    "book/book-vgm.json", "book/book-phb.json")) {
                tui.readFile(TestUtils.TOOLS_PATH.resolve(x), index.importFile());
            }
            tui.read5eTools(TestUtils.TOOLS_PATH, index.importFile());

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
            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.feat);
        }
    }

    public void testBackgroundList(Path outputPath) {
        tui.setOutputPath(outputPath);
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.background)
                    .writeRulesAndTables();
        }
    }

    public void testSpellList(Path outputPath) {
        tui.setOutputPath(outputPath);
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.spell);
        }
    }

    public void testRaceList(Path outputPath) {
        tui.setOutputPath(outputPath);
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.race);
        }
    }

    public void testClassList(Path outputPath) {
        tui.setOutputPath(outputPath);
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.classtype);
        }
    }

    public void testDeityList(Path outputPath) {
        tui.setOutputPath(outputPath);
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.deity);

            Path imageDir = outputPath.resolve(index.compendiumPath()).resolve("deities/img");
            assertThat(imageDir.toFile().exists()).isTrue();
        }
    }

    public void testItemList(Path outputPath) {
        tui.setOutputPath(outputPath);
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.item);
        }
    }

    public void testMonsterList(Path outputPath) {
        tui.setOutputPath(outputPath);
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            MarkdownWriter writer = new MarkdownWriter(outputPath, templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.monster);
        }
    }

    public void testMonsterYamlHeader(Path outputPath) {
        tui.setOutputPath(outputPath);
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            TemplatePaths templatePaths = new TemplatePaths();
            templatePaths.setCustomTemplate("monster2md.txt",
                    TestUtils.PROJECT_PATH.resolve("src/main/resources/templates/monster2md-yamlStatblock-header.txt"));
            templates.setCustomTemplates(templatePaths);

            MarkdownWriter writer = new MarkdownWriter(outputPath.resolve("yaml-header"), templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.monster);
        }
    }

    public void testMonsterYamlBody(Path outputPath) {
        tui.setOutputPath(outputPath);
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            TemplatePaths templatePaths = new TemplatePaths();
            templatePaths.setCustomTemplate("monster2md.txt",
                    TestUtils.PROJECT_PATH.resolve("src/main/resources/templates/monster2md-yamlStatblock-body.txt"));
            templates.setCustomTemplates(templatePaths);

            MarkdownWriter writer = new MarkdownWriter(outputPath.resolve("yaml-body"), templates, tui);
            new Json2MarkdownConverter(index, writer)
                    .writeFiles(IndexType.monster);
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
