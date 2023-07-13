package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.ebullient.convert.TestUtils;
import dev.ebullient.convert.tools.dnd5e.CommonDataTests.TestInput;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class JsonDataSubsetTest {

    static CommonDataTests commonTests;
    static final Path outputPath = TestUtils.OUTPUT_ROOT_5E.resolve("subset");

    @BeforeAll
    public static void setupDir() throws Exception {
        outputPath.toFile().mkdirs();
        // This uses test/resources/sources.json to constrain sources
        commonTests = new CommonDataTests(TestInput.subset);
    }

    @AfterEach
    public void cleanup() {
        commonTests.cleanup();
    }

    @Test
    public void testKeyIndex() throws Exception {
        commonTests.testKeyIndex(outputPath);
    }

    @Test
    public void testFeatList() {
        commonTests.testFeatList(outputPath);
    }

    @Test
    public void testBackgroundList() {
        commonTests.testBackgroundList(outputPath);
    }

    @Test
    public void testSpellList() {
        commonTests.testSpellList(outputPath);
    }

    @Test
    public void testRaceList() {
        commonTests.testRaceList(outputPath);

        // Changeling from mpmm is a reprint..
        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            final String raceRelative = Tools5eQuteBase.getRelativePath(Tools5eIndexType.race);

            // Single included race: changeling from mpmm
            Path changeling = outputPath
                    .resolve(commonTests.compendiumFilePath())
                    .resolve(raceRelative)
                    .resolve("changeling-mpmm.md");
            assertThat(changeling).exists();
        }
    }

    @Test
    public void testClassList() {
        commonTests.testClassList(outputPath);
    }

    @Test
    public void testDeityList() {
        commonTests.testDeityList(outputPath);
    }

    @Test
    public void testItemList() {
        commonTests.testItemList(outputPath);
    }

    @Test
    public void testMonsterList() {
        commonTests.testMonsterList(outputPath);

        if (TestUtils.TOOLS_PATH_5E.toFile().exists()) {
            // Tree blight is from Curse of Strahd, but is also present in
            // The Wild Beyond the Witchlight --> an "otherSource".
            // The tree blight should be included when WBtW is included
            Path treeBlight = outputPath
                    .resolve(commonTests.compendiumFilePath())
                    .resolve(Tools5eQuteBase.monsterPath(false, "plant"))
                    .resolve("tree-blight-cos.md");
            assertThat(treeBlight).exists();
        }
    }

    @Test
    public void testOptionalFeatureList() {
        commonTests.testOptionalFeatureList(outputPath);
    }

    @Test
    public void testRewardList() {
        commonTests.testRewardList(outputPath);
    }

    @Test
    public void testRules() {
        commonTests.testRules(outputPath);
    }

    @Test
    public void testTrapsHazardsList() {
        commonTests.testTrapsHazardsList(outputPath);
    }
}
