package dev.ebullient.convert.tools.pf2e;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.ebullient.convert.config.CompendiumConfig.DiceRoller;
import dev.ebullient.convert.tools.pf2e.CommonDataTests.TestInput;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class Pf2eJsonDataNoneTest {

    static CommonDataTests commonTests;

    @BeforeAll
    public static void setupDir() throws Exception {
        commonTests = new CommonDataTests(TestInput.none);
    }

    @AfterAll
    public static void done() {
        System.out.println("Done.");
    }

    @Test
    public void testIndex_p2fe() throws Exception {
        commonTests.testDataIndex_pf2e();
    }

    @Test
    public void testAbility_p2fe() throws Exception {
        commonTests.generateNotesForType(Pf2eIndexType.ability);
    }

    @Test
    public void testAction_p2fe() throws Exception {
        commonTests.generateNotesForType(Pf2eIndexType.action);
    }

    @Test
    public void testAffliction_p2fe() throws Exception {
        commonTests.generateNotesForType(List.of(Pf2eIndexType.curse, Pf2eIndexType.disease));
    }

    @Test
    public void testArchetype_p2fe() throws Exception {
        commonTests.generateNotesForType(Pf2eIndexType.archetype);
    }

    @Test
    public void testBackground_p2fe() throws Exception {
        commonTests.generateNotesForType(Pf2eIndexType.background);
    }

    @Test
    public void testDeity_p2fe() throws Exception {
        commonTests.generateNotesForType(Pf2eIndexType.deity);
    }

    @Test
    public void testDomain_p2fe() throws Exception {
        commonTests.generateNotesForType(Pf2eIndexType.domain);
    }

    @Test
    public void testFeat_p2fe() throws Exception {
        commonTests.generateNotesForType(Pf2eIndexType.feat);
    }

    @Test
    public void testHazard_p2fe() throws Exception {
        commonTests.generateNotesForType(Pf2eIndexType.hazard);
    }

    @Test
    public void testItem_p2fe() throws Exception {
        commonTests.configurator.setUseDiceRoller(DiceRoller.enabled);
        commonTests.generateNotesForType(Pf2eIndexType.item);
    }

    @Test
    public void testRitual_p2fe() throws Exception {
        commonTests.generateNotesForType(Pf2eIndexType.ritual);
    }

    @Test
    public void testSpell_p2fe() throws Exception {
        commonTests.generateNotesForType(Pf2eIndexType.spell);
    }

    @Test
    public void testTable_p2fe() throws Exception {
        commonTests.generateNotesForType(Pf2eIndexType.table);
    }

    @Test
    public void testTrait_p2fe() throws Exception {
        commonTests.generateNotesForType(Pf2eIndexType.trait);
    }

    @Test
    public void testNotes_p2fe() throws Exception {
        commonTests.testNotes_p2fe();
    }
}
