package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.TestUtils;
import dev.ebullient.convert.tools.dnd5e.CommonDataTests.TestInput;
import dev.ebullient.convert.tools.dnd5e.Json2QuteMonster.MonsterFields;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class JsonDataTest {

    static CommonDataTests commonTests;
    static final Path outputPath = TestUtils.OUTPUT_ROOT_5E.resolve("all");

    @BeforeAll
    public static void setupDir() throws Exception {
        outputPath.toFile().mkdirs();
        commonTests = new CommonDataTests(TestInput.all, TestUtils.PATH_5E_TOOLS_DATA);
    }

    @AfterAll
    public static void done() {
        System.out.println("Done.");
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
    public void testMagicVariants() {
        if (!TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {
            return;
        }

        // "requires":[{"type":"HA"},{"type":"MA"}], "excludes": {"name": "Hide Armor" }
        JsonNode adamantineArmor = commonTests.index.getOrigin("magicvariant|adamantine armor|dmg");

        // "requires":[{"type":"M"}],"excludes":{"property":"2H"}
        JsonNode armBlade = commonTests.index.getOrigin("magicvariant|armblade|erlw");

        // "requires":[{"type":"R"},{"type":"T"}],
        JsonNode arrowSlaying = commonTests.index.getOrigin("magicvariant|arrow of slaying (*)|dmg");

        // "requires":[{"sword":true}]
        JsonNode luckBlade = commonTests.index.getOrigin("magicvariant|luck blade|dmg");

        // "requires":[{"type":"SCF","scfType":"arcane"}],
        // "excludes":{"name":["Staff","Rod","Wand"]}
        JsonNode orbOfShielding = commonTests.index.getOrigin("magicvariant|orb of shielding (irian quartz)|erlw");

        // "requires":[{"type":"R"},{"property":"T"}],
        // "excludes":{"net":true}
        JsonNode oceanicWeapon = commonTests.index.getOrigin("magicvariant|oceanic weapon|tdcsr");

        JsonNode x;

        x = commonTests.index.getOrigin("item|arrow|phb");
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(arrowSlaying, x))
                .describedAs("arrowSlaying: Arrow has one required property")
                .isTrue();

        x = commonTests.index.getOrigin("item|crystal|phb");
        assertThat(MagicVariant.INSTANCE.hasExcludedProperty(armBlade, x))
                .describedAs("armBlade: Crystal is not a two-handed weapon (2H)")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasExcludedProperty(orbOfShielding, x))
                .describedAs("orbOfShielding: Crystal does not have excluded name")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasExcludedProperty(oceanicWeapon, x))
                .describedAs("oceanicWeapon: Crystal does not have excluded property (net)")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(armBlade, x))
                .describedAs("armBlade: Crystal is not a melee type (M)")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(arrowSlaying, x))
                .describedAs("arrowSlaying: Crystal does not have either required property")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(luckBlade, x))
                .describedAs("luckBlade: Crystal is not a sword")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(orbOfShielding, x))
                .describedAs("orbOfShielding: Crystal has required property (SCF)")
                .isTrue();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(oceanicWeapon, x))
                .describedAs("oceanicWeapon: Crystal is not the right type (R) and does not have the right property (T)")
                .isFalse();

        x = commonTests.index.getOrigin("item|dagger|phb");
        assertThat(MagicVariant.INSTANCE.hasExcludedProperty(armBlade, x))
                .describedAs("armBlade: Dagger is not a two-handed weapon (2H)")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(armBlade, x))
                .describedAs("armBlade: Dagger is a melee type (M)")
                .isTrue();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(luckBlade, x))
                .describedAs("luckBlade: Dagger is not a sword")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(orbOfShielding, x))
                .describedAs("orbOfShielding: Dagger does not have the required property (SCF / arcane)")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(oceanicWeapon, x))
                .describedAs("oceanicWeapon: Dagger has one of two required properties")
                .isTrue();

        x = commonTests.index.getOrigin("item|greatsword|phb");
        assertThat(MagicVariant.INSTANCE.hasExcludedProperty(armBlade, x))
                .describedAs("armBlade: Greatsword is a two-handed weapon (2H)")
                .isTrue();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(luckBlade, x))
                .describedAs("luckBlade: Greatsword is a sword")
                .isTrue();

        x = commonTests.index.getOrigin("item|net|phb");
        assertThat(MagicVariant.INSTANCE.hasExcludedProperty(oceanicWeapon, x))
                .describedAs("oceanicWeapon: Net property is excluded")
                .isTrue();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(oceanicWeapon, x))
                .describedAs("oceanicWeapon: Net has the right type (R) and the right property (T)")
                .isTrue();

        x = commonTests.index.getOrigin("item|scimitar|phb");
        assertThat(MagicVariant.INSTANCE.hasExcludedProperty(armBlade, x))
                .describedAs("armBlade: Scimitar is not a two-handed weapon (2H)")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(armBlade, x))
                .describedAs("armBlade: Scimitar is a melee type (M)")
                .isTrue();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(luckBlade, x))
                .describedAs("luckBlade: Scimitar is a sword")
                .isTrue();

        x = commonTests.index.getOrigin("item|wand|phb");
        assertThat(MagicVariant.INSTANCE.hasExcludedProperty(orbOfShielding, x))
                .describedAs("orbOfShielding: Wand is an excluded name")
                .isTrue();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(orbOfShielding, x))
                .describedAs("orbOfShielding: Wand has the required property (SCF / arcane)")
                .isTrue();

        x = commonTests.index.getOrigin("item|wooden staff|phb");
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(orbOfShielding, x))
                .describedAs("orbOfShielding: Wooden staff (SCF / druid) does not have all required properties (SCF / arcane)")
                .isFalse();

        x = commonTests.index.getOrigin("item|chain mail|phb");
        assertThat(MagicVariant.INSTANCE.hasExcludedProperty(adamantineArmor, x))
                .describedAs("adamantineArmor: Chain Mail is not excluded")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(adamantineArmor, x))
                .describedAs("adamantineArmor: Chain Mail is HA")
                .isTrue();

        x = commonTests.index.getOrigin("item|hide armor|phb");
        assertThat(MagicVariant.INSTANCE.hasExcludedProperty(adamantineArmor, x))
                .describedAs("adamantineArmor: Hide Armor is excluded")
                .isTrue();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(adamantineArmor, x))
                .describedAs("adamantineArmor: Hide Armor is MA")
                .isTrue();
    }

    @Test
    public void testMonsterList() {
        commonTests.testMonsterList(outputPath);

        if (!TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {
            return;
        }

        JsonNode x;

        x = commonTests.index.getOrigin("monster|reduced-threat aboleth|tftyp");
        JsonNode hp = MonsterFields.hp.getFrom(x);
        assertThat(hp).isNotNull();
        assertThat(MonsterFields.average.getFrom(hp).toString())
                .describedAs("Reduced Threat monsters should have a template with stat modifications applied")
                .isEqualTo("67.0");
        assertThat(MonsterFields.trait.getFrom(x).toPrettyString())
                .describedAs("Reduced Threat monsters should have a template with stat modifications applied")
                .contains("Reduced Threat");
    }

    @Test
    public void testMonsterAlternateScores() {
        commonTests.testMonsterAlternateScores(outputPath);
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
    public void testObjectList() {
        commonTests.testObjectList(outputPath);
    }

    @Test
    public void testOptionalFeatureList() {
        commonTests.testOptionalFeatureList(outputPath);
    }

    @Test
    public void testPsionicList() {
        commonTests.testPsionicList(outputPath);
    }

    @Test
    public void testRaceList() {
        commonTests.testRaceList(outputPath);
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
