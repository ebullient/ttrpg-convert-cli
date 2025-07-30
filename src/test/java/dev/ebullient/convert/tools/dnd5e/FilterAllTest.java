package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
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
public class FilterAllTest {

    static CommonDataTests commonTests;
    static final TestInput testInput = TestInput.all;
    static final Path outputPath = TestUtils.OUTPUT_5E_DATA.resolve(testInput.name());

    @BeforeAll
    public static void setupDir() throws Exception {
        outputPath.toFile().mkdirs();
        // extra escaping for regex as we're reading from string
        String config = """
                {
                    "reprintBehavior": "all",
                    "sources": {
                        "book": [
                            "XGE",
                            "XMM"
                        ],
                        "adventure": [
                            "OotA",
                            "DIP"
                        ],
                        "reference": [
                            "*"
                        ]
                    },
                    "include": [
                        "race|changeling|mpmm"
                    ],
                    "exclude": [
                        "monster|expert|dc",
                        "monster|expert|sdw",
                        "monster|expert|slw"
                    ],
                    "excludePattern": [
                        "race\\\\|.*\\\\|dmg"
                    ],
                    "paths": {
                        "rules": "rules/",
                        "compendium": ""
                    },
                    "images": {
                        "copyInternal": false
                    },
                    "useDiceRoller" : true
                }
                """.stripIndent();
        commonTests = new CommonDataTests(testInput, config, TestUtils.PATH_5E_TOOLS_DATA);
    }

    @AfterAll
    public static void done() throws IOException {
        commonTests.afterAll(outputPath);
    }

    @AfterEach
    public void cleanup() throws Exception {
        commonTests.afterEach();
    }

    @Test
    public void testKeyIndex() throws Exception {
        commonTests.testKeyIndex(outputPath);

        // All sources, but reprints will be followed.
        // PHB elements should be missing/replaced by XPHB equivalents (e.g.)
        if (commonTests.dataPresent) {
            var config = commonTests.config;

            assertThat(config.sourceIncluded("srd")).isTrue();
            assertThat(config.sourceIncluded("basicrules")).isTrue();
            assertThat(config.sourceIncluded("srd52")).isTrue();
            assertThat(config.sourceIncluded("basicRules2024")).isTrue();

            assertThat(config.sourceIncluded("DMG")).isTrue();
            assertThat(config.sourceIncluded("PHB")).isTrue();

            assertThat(config.sourceIncluded("XDMG")).isTrue();
            assertThat(config.sourceIncluded("XPHB")).isTrue();

            commonTests.assert_Present("action|attack|phb");
            commonTests.assert_Present("action|attack|xphb");
            commonTests.assert_Present("action|cast a spell|phb");
            commonTests.assert_Present("action|disengage|phb");
            commonTests.assert_Present("action|disengage|xphb");

            commonTests.assert_Present("feat|alert|phb");
            commonTests.assert_Present("feat|alert|xphb");
            commonTests.assert_Present("feat|dueling|xphb");
            commonTests.assert_Present("feat|grappler|phb");
            commonTests.assert_Present("feat|grappler|xphb");
            commonTests.assert_Present("feat|mobile|phb");
            commonTests.assert_Present("feat|moderately armored|phb");
            commonTests.assert_Present("feat|moderately armored|xphb");

            commonTests.assert_Present("variantrule|facing|dmg");
            commonTests.assert_Present("variantrule|falling|xge");
            commonTests.assert_Present("variantrule|familiars|mm");
            commonTests.assert_Present("variantrule|simultaneous effects|xge");
            commonTests.assert_Present("variantrule|simultaneous effects|xphb");

            commonTests.assert_Present("background|sage|phb");
            commonTests.assert_Present("background|sage|xphb");
            commonTests.assert_Present("background|baldur's gate acolyte|bgdia");

            commonTests.assert_Present("condition|blinded|phb");
            commonTests.assert_Present("condition|blinded|xphb");

            commonTests.assert_Present("deity|auril|faerûnian|scag");
            commonTests.assert_Present("deity|auril|forgotten realms|phb");
            commonTests.assert_Present("deity|chemosh|dragonlance|dsotdq");
            commonTests.assert_Present("deity|chemosh|dragonlance|phb");
            commonTests.assert_Present("deity|ehlonna|greyhawk|phb");
            commonTests.assert_Present("deity|ehlonna|greyhawk|xdmg");
            commonTests.assert_Present("deity|gruumsh|dawn war|dmg");
            commonTests.assert_Present("deity|gruumsh|exandria|egw");
            commonTests.assert_Present("deity|gruumsh|nonhuman|phb");
            commonTests.assert_Present("deity|gruumsh|orc|scag");
            commonTests.assert_Present("deity|gruumsh|orc|vgm");
            commonTests.assert_Present("deity|the traveler|eberron|erlw");
            commonTests.assert_Present("deity|the traveler|eberron|phb");
            commonTests.assert_Present("deity|the traveler|exandria|egw");
            commonTests.assert_Present("deity|the traveler|exandria|tdcsr");

            commonTests.assert_Present("disease|cackle fever|dmg");
            commonTests.assert_Present("disease|cackle fever|xdmg");

            commonTests.assert_Present("hazard|quicksand pit|xdmg");
            commonTests.assert_Present("hazard|quicksand|dmg");
            commonTests.assert_Present("hazard|razorvine|dmg");
            commonTests.assert_Present("hazard|razorvine|xdmg");

            commonTests.assert_Present("itemgroup|arcane focus|phb");
            commonTests.assert_Present("itemgroup|arcane focus|xphb");
            commonTests.assert_Present("itemgroup|carpet of flying|dmg");
            commonTests.assert_Present("itemgroup|carpet of flying|xdmg");
            commonTests.assert_Present("itemgroup|ioun stone|dmg");
            commonTests.assert_Present("itemgroup|ioun stone|llk");
            commonTests.assert_Present("itemgroup|ioun stone|xdmg");
            commonTests.assert_Present("itemgroup|musical instrument|phb");
            commonTests.assert_Present("itemgroup|musical instrument|scag");
            commonTests.assert_Present("itemgroup|musical instrument|xphb");
            commonTests.assert_Present("itemgroup|spell scroll|dmg");
            commonTests.assert_Present("itemgroup|spell scroll|xdmg");

            commonTests.assert_Present("itemproperty|2h|phb");
            commonTests.assert_Present("itemproperty|2h|xphb");
            commonTests.assert_Present("itemproperty|bf|dmg");
            commonTests.assert_Present("itemproperty|bf|xdmg");
            commonTests.assert_Present("itemproperty|er|tdcsr");
            commonTests.assert_Present("itemproperty|s|phb");

            commonTests.assert_Present("itemtype|$c|phb");
            commonTests.assert_Present("itemtype|$c|xphb");
            commonTests.assert_Present("itemtype|$g|dmg");
            commonTests.assert_Present("itemtype|$g|xdmg");
            commonTests.assert_Present("itemtype|sc|dmg");
            commonTests.assert_Present("itemtype|sc|xphb");
            commonTests.assert_Present("itemtypeadditionalentries|gs|phb|xge");

            commonTests.assert_Present("item|+1 rod of the pact keeper|dmg");
            commonTests.assert_Present("item|+1 rod of the pact keeper|xdmg");
            commonTests.assert_Present("item|+2 wraps of unarmed power|xdmg");
            commonTests.assert_Present("item|+2 wraps of unarmed prowess|bmt");
            commonTests.assert_Present("item|acid (vial)|phb");
            commonTests.assert_Present("item|acid absorbing tattoo|tce");
            commonTests.assert_Present("item|acid|xphb");
            commonTests.assert_Present("item|alchemist's doom|scc");
            commonTests.assert_Present("item|alchemist's fire (flask)|phb");
            commonTests.assert_Present("item|alchemist's fire|xphb");
            commonTests.assert_Present("item|alchemist's supplies|phb");
            commonTests.assert_Present("item|alchemist's supplies|xphb");
            commonTests.assert_Present("item|amulet of health|dmg");
            commonTests.assert_Present("item|amulet of health|xdmg");
            commonTests.assert_Present("item|amulet of proof against detection and location|dmg");
            commonTests.assert_Present("item|amulet of proof against detection and location|xdmg");
            commonTests.assert_Present("item|armor of invulnerability|dmg");
            commonTests.assert_Present("item|armor of invulnerability|xdmg");
            commonTests.assert_Present("item|automatic pistol|dmg");
            commonTests.assert_Present("item|automatic rifle|dmg");
            commonTests.assert_Present("item|automatic rifle|xdmg");
            commonTests.assert_Present("item|ball bearings (bag of 1,000)|phb");
            commonTests.assert_Present("item|ball bearings|xphb");
            commonTests.assert_Present("item|ball bearing|phb");
            commonTests.assert_Present("item|chain (10 feet)|phb");
            commonTests.assert_Present("item|chain|xphb");

            commonTests.assert_Present("monster|abjurer wizard|mpmm");
            commonTests.assert_Present("monster|abjurer|vgm");
            commonTests.assert_Present("monster|alkilith|mpmm");
            commonTests.assert_Present("monster|alkilith|mtf");
            commonTests.assert_Present("monster|animated object (huge)|phb");
            commonTests.assert_Present("monster|ape|mm");
            commonTests.assert_Present("monster|ape|xmm");
            commonTests.assert_Present("monster|ash zombie|lmop");
            commonTests.assert_Present("monster|ash zombie|pabtso");
            commonTests.assert_Present("monster|awakened shrub|mm");
            commonTests.assert_Present("monster|awakened shrub|xmm");
            commonTests.assert_Present("monster|beast of the land|tce");
            commonTests.assert_Present("monster|beast of the land|xphb");
            commonTests.assert_Present("monster|bestial spirit (air)|tce");
            commonTests.assert_Present("monster|bestial spirit (air)|xphb");
            commonTests.assert_Present("monster|cat|mm");
            commonTests.assert_Present("monster|cat|xmm");
            commonTests.assert_Present("monster|derro savant|mpmm");
            commonTests.assert_Present("monster|derro savant|mtf");
            commonTests.assert_Present("monster|derro savant|oota");
            commonTests.assert_Present("monster|sibriex|mpmm");
            commonTests.assert_Present("monster|sibriex|mtf");

            commonTests.assert_Present("object|trebuchet|dmg");
            commonTests.assert_Present("object|trebuchet|xdmg");

            commonTests.assert_Present("optfeature|ambush|tce");
            commonTests.assert_Present("optfeature|ambush|xphb");
            commonTests.assert_Present("optfeature|investment of the chain master|tce");
            commonTests.assert_Present("optfeature|investment of the chain master|xphb");

            commonTests.assert_Present("reward|blessing of weapon enhancement|dmg");
            commonTests.assert_Present("reward|blessing of weapon enhancement|xdmg");
            commonTests.assert_Present("reward|blessing of wound closure|dmg");
            commonTests.assert_Present("reward|blessing of wound closure|xdmg");
            commonTests.assert_Present("reward|boon of combat prowess|dmg");
            commonTests.assert_Present("reward|boon of dimensional travel|dmg");
            commonTests.assert_Present("reward|boon of fate|dmg");
            commonTests.assert_Present("reward|boon of fortitude|dmg");
            commonTests.assert_Present("reward|boon of high magic|dmg");

            commonTests.assert_Present("sense|blindsight|phb");
            commonTests.assert_Present("sense|blindsight|xphb");

            commonTests.assert_Present("skill|athletics|phb");
            commonTests.assert_Present("skill|athletics|xphb");

            commonTests.assert_Present("spell|acid splash|phb");
            commonTests.assert_Present("spell|acid splash|xphb");
            commonTests.assert_Present("spell|aganazzar's scorcher|xge");
            commonTests.assert_Present("spell|blade barrier|phb");
            commonTests.assert_Present("spell|blade barrier|xphb");
            commonTests.assert_Present("spell|feeblemind|phb");
            commonTests.assert_Present("spell|illusory dragon|xge");
            commonTests.assert_Present("spell|illusory script|phb");
            commonTests.assert_Present("spell|illusory script|xphb");
            commonTests.assert_Present("spell|wrath of nature|xge");

            commonTests.assert_Present("status|surprised|phb");
            commonTests.assert_Present("status|surprised|xphb");

            commonTests.assert_Present("subclass|thief|rogue|phb|phb");
            commonTests.assert_MISSING("subclass|thief|rogue|xphb|phb");
            commonTests.assert_Present("subclass|thief|rogue|xphb|xphb");

            commonTests.assert_Present("trap|collapsing roof|dmg");
            commonTests.assert_Present("trap|collapsing roof|xdmg");
            commonTests.assert_Present("trap|falling net|dmg");
            commonTests.assert_Present("trap|falling net|xdmg");
            commonTests.assert_Present("trap|pits|dmg");
            commonTests.assert_Present("trap|hidden pit|xdmg");
            commonTests.assert_Present("trap|poison darts|dmg");
            commonTests.assert_Present("trap|poison needle trap|xge");
            commonTests.assert_Present("trap|poison needle|dmg");
            commonTests.assert_Present("trap|poisoned darts|xdmg");
            commonTests.assert_Present("trap|rolling sphere|dmg");
            commonTests.assert_Present("trap|rolling stone|xdmg");

            commonTests.assert_Present("vehicle|apparatus of kwalish|dmg");
            commonTests.assert_Present("vehicle|apparatus of kwalish|xdmg");

            // Classes, subclasses, class features, and subclass features

            commonTests.assert_Present("classtype|artificer|tce");

            // "Path of Wild Magic|Barbarian||Wild Magic|TCE|3",
            // "Bolstering Magic|Barbarian||Wild Magic|TCE|6",
            // "Unstable Backlash|Barbarian||Wild Magic|TCE|10",
            // "Controlled Surge|Barbarian||Wild Magic|TCE|14",

            commonTests.assert_Present("classtype|barbarian|phb");
            commonTests.assert_Present("classtype|barbarian|xphb");

            commonTests.assert_Present("subclass|path of wild magic|barbarian|phb|tce");
            commonTests.assert_MISSING("subclass|path of wild magic|barbarian|xphb|tce");

            commonTests.assert_Present("subclassfeature|bolstering magic|barbarian|phb|wild magic|tce|6|tce");
            commonTests.assert_Present("subclassfeature|controlled surge|barbarian|phb|wild magic|tce|14|tce");
            commonTests.assert_Present("subclassfeature|magic awareness|barbarian|phb|wild magic|tce|3|tce");
            commonTests.assert_Present("subclassfeature|path of wild magic|barbarian|phb|wild magic|tce|3|tce");
            commonTests.assert_Present("subclassfeature|unstable backlash|barbarian|phb|wild magic|tce|10|tce");
            commonTests.assert_Present("subclassfeature|wild surge|barbarian|phb|wild magic|tce|3|tce");

            // "Thief|Rogue||Thief||3",
            // "Supreme Sneak|Rogue||Thief||9",
            // "Use Magic Device|Rogue||Thief||13",
            // "Thief's Reflexes|Rogue||Thief||17"

            commonTests.assert_Present("classtype|rogue|phb");
            commonTests.assert_Present("classtype|rogue|xphb");

            commonTests.assert_Present("subclass|thief|rogue|phb|phb");
            commonTests.assert_MISSING("subclass|thief|rogue|xphb|phb");
            commonTests.assert_Present("subclass|thief|rogue|xphb|xphb");

            commonTests.assert_Present("subclassfeature|thief|rogue|phb|thief|phb|3|phb");
            commonTests.assert_Present("subclassfeature|thief|rogue|xphb|thief|xphb|3|xphb");
            commonTests.assert_Present("subclassfeature|supreme sneak|rogue|phb|thief|phb|9|phb");
            commonTests.assert_Present("subclassfeature|supreme sneak|rogue|xphb|thief|xphb|9|xphb");
            commonTests.assert_Present("subclassfeature|use magic device|rogue|phb|thief|phb|13|phb");
            commonTests.assert_Present("subclassfeature|use magic device|rogue|xphb|thief|xphb|13|xphb");
            commonTests.assert_Present("subclassfeature|thief's reflexes|rogue|phb|thief|phb|17|phb");
            commonTests.assert_Present("subclassfeature|thief's reflexes|rogue|xphb|thief|xphb|17|xphb");

            // Races and subraces

            commonTests.assert_Present("race|bugbear|erlw");
            commonTests.assert_Present("race|bugbear|mpmm");
            commonTests.assert_Present("race|bugbear|vgm");
            commonTests.assert_Present("race|human|phb");
            commonTests.assert_Present("race|human|xphb");
            commonTests.assert_Present("race|tiefling|phb");
            commonTests.assert_Present("race|tiefling|xphb");
            commonTests.assert_Present("race|warforged|erlw");
            commonTests.assert_Present("race|yuan-ti pureblood|vgm");
            commonTests.assert_Present("race|yuan-ti|mpmm");

            commonTests.assert_Present("subrace|genasi (air)|genasi|eepc|eepc");
            commonTests.assert_Present("subrace|genasi (air)|genasi|mpmm|mpmm");
            commonTests.assert_Present("subrace|human|human|phb|phb");
            commonTests.assert_Present("subrace|tiefling (zariel)|tiefling|phb|mtf");
            commonTests.assert_Present("subrace|tiefling|tiefling|phb|phb");
            commonTests.assert_Present("subrace|vampire (ixalan)|vampire|psz|psx");
        }
    }

    @Test
    public void testAdventures() {
        commonTests.testAdventures(outputPath);
    }

    @Test
    public void testBackgroundList() {
        commonTests.testBackgroundList(outputPath);
    }

    @Test
    public void testBooks() {
        commonTests.testBookList(outputPath);
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
    public void testFacilityList() {
        commonTests.testFacilityList(outputPath);
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
        assertThat(adamantineArmor).isNotNull();

        // "requires":[{"type":"M"}],"excludes":{"property":"2H"}
        JsonNode armBlade = commonTests.index.getOrigin("magicvariant|armblade|erlw");
        assertThat(armBlade).isNotNull();

        // "requires":[{"type":"R"},{"type":"T"}],
        JsonNode arrowSlaying = commonTests.index.getOrigin("magicvariant|arrow of slaying (*)|dmg");
        assertThat(arrowSlaying).isNotNull();

        // "requires":[{"sword":true}]
        JsonNode luckBlade = commonTests.index.getOrigin("magicvariant|luck blade|dmg");
        assertThat(luckBlade).isNotNull();

        // "requires":[{"type":"SCF","scfType":"arcane"}],
        // "excludes":{"name":["Staff","Rod","Wand"]}
        JsonNode orbOfShielding = commonTests.index.getOrigin("magicvariant|orb of shielding (irian quartz)|erlw");
        assertThat(orbOfShielding).isNotNull();

        // "requires":[{"type":"R"},{"property":"T"}],
        // "excludes":{"net":true}
        JsonNode oceanicWeapon = commonTests.index.getOrigin("magicvariant|oceanic weapon|tdcsr");
        assertThat(oceanicWeapon).isNotNull();

        JsonNode x;

        x = commonTests.index.getOrigin("item|arrow|phb");
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(x, arrowSlaying))
                .describedAs("arrowSlaying: Arrow has one required property")
                .isTrue();

        x = commonTests.index.getOrigin("item|crystal|phb");
        assertThat(MagicVariant.INSTANCE.hasExcludedProperty(x, armBlade))
                .describedAs("armBlade: Crystal is not a two-handed weapon (2H)")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasExcludedProperty(x, orbOfShielding))
                .describedAs("orbOfShielding: Crystal does not have excluded name")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasExcludedProperty(x, oceanicWeapon))
                .describedAs("oceanicWeapon: Crystal does not have excluded property (net)")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(x, armBlade))
                .describedAs("armBlade: Crystal is not a melee type (M)")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(x, arrowSlaying))
                .describedAs("arrowSlaying: Crystal does not have either required property")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(x, luckBlade))
                .describedAs("luckBlade: Crystal is not a sword")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(x, orbOfShielding))
                .describedAs("orbOfShielding: Crystal has required property (SCF)")
                .isTrue();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(x, oceanicWeapon))
                .describedAs("oceanicWeapon: Crystal is not the right type (R) and does not have the right property (T)")
                .isFalse();

        x = commonTests.index.getOrigin("item|dagger|phb");
        assertThat(MagicVariant.INSTANCE.hasExcludedProperty(x, armBlade))
                .describedAs("armBlade: Dagger is not a two-handed weapon (2H)")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(x, armBlade))
                .describedAs("armBlade: Dagger is a melee type (M)")
                .isTrue();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(x, luckBlade))
                .describedAs("luckBlade: Dagger is not a sword")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(x, orbOfShielding))
                .describedAs("orbOfShielding: Dagger does not have the required property (SCF / arcane)")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(x, oceanicWeapon))
                .describedAs("oceanicWeapon: Dagger has one of two required properties")
                .isTrue();

        x = commonTests.index.getOrigin("item|greatsword|phb");
        assertThat(MagicVariant.INSTANCE.hasExcludedProperty(x, armBlade))
                .describedAs("armBlade: Greatsword is a two-handed weapon (2H)")
                .isTrue();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(x, luckBlade))
                .describedAs("luckBlade: Greatsword is a sword")
                .isTrue();

        x = commonTests.index.getOrigin("item|net|phb");
        assertThat(MagicVariant.INSTANCE.hasExcludedProperty(x, oceanicWeapon))
                .describedAs("oceanicWeapon: Net property is excluded")
                .isTrue();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(x, oceanicWeapon))
                .describedAs("oceanicWeapon: Net has the right type (R) and the right property (T)")
                .isTrue();

        x = commonTests.index.getOrigin("item|scimitar|phb");
        assertThat(MagicVariant.INSTANCE.hasExcludedProperty(x, armBlade))
                .describedAs("armBlade: Scimitar is not a two-handed weapon (2H)")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(x, armBlade))
                .describedAs("armBlade: Scimitar is a melee type (M)")
                .isTrue();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(x, luckBlade))
                .describedAs("luckBlade: Scimitar is a sword")
                .isTrue();

        x = commonTests.index.getOrigin("item|wand|phb");
        assertThat(MagicVariant.INSTANCE.hasExcludedProperty(x, orbOfShielding))
                .describedAs("orbOfShielding: Wand is an excluded name")
                .isTrue();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(x, orbOfShielding))
                .describedAs("orbOfShielding: Wand has the required property (SCF / arcane)")
                .isTrue();

        x = commonTests.index.getOrigin("item|wooden staff|phb");
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(x, orbOfShielding))
                .describedAs("orbOfShielding: Wooden staff (SCF / druid) does not have all required properties (SCF / arcane)")
                .isFalse();

        x = commonTests.index.getOrigin("item|chain mail|phb");
        assertThat(MagicVariant.INSTANCE.hasExcludedProperty(x, adamantineArmor))
                .describedAs("adamantineArmor: Chain Mail is not excluded")
                .isFalse();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(x, adamantineArmor))
                .describedAs("adamantineArmor: Chain Mail is HA")
                .isTrue();

        x = commonTests.index.getOrigin("item|hide armor|phb");
        assertThat(MagicVariant.INSTANCE.hasExcludedProperty(x, adamantineArmor))
                .describedAs("adamantineArmor: Hide Armor is excluded")
                .isTrue();
        assertThat(MagicVariant.INSTANCE.hasRequiredProperty(x, adamantineArmor))
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
    public void testMonster2024() {
        commonTests.testMonster2024(outputPath);
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
