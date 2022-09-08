package dev.ebullient.json5e;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class JsonDataSubsetTest {

    static CommonDataTests commonTests;
    static final Path outputPath = TestUtils.OUTPUT_ROOT.resolve("subset");

    @BeforeAll
    public static void setupDir() throws Exception {
        outputPath.toFile().mkdirs();
        // A Tree Blight is from CoS, but appears in WbtW. It is listed in "otherSources" (should pull that entry in)
        commonTests = new CommonDataTests(true);
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
    public void testNameList() {
        commonTests.testNameList(outputPath);
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
    }

    @Test
    public void testClassList() {
        commonTests.testClassList(outputPath);
    }

    @Test
    public void testItemList() {
        commonTests.testItemList(outputPath);
    }

    @Test
    public void testMonsterList() {
        commonTests.testMonsterList(outputPath);
    }

    @Test
    public void testMonsterYamlHeader() {
        commonTests.testMonsterYamlHeader(outputPath);
    }

    @Test
    public void testMonsterYamlBody() {
        commonTests.testMonsterYamlBody(outputPath);
    }

    @Test
    public void testRules() {
        commonTests.testRules(outputPath);
    }
}
