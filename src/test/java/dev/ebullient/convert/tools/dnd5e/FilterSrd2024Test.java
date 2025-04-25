package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.ebullient.convert.TestUtils;
import dev.ebullient.convert.tools.dnd5e.CommonDataTests.TestInput;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FilterSrd2024Test {

    static CommonDataTests commonTests;
    static final TestInput testInput = TestInput.srd2024;
    static final Path outputPath = TestUtils.OUTPUT_5E_DATA.resolve(testInput.name());

    @BeforeAll
    public static void setupDir() throws Exception {
        outputPath.toFile().mkdirs();
        String config = """
                {
                    "sources": {
                        "reference": ["srd52", "basicRules2024"]
                    },
                    "images": {
                        "copyInternal": false
                    }
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

        if (commonTests.dataPresent) {
            var config = commonTests.config;

            assertThat(config.sourceIncluded("srd")).isFalse();
            assertThat(config.sourceIncluded("basicrules")).isFalse();
            assertThat(config.sourceIncluded("srd52")).isTrue();
            assertThat(config.sourceIncluded("basicRules2024")).isTrue();

            assertThat(config.sourceIncluded("DMG")).isFalse();
            assertThat(config.sourceIncluded("PHB")).isFalse();

            assertThat(config.sourceIncluded("XDMG")).isFalse();
            assertThat(config.sourceIncluded("XPHB")).isFalse();

            commonTests.assert_MISSING("action|attack|phb");
            commonTests.assert_Present("action|attack|xphb");
            commonTests.assert_MISSING("action|cast a spell|phb");
            commonTests.assert_MISSING("action|disengage|phb");
            commonTests.assert_Present("action|disengage|xphb");

            commonTests.assert_MISSING("feat|alert|phb");
            commonTests.assert_Present("feat|alert|xphb");
            commonTests.assert_Present("feat|dueling|xphb");
            commonTests.assert_MISSING("feat|grappler|phb");
            commonTests.assert_Present("feat|grappler|xphb");
            commonTests.assert_MISSING("feat|mobile|phb");
            commonTests.assert_MISSING("feat|moderately armored|phb");
            commonTests.assert_MISSING("feat|moderately armored|xphb");

            commonTests.assert_MISSING("variantrule|facing|dmg");
            commonTests.assert_MISSING("variantrule|falling|xge");
            commonTests.assert_MISSING("variantrule|familiars|mm");
            commonTests.assert_MISSING("variantrule|simultaneous effects|xge");
            commonTests.assert_Present("variantrule|simultaneous effects|xphb");

            commonTests.assert_MISSING("background|sage|phb");
            commonTests.assert_Present("background|sage|xphb");
            commonTests.assert_MISSING("background|baldur's gate acolyte|bgdia");

            commonTests.assert_MISSING("condition|blinded|phb");
            commonTests.assert_Present("condition|blinded|xphb");

            commonTests.assert_MISSING("deity|auril|faerûnian|scag");
            commonTests.assert_MISSING("deity|auril|forgotten realms|phb");
            commonTests.assert_MISSING("deity|chemosh|dragonlance|dsotdq");
            commonTests.assert_MISSING("deity|chemosh|dragonlance|phb");
            commonTests.assert_MISSING("deity|ehlonna|greyhawk|phb");
            commonTests.assert_MISSING("deity|ehlonna|greyhawk|xdmg");
            commonTests.assert_MISSING("deity|gruumsh|dawn war|dmg");
            commonTests.assert_MISSING("deity|gruumsh|exandria|egw");
            commonTests.assert_MISSING("deity|gruumsh|nonhuman|phb");
            commonTests.assert_MISSING("deity|gruumsh|orc|scag");
            commonTests.assert_MISSING("deity|gruumsh|orc|vgm");
            commonTests.assert_MISSING("deity|the traveler|eberron|erlw");
            commonTests.assert_MISSING("deity|the traveler|eberron|phb");
            commonTests.assert_MISSING("deity|the traveler|exandria|egw");
            commonTests.assert_MISSING("deity|the traveler|exandria|tdcsr");

            commonTests.assert_MISSING("disease|cackle fever|dmg");
            commonTests.assert_Present("disease|cackle fever|xdmg");

            commonTests.assert_MISSING("hazard|quicksand pit|xdmg");
            commonTests.assert_MISSING("hazard|quicksand|dmg");
            commonTests.assert_MISSING("hazard|razorvine|dmg");
            commonTests.assert_MISSING("hazard|razorvine|xdmg");

            commonTests.assert_MISSING("itemgroup|arcane focus|phb");
            commonTests.assert_MISSING("itemgroup|arcane focus|xphb");
            commonTests.assert_MISSING("itemgroup|carpet of flying|dmg");
            commonTests.assert_Present("itemgroup|carpet of flying|xdmg");
            commonTests.assert_MISSING("itemgroup|ioun stone|dmg");
            commonTests.assert_MISSING("itemgroup|ioun stone|llk");
            commonTests.assert_Present("itemgroup|ioun stone|xdmg");
            commonTests.assert_MISSING("itemgroup|musical instrument|phb");
            commonTests.assert_MISSING("itemgroup|musical instrument|scag");
            commonTests.assert_Present("itemgroup|musical instrument|xphb");
            commonTests.assert_MISSING("itemgroup|spell scroll|dmg");
            commonTests.assert_Present("itemgroup|spell scroll|xdmg");

            commonTests.assert_MISSING("itemproperty|2h|phb");
            commonTests.assert_Present("itemproperty|2h|xphb");
            commonTests.assert_MISSING("itemproperty|bf|dmg");
            commonTests.assert_MISSING("itemproperty|bf|xdmg");
            commonTests.assert_MISSING("itemproperty|er|tdcsr");
            commonTests.assert_MISSING("itemproperty|s|phb");

            commonTests.assert_MISSING("itemtype|$c|phb");
            commonTests.assert_Present("itemtype|$c|xphb");
            commonTests.assert_MISSING("itemtype|$g|dmg");
            commonTests.assert_MISSING("itemtype|$g|xdmg");
            commonTests.assert_MISSING("itemtype|sc|dmg");
            commonTests.assert_Present("itemtype|sc|xphb");
            commonTests.assert_MISSING("itemtypeadditionalentries|gs|phb|xge");

            commonTests.assert_MISSING("item|+1 rod of the pact keeper|dmg");
            commonTests.assert_MISSING("item|+1 rod of the pact keeper|xdmg");
            commonTests.assert_MISSING("item|+2 wraps of unarmed power|xdmg");
            commonTests.assert_MISSING("item|+2 wraps of unarmed prowess|bmt");
            commonTests.assert_MISSING("item|acid (vial)|phb");
            commonTests.assert_MISSING("item|acid absorbing tattoo|tce");
            commonTests.assert_Present("item|acid|xphb");
            commonTests.assert_MISSING("item|alchemist's doom|scc");
            commonTests.assert_MISSING("item|alchemist's fire (flask)|phb");
            commonTests.assert_Present("item|alchemist's fire|xphb");
            commonTests.assert_MISSING("item|alchemist's supplies|phb");
            commonTests.assert_Present("item|alchemist's supplies|xphb");
            commonTests.assert_MISSING("item|amulet of health|dmg");
            commonTests.assert_Present("item|amulet of health|xdmg");
            commonTests.assert_MISSING("item|amulet of proof against detection and location|dmg");
            commonTests.assert_Present("item|amulet of proof against detection and location|xdmg");
            commonTests.assert_MISSING("item|armor of invulnerability|dmg");
            commonTests.assert_Present("item|armor of invulnerability|xdmg");
            commonTests.assert_MISSING("item|automatic pistol|dmg");
            commonTests.assert_MISSING("item|automatic rifle|dmg");
            commonTests.assert_MISSING("item|automatic rifle|xdmg");
            commonTests.assert_MISSING("item|ball bearings (bag of 1,000)|phb");
            commonTests.assert_Present("item|ball bearings|xphb");
            commonTests.assert_MISSING("item|ball bearing|phb");
            commonTests.assert_MISSING("item|chain (10 feet)|phb");
            commonTests.assert_Present("item|chain|xphb");

            commonTests.assert_MISSING("monster|abjurer wizard|mpmm");
            commonTests.assert_MISSING("monster|abjurer|vgm");
            commonTests.assert_MISSING("monster|alkilith|mpmm");
            commonTests.assert_MISSING("monster|alkilith|mtf");
            commonTests.assert_MISSING("monster|animated object (huge)|phb");
            commonTests.assert_MISSING("monster|ape|mm");
            commonTests.assert_Present("monster|ape|xmm");
            commonTests.assert_MISSING("monster|ash zombie|lmop");
            commonTests.assert_MISSING("monster|ash zombie|pabtso");
            commonTests.assert_MISSING("monster|awakened shrub|mm");
            commonTests.assert_Present("monster|awakened shrub|xmm");
            commonTests.assert_MISSING("monster|beast of the land|tce");
            commonTests.assert_MISSING("monster|beast of the land|xphb");
            commonTests.assert_MISSING("monster|bestial spirit (air)|tce");
            commonTests.assert_MISSING("monster|bestial spirit (air)|xphb");
            commonTests.assert_MISSING("monster|cat|mm");
            commonTests.assert_Present("monster|cat|xmm");
            commonTests.assert_MISSING("monster|derro savant|mpmm");
            commonTests.assert_MISSING("monster|derro savant|mtf");
            commonTests.assert_MISSING("monster|derro savant|oota");
            commonTests.assert_MISSING("monster|sibriex|mpmm");
            commonTests.assert_MISSING("monster|sibriex|mtf");

            commonTests.assert_MISSING("object|trebuchet|dmg");
            commonTests.assert_MISSING("object|trebuchet|xdmg");

            commonTests.assert_MISSING("optfeature|ambush|tce");
            commonTests.assert_MISSING("optfeature|ambush|xphb");
            commonTests.assert_MISSING("optfeature|investment of the chain master|tce");
            commonTests.assert_Present("optfeature|investment of the chain master|xphb");

            commonTests.assert_MISSING("reward|blessing of weapon enhancement|dmg");
            commonTests.assert_MISSING("reward|blessing of weapon enhancement|xdmg");
            commonTests.assert_MISSING("reward|blessing of wound closure|dmg");
            commonTests.assert_MISSING("reward|blessing of wound closure|xdmg");
            commonTests.assert_MISSING("reward|boon of combat prowess|dmg");
            commonTests.assert_MISSING("reward|boon of dimensional travel|dmg");
            commonTests.assert_MISSING("reward|boon of fate|dmg");
            commonTests.assert_MISSING("reward|boon of fortitude|dmg");
            commonTests.assert_MISSING("reward|boon of high magic|dmg");

            commonTests.assert_MISSING("sense|blindsight|phb");
            commonTests.assert_Present("sense|blindsight|xphb");

            commonTests.assert_MISSING("skill|athletics|phb");
            commonTests.assert_Present("skill|athletics|xphb");

            commonTests.assert_MISSING("spell|acid splash|phb");
            commonTests.assert_Present("spell|acid splash|xphb");
            commonTests.assert_MISSING("spell|aganazzar's scorcher|xge");
            commonTests.assert_MISSING("spell|blade barrier|phb");
            commonTests.assert_Present("spell|blade barrier|xphb");
            commonTests.assert_MISSING("spell|feeblemind|phb");
            commonTests.assert_MISSING("spell|illusory dragon|xge");
            commonTests.assert_MISSING("spell|illusory script|phb");
            commonTests.assert_Present("spell|illusory script|xphb");
            commonTests.assert_MISSING("spell|wrath of nature|xge");

            commonTests.assert_MISSING("status|surprised|phb");
            commonTests.assert_Present("status|surprised|xphb");

            commonTests.assert_MISSING("trap|collapsing roof|dmg");
            commonTests.assert_Present("trap|collapsing roof|xdmg"); // basicRules2024
            commonTests.assert_MISSING("trap|falling net|dmg");
            commonTests.assert_Present("trap|falling net|xdmg"); // basicRules2024
            commonTests.assert_MISSING("trap|pits|dmg");
            commonTests.assert_Present("trap|hidden pit|xdmg"); // basicRules2024
            commonTests.assert_MISSING("trap|poison darts|dmg");
            commonTests.assert_Present("trap|poisoned darts|xdmg"); // basicRules2024
            commonTests.assert_MISSING("trap|poison needle trap|xge");
            commonTests.assert_MISSING("trap|poison needle|dmg");
            commonTests.assert_Present("trap|poisoned darts|xdmg"); // basicRules2024
            commonTests.assert_MISSING("trap|rolling sphere|dmg");
            commonTests.assert_Present("trap|rolling stone|xdmg"); // basicRules2024

            commonTests.assert_MISSING("vehicle|apparatus of kwalish|dmg");
            commonTests.assert_Present("vehicle|apparatus of kwalish|xdmg");

            // Classes, subclasses, class features, and subclass features

            commonTests.assert_MISSING("classtype|artificer|tce");

            // "Path of Wild Magic|Barbarian||Wild Magic|TCE|3",
            // "Bolstering Magic|Barbarian||Wild Magic|TCE|6",
            // "Unstable Backlash|Barbarian||Wild Magic|TCE|10",
            // "Controlled Surge|Barbarian||Wild Magic|TCE|14",

            commonTests.assert_MISSING("classtype|barbarian|phb");
            commonTests.assert_Present("classtype|barbarian|xphb");

            commonTests.assert_MISSING("subclass|path of wild magic|barbarian|phb|tce");
            commonTests.assert_MISSING("subclass|path of wild magic|barbarian|xphb|tce");

            commonTests.assert_MISSING("subclassfeature|bolstering magic|barbarian|phb|wild magic|tce|6|tce");
            commonTests.assert_MISSING("subclassfeature|controlled surge|barbarian|phb|wild magic|tce|14|tce");
            commonTests.assert_MISSING("subclassfeature|magic awareness|barbarian|phb|wild magic|tce|3|tce");
            commonTests.assert_MISSING("subclassfeature|path of wild magic|barbarian|phb|wild magic|tce|3|tce");
            commonTests.assert_MISSING("subclassfeature|unstable backlash|barbarian|phb|wild magic|tce|10|tce");
            commonTests.assert_MISSING("subclassfeature|wild surge|barbarian|phb|wild magic|tce|3|tce");

            // "Thief|Rogue||Thief||3",
            // "Supreme Sneak|Rogue||Thief||9",
            // "Use Magic Device|Rogue||Thief||13",
            // "Thief's Reflexes|Rogue||Thief||17"

            commonTests.assert_MISSING("classtype|rogue|phb");
            commonTests.assert_Present("classtype|rogue|xphb");

            commonTests.assert_MISSING("subclass|thief|rogue|phb|phb");
            commonTests.assert_MISSING("subclass|thief|rogue|xphb|phb");
            commonTests.assert_Present("subclass|thief|rogue|xphb|xphb");

            commonTests.assert_MISSING("subclassfeature|thief|rogue|phb|thief|phb|3|phb");
            commonTests.assert_Present("subclassfeature|thief|rogue|xphb|thief|xphb|3|xphb");
            commonTests.assert_MISSING("subclassfeature|supreme sneak|rogue|phb|thief|phb|9|phb");
            commonTests.assert_Present("subclassfeature|supreme sneak|rogue|xphb|thief|xphb|9|xphb");
            commonTests.assert_MISSING("subclassfeature|use magic device|rogue|phb|thief|phb|13|phb");
            commonTests.assert_Present("subclassfeature|use magic device|rogue|xphb|thief|xphb|13|xphb");
            commonTests.assert_MISSING("subclassfeature|thief's reflexes|rogue|phb|thief|phb|17|phb");
            commonTests.assert_Present("subclassfeature|thief's reflexes|rogue|xphb|thief|xphb|17|xphb");

            // Races and subraces

            commonTests.assert_MISSING("race|bugbear|erlw");
            commonTests.assert_MISSING("race|bugbear|mpmm");
            commonTests.assert_MISSING("race|bugbear|vgm");
            commonTests.assert_MISSING("race|human|phb");
            commonTests.assert_Present("race|human|xphb");
            commonTests.assert_MISSING("race|tiefling|phb");
            commonTests.assert_Present("race|tiefling|xphb");
            commonTests.assert_MISSING("race|warforged|erlw");
            commonTests.assert_MISSING("race|yuan-ti pureblood|vgm");
            commonTests.assert_MISSING("race|yuan-ti|mpmm");

            commonTests.assert_MISSING("subrace|genasi (air)|genasi|eepc|eepc");
            commonTests.assert_MISSING("subrace|genasi (air)|genasi|mpmm|mpmm");
            commonTests.assert_MISSING("subrace|human|human|phb|phb");
            commonTests.assert_MISSING("subrace|tiefling (zariel)|tiefling|phb|mtf");
            commonTests.assert_MISSING("subrace|tiefling|tiefling|phb|phb");
            commonTests.assert_MISSING("subrace|vampire (ixalan)|vampire|psz|psx");
        }
    }
}
