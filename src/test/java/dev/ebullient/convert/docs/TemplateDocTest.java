package dev.ebullient.convert.docs;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import dev.ebullient.convert.TestUtils;
import dev.ebullient.convert.io.MarkdownDoclet;
import dev.ebullient.convert.io.Tui;

public class TemplateDocTest {
    protected static Tui tui;

    @BeforeAll
    public static void prepare() {
        tui = new Tui();
        tui.init(null, true, false);
    }

    // Use this test in IDEs
    @Test
    @DisabledIfSystemProperty(named = "maven.home", matches = ".*")
    public void buildVerifyDocs() throws Exception {
        MarkdownDoclet.main(null);
        verifyDocs();
    }

    // Use this test in maven builds
    @Test
    @EnabledIfSystemProperty(named = "maven.home", matches = ".*")
    public void verifyDocs() throws Exception {
        TestUtils.assertMarkdownLinks(TestUtils.PROJECT_PATH.resolve("docs"), tui);
        TestUtils.assertMarkdownLinks(TestUtils.PROJECT_PATH.resolve("examples"), tui);
        TestUtils.assertMarkdownLinks(TestUtils.PROJECT_PATH.resolve("README.md"), tui);
        TestUtils.assertMarkdownLinks(TestUtils.PROJECT_PATH.resolve("README-WINDOWS.md"), tui);
        TestUtils.assertMarkdownLinks(TestUtils.PROJECT_PATH.resolve("CHANGELOG.md"), tui);
    }
}
