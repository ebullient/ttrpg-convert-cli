package dev.ebullient.convert.tools.dnd5e;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.ebullient.convert.TestUtils;
import dev.ebullient.convert.tools.dnd5e.CommonDataTests.TestInput;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class JsonDataNoneTest {

    static CommonDataTests commonTests;
    static final Path outputPath = TestUtils.OUTPUT_ROOT_5E.resolve("none");

    @BeforeAll
    public static void setupDir() throws Exception {
        outputPath.toFile().mkdirs();
        // This uses test/resources/sources.json to constrain sources
        commonTests = new CommonDataTests(TestInput.none, TestUtils.PATH_5E_TOOLS_DATA);
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
    public void testBackgroundList() {
        commonTests.testBackgroundList(outputPath);
    }

    @Test
    public void testClassList() {
        commonTests.testClassList(outputPath);
    }

    @Test
    public void testDeckList() {
        commonTests.testDeckList(outputPath);
    }

    @Test
    public void testDeityList() {
        commonTests.testDeityList(outputPath);
    }

    @Test
    public void testFeatList() {
        commonTests.testFeatList(outputPath);
    }

    @Test
    public void testItemList() {
        commonTests.testItemList(outputPath);
    }

    @Test
    public void testObjectList() {
        commonTests.testObjectList(outputPath);
    }

    @Test
    public void testMonsterList() {
        commonTests.testMonsterList(outputPath);
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
    public void testRaceList() {
        commonTests.testRaceList(outputPath);
    }

    @Test
    public void testRules() {
        commonTests.testRules(outputPath);
    }

    @Test
    public void testSpellList() {
        commonTests.testSpellList(outputPath);
    }

    @Test
    public void testTrapsHazardsList() {
        commonTests.testTrapsHazardsList(outputPath);
    }

    @Test
    public void testVehicleList() {
        commonTests.testVehicleList(outputPath);
    }
}
