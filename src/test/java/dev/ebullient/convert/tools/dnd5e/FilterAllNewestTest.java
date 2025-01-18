package dev.ebullient.convert.tools.dnd5e;

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
public class FilterAllNewestTest {

    static CommonDataTests commonTests;
    static final TestInput testInput = TestInput.allNewest;
    static final Path outputPath = TestUtils.OUTPUT_5E_DATA.resolve(testInput.name());

    @BeforeAll
    public static void setupDir() throws Exception {
        outputPath.toFile().mkdirs();
        // This uses test/resources/sources.json to constrain sources
        commonTests = new CommonDataTests(testInput, TestUtils.PATH_5E_TOOLS_DATA);
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

        // All sources, but things that have been reprinted will be replaced by the newest version
        // e.g. PHB elements should be missing/replaced by XPHB equivalents
        if (commonTests.dataPresent) {
            commonTests.assert_MISSING("action|attack|phb");
            commonTests.assert_Present("action|attack|xphb");
            commonTests.assert_MISSING("action|cast a spell|phb");
            commonTests.assert_MISSING("action|disengage|phb");
            commonTests.assert_Present("action|disengage|xphb");
            commonTests.assert_MISSING("background|sage|phb");
            commonTests.assert_Present("background|sage|xphb");
            commonTests.assert_Present("background|baldur's gate acolyte|bgdia");
            commonTests.assert_Present("classtype|artificer|tce");
            commonTests.assert_MISSING("classtype|bard|phb");
            commonTests.assert_Present("classtype|bard|xphb");
            commonTests.assert_MISSING("condition|blinded|phb");
            commonTests.assert_Present("condition|blinded|xphb");
            commonTests.assert_Present("deity|auril|faerûnian|scag");
            commonTests.assert_Present("deity|auril|forgotten realms|phb");
            commonTests.assert_Present("deity|chemosh|dragonlance|dsotdq");
            commonTests.assert_MISSING("deity|chemosh|dragonlance|phb");
            commonTests.assert_Present("deity|the mockery|eberron|erlw");
            commonTests.assert_MISSING("deity|the mockery|eberron|phb");
            commonTests.assert_Present("deity|the traveler|eberron|erlw");
            commonTests.assert_MISSING("deity|the traveler|eberron|phb");
            commonTests.assert_MISSING("deity|the traveler|exandria|egw");
            commonTests.assert_Present("deity|the traveler|exandria|tdcsr");
            commonTests.assert_MISSING("disease|cackle fever|dmg");
            commonTests.assert_Present("disease|cackle fever|xdmg");
            commonTests.assert_MISSING("feat|alert|phb");
            commonTests.assert_Present("feat|alert|xphb");
            commonTests.assert_Present("feat|dueling|xphb");
            commonTests.assert_MISSING("feat|grappler|phb");
            commonTests.assert_Present("feat|grappler|xphb");
            commonTests.assert_MISSING("feat|mobile|phb");
            commonTests.assert_MISSING("feat|moderately armored|phb");
            commonTests.assert_Present("feat|moderately armored|xphb");
            commonTests.assert_Present("hazard|quicksand pit|xdmg");
            commonTests.assert_MISSING("hazard|quicksand|dmg");
            commonTests.assert_MISSING("hazard|razorvine|dmg");
            commonTests.assert_Present("hazard|razorvine|xdmg");
            commonTests.assert_MISSING("itemgroup|arcane focus|phb");
            commonTests.assert_Present("itemgroup|arcane focus|xphb");
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
            commonTests.assert_Present("itemproperty|bf|xdmg");
            commonTests.assert_MISSING("itemtype|$c|phb");
            commonTests.assert_Present("itemtype|$c|xphb");
            commonTests.assert_MISSING("itemtype|$g|dmg");
            commonTests.assert_Present("itemtype|$g|xdmg");
            commonTests.assert_MISSING("item|+1 rod of the pact keeper|dmg");
            commonTests.assert_Present("item|+1 rod of the pact keeper|xdmg");
            commonTests.assert_Present("item|+2 wraps of unarmed power|xdmg");
            commonTests.assert_MISSING("item|+2 wraps of unarmed prowess|bmt");
            commonTests.assert_MISSING("item|acid (vial)|phb");
            commonTests.assert_Present("item|acid absorbing tattoo|tce");
            commonTests.assert_Present("item|acid|xphb");
            commonTests.assert_Present("item|alchemist's doom|scc");
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
            commonTests.assert_Present("item|automatic rifle|xdmg");
            commonTests.assert_MISSING("item|ball bearings (bag of 1,000)|phb");
            commonTests.assert_Present("item|ball bearings|xphb");
            commonTests.assert_Present("item|ball bearing|phb");
            commonTests.assert_MISSING("item|chain (10 feet)|phb");
            commonTests.assert_MISSING("item|chain mail|phb");
            commonTests.assert_Present("item|chain mail|xphb");
            commonTests.assert_Present("item|chain|xphb");
            commonTests.assert_Present("monster|abjurer wizard|mpmm");
            commonTests.assert_MISSING("monster|abjurer|vgm");
            commonTests.assert_Present("monster|alkilith|mpmm");
            commonTests.assert_MISSING("monster|alkilith|mtf");
            commonTests.assert_MISSING("monster|animated object (huge)|phb");
            commonTests.assert_MISSING("monster|ape|mm");
            commonTests.assert_Present("monster|ape|xphb");
            commonTests.assert_MISSING("monster|ash zombie|lmop");
            commonTests.assert_Present("monster|ash zombie|pabtso");
            commonTests.assert_MISSING("monster|awakened shrub|mm");
            commonTests.assert_Present("monster|awakened shrub|xmm");
            commonTests.assert_MISSING("monster|beast of the land|tce");
            commonTests.assert_Present("monster|beast of the land|xphb");
            commonTests.assert_MISSING("monster|bestial spirit (air)|tce");
            commonTests.assert_Present("monster|bestial spirit (air)|xphb");
            commonTests.assert_MISSING("monster|cat|mm");
            commonTests.assert_Present("monster|cat|xphb");
            commonTests.assert_Present("monster|derro savant|mpmm");
            commonTests.assert_MISSING("monster|derro savant|mtf");
            commonTests.assert_MISSING("monster|derro savant|oota");
            commonTests.assert_Present("monster|sibriex|mpmm");
            commonTests.assert_MISSING("monster|sibriex|mtf");
            commonTests.assert_MISSING("object|trebuchet|dmg");
            commonTests.assert_Present("object|trebuchet|xdmg");
            commonTests.assert_MISSING("optfeature|ambush|tce");
            commonTests.assert_Present("optfeature|ambush|xphb");
            commonTests.assert_MISSING("optfeature|investment of the chain master|tce");
            commonTests.assert_Present("optfeature|investment of the chain master|xphb");
            commonTests.assert_MISSING("race|bugbear|erlw");
            commonTests.assert_Present("race|bugbear|mpmm");
            commonTests.assert_MISSING("race|bugbear|vgm");
            commonTests.assert_MISSING("race|human|phb");
            commonTests.assert_Present("race|human|xphb");
            commonTests.assert_MISSING("race|tiefling|phb");
            commonTests.assert_Present("race|tiefling|xphb");
            commonTests.assert_Present("race|warforged|erlw");
            commonTests.assert_MISSING("race|yuan-ti pureblood|vgm");
            commonTests.assert_Present("race|yuan-ti|mpmm");
            commonTests.assert_MISSING("reward|blessing of weapon enhancement|dmg");
            commonTests.assert_Present("reward|blessing of weapon enhancement|xdmg");
            commonTests.assert_MISSING("reward|blessing of wound closure|dmg");
            commonTests.assert_Present("reward|blessing of wound closure|xdmg");
            commonTests.assert_MISSING("reward|boon of combat prowess|dmg");
            commonTests.assert_MISSING("reward|boon of dimensional travel|dmg");
            commonTests.assert_MISSING("reward|boon of fate|dmg");
            commonTests.assert_MISSING("reward|boon of fortitude|dmg");
            commonTests.assert_Present("reward|boon of high magic|dmg");
            commonTests.assert_MISSING("sense|blindsight|phb");
            commonTests.assert_Present("sense|blindsight|xphb");
            commonTests.assert_MISSING("skill|athletics|phb");
            commonTests.assert_Present("skill|athletics|xphb");
            commonTests.assert_MISSING("spell|acid splash|phb");
            commonTests.assert_Present("spell|acid splash|xphb");
            commonTests.assert_Present("spell|aganazzar's scorcher|xge");
            commonTests.assert_MISSING("spell|blade barrier|phb");
            commonTests.assert_Present("spell|blade barrier|xphb");
            commonTests.assert_MISSING("spell|feeblemind|phb");
            commonTests.assert_Present("spell|illusory dragon|xge");
            commonTests.assert_MISSING("spell|illusory script|phb");
            commonTests.assert_Present("spell|illusory script|xphb");
            commonTests.assert_Present("spell|wrath of nature|xge");
            commonTests.assert_MISSING("status|surprised|phb");
            commonTests.assert_Present("status|surprised|xphb");
            commonTests.assert_MISSING("subclass|thief|rogue|phb|phb");
            commonTests.assert_MISSING("subclass|thief|rogue|xphb|phb");
            commonTests.assert_Present("subclass|thief|rogue|xphb|xphb");
            commonTests.assert_MISSING("subrace|genasi (air)|genasi|eepc|eepc");
            commonTests.assert_Present("subrace|genasi (air)|genasi|mpmm|mpmm");
            commonTests.assert_MISSING("subrace|human|human|phb|phb");
            commonTests.assert_Present("subrace|luma (sable)|luma|hwcs|hwcs");
            commonTests.assert_MISSING("subrace|tiefling (zariel)|tiefling|phb|mtf");
            commonTests.assert_MISSING("subrace|tiefling|tiefling|phb|phb");
            commonTests.assert_Present("subrace|vampire (ixalan)|vampire|psz|psx");
            commonTests.assert_MISSING("trap|collapsing roof|dmg");
            commonTests.assert_Present("trap|collapsing roof|xdmg");
            commonTests.assert_MISSING("trap|falling net|dmg");
            commonTests.assert_Present("trap|falling net|xdmg");
            commonTests.assert_MISSING("trap|pits|dmg");
            commonTests.assert_MISSING("trap|poison darts|dmg");
            commonTests.assert_Present("trap|poison needle trap|xge");
            commonTests.assert_MISSING("trap|poison needle|dmg");
            commonTests.assert_Present("trap|poisoned darts|xdmg");
            commonTests.assert_MISSING("trap|rolling sphere|dmg");
            commonTests.assert_Present("trap|rolling stone|xdmg");
            commonTests.assert_Present("variantrule|facing|dmg");
            commonTests.assert_MISSING("variantrule|falling|xge");
            commonTests.assert_Present("variantrule|familiars|mm");
            commonTests.assert_MISSING("variantrule|simultaneous effects|xge");
            commonTests.assert_Present("variantrule|simultaneous effects|xphb");
            commonTests.assert_MISSING("vehicle|apparatus of kwalish|dmg");
            commonTests.assert_Present("vehicle|apparatus of kwalish|xdmg");
        }
    }
}
