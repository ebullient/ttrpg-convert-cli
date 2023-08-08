package dev.ebullient.convert.docs;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

    // Uset this test in IDEs
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
        Path docs = TestUtils.PROJECT_PATH.resolve("docs");
        TestUtils.assertDirectoryContents(docs, tui, (p, content) -> {
            List<String> errors = new ArrayList<>();
            content.forEach(l -> TestUtils.checkMarkdownLinks(docs.toString(), p, l, errors));
            return errors;
        });
    }
}
