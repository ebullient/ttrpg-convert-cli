package dev.ebullient.convert.tools.pf2e;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.ebullient.convert.TestUtils;
import dev.ebullient.convert.tools.pf2e.CommonDataTests.TestInput;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class Pf2eJsonDataYamlStatblockTest {
    private static CommonDataTests commonTests;

    @BeforeAll
    public static void setupDir() throws Exception {
        commonTests = new CommonDataTests(TestInput.yamlStatblocks);
    }

    @Test
    public void testCreature_pf2e() {
        Path dir = commonTests.generateNotesForType(Pf2eIndexType.creature);

        TestUtils.assertDirectoryContents(dir, commonTests.tui, TestUtils::yamlStatblockChecker);
    }
}
