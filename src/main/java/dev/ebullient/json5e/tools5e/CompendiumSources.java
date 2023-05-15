package dev.ebullient.json5e.tools5e;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.json5e.io.Json5eTui;

public class CompendiumSources {
    final IndexType type;
    final String key;
    final String name;
    final Set<String> bookSources = new LinkedHashSet<>();
    final String sourceText;

    public CompendiumSources(IndexType type, String key, JsonNode jsonElement) {
        this.type = type;
        this.key = key;
        this.name = (jsonElement.has("name")
                ? jsonElement.get("name").asText()
                : jsonElement.get("abbreviation").asText()).trim();
        this.sourceText = findSourceText(jsonElement);
    }

    public String getSourceText(boolean useSrd) {
        if (useSrd) {
            return "SRD / Basic Rules";
        }
        return sourceText;
    }

    public List<String> getSourceTags() {
        return List.of("compendium/src/" + primarySource().toLowerCase());
    }

    private String findSourceText(JsonNode jsonElement) {
        this.bookSources.add(jsonElement.get("source").asText());

        List<String> srcText = new ArrayList<>();
        srcText.add(sourceAndPage(jsonElement));

        String copyOf = jsonElement.has("_copy")
                ? jsonElement.get("_copy").get("name").asText()
                : null;
        String copySrc = jsonElement.has("_copy")
                ? jsonElement.get("_copy").get("source").asText()
                : null;

        if (copyOf != null) {
            srcText.add(String.format("Derived from %s (%s)", copyOf, copySrc));
        }

        if (jsonElement.has("additionalSources")) {
            srcText.addAll(StreamSupport.stream(jsonElement.withArray("additionalSources").spliterator(), false)
                    .filter(x -> !x.get("source").asText().equals(copySrc))
                    .peek(x -> this.bookSources.add(x.get("source").asText()))
                    .map(this::sourceAndPage)
                    .collect(Collectors.toList()));
        }

        if (jsonElement.has("otherSources")) {
            srcText.addAll(StreamSupport.stream(jsonElement.withArray("otherSources").spliterator(), false)
                    .filter(x -> !x.get("source").asText().equals(copySrc))
                    .peek(x -> this.bookSources.add(x.get("source").asText()))
                    .map(this::sourceAndPage)
                    .collect(Collectors.toList()));
        }

        return String.join(", ", srcText);
    }

    private String sourceAndPage(JsonNode source) {
        String src = source.get("source").asText();
        String book = abvToName.getOrDefault(src, src);
        if (source.has("page")) {
            return String.format("%s p. %s", book, source.get("page").asText());
        }
        return book;
    }

    public boolean isPrimarySource(String source) {
        return bookSources.iterator().next().equals(source);
    }

    public String primarySource() {
        return bookSources.iterator().next();
    }

    public Optional<String> uaSource() {
        Optional<String> source = bookSources.stream().filter(x -> x.contains("UA") && !x.equals("UAWGE")).findFirst();
        return source.map(s -> sourceToAbbreviation(s));
    }

    public String mapPrimarySource() {
        String primary = primarySource();
        return sourceToAbv.getOrDefault(primary, primary);
    }

    public String alternateSource() {
        Iterator<String> i = bookSources.iterator();
        if (bookSources.size() > 1) {
            i.next();
        }
        return i.next();
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public IndexType getType() {
        return type;
    }

    public static String sourceToLongName(String src) {
        return abvToName.getOrDefault(sourceToAbbreviation(src), src);
    }

    public static String sourceToAbbreviation(String src) {
        return sourceToAbv.getOrDefault(src, src);
    }

    @Override
    public String toString() {
        return "sources[" + key + ']';
    }

    final static String AL_PREFIX = "Adventurers League: ";
    final static String AL_PREFIX_SHORT = "AL: ";
    final static String PS_PREFIX = "Plane Shift: ";
    final static String PS_PREFIX_SHORT = "PS: ";
    final static String UA_PREFIX = "Unearthed Arcana: ";
    final static String UA_PREFIX_SHORT = "UA: ";
    final static String TftYP_NAME = "Tales from the Yawning Portal";
    final static String AitFR_NAME = "Adventures in the Forgotten Realms";
    final static String NRH_NAME = "NERDS Restoring Harmony";
    final static String MCVX_PREFIX = "Monster Compendium Volume ";

    final static Map<String, String> abvToName = new HashMap<>();
    final static Map<String, String> sourceToAbv = new HashMap<>();

    static {
        abvToName.put("AAG", "Astral Adventurer's Guide");
        abvToName.put("AI", "Acquisitions Incorporated");
        abvToName.put("AitFR-AVT", AitFR_NAME + ": A Verdant Tomb");
        abvToName.put("AitFR-DN", AitFR_NAME + ": Deepest Night");
        abvToName.put("AitFR-FCD", AitFR_NAME + ": From Cyan Depths");
        abvToName.put("AitFR-ISF", AitFR_NAME + ": In Scarlet Flames");
        abvToName.put("AitFR-THP", AitFR_NAME + ": The Hidden Page");
        abvToName.put("AitFR", AitFR_NAME);
        abvToName.put("AL", "Adventurers' League");
        abvToName.put("ALCoS", AL_PREFIX + "Curse of Strahd");
        abvToName.put("ALEE", AL_PREFIX + "Elemental Evil");
        abvToName.put("ALRoD", AL_PREFIX + "Rage of Demons");
        abvToName.put("AWM", "Adventure with Muk");
        abvToName.put("AZfyT", "A Zib for your Thoughts");
        abvToName.put("BAM", "Boo's Astral Menagerie");
        abvToName.put("BGDIA", "Baldur's Gate: Descent Into Avernus");
        abvToName.put("CM", "Candlekeep Mysteries");
        abvToName.put("CoS", "Curse of Strahd");
        abvToName.put("CRCotN", "Critical Role: Call of the Netherdeep");
        abvToName.put("DC", "Divine Contention");
        abvToName.put("DIP", "Dragon of Icespire Peak");
        abvToName.put("DMG", "Dungeon Master's Guide");
        abvToName.put("DoD", "Domains of Delight");
        abvToName.put("DoSI", "Dragons of Stormwreck Isle");
        abvToName.put("EEPC", "Elemental Evil Player's Companion");
        abvToName.put("EET", "Elemental Evil: Trinkets");
        abvToName.put("EFR", "Eberron: Forgotten Relics");
        abvToName.put("DD", "Dangerous Designs");
        abvToName.put("FS", "Frozen Sick");
        abvToName.put("ToR", "Tide of Retribution");
        abvToName.put("US", "Unwelcome Spirits");
        abvToName.put("EGW_DD", "Dangerous Designs");
        abvToName.put("EGW_FS", "Frozen Sick");
        abvToName.put("EGW_ToR", "Tide of Retribution");
        abvToName.put("EGW_US", "Unwelcome Spirits");
        abvToName.put("EGW", "Explorer's Guide to Wildemount");
        abvToName.put("ERLW", "Eberron: Rising from the Last War");
        abvToName.put("ESK", "Essentials Kit");
        abvToName.put("FTD", "Fizban's Treasury of Dragons");
        abvToName.put("GGR", "Guildmasters' Guide to Ravnica");
        abvToName.put("GoS", "Ghosts of Saltmarsh");
        abvToName.put("HEROES_FEAST", "Heroes' Feast");
        abvToName.put("HftT", "Hunt for the Thessalhydra");
        abvToName.put("HoL", "The House of Lament");
        abvToName.put("HotDQ", "Hoard of the Dragon Queen");
        abvToName.put("IDRotF", "Icewind Dale: Rime of the Frostmaiden");
        abvToName.put("IMR", "Infernal Machine Rebuild");
        abvToName.put("JttRC", "Journeys through the Radiant Citadel");
        abvToName.put("KKW", "Krenko's Way");
        abvToName.put("LLK", "Lost Laboratory of Kwalish");
        abvToName.put("LMoP", "Lost Mine of Phandelver");
        abvToName.put("LoX", "Light of Xaryxis");
        abvToName.put("LR", "Locathah Rising");
        abvToName.put("MaBJoV", "Minsc and Boo's Journal of Villainy");
        abvToName.put("MCV1SC", MCVX_PREFIX + "1: Spelljammer Creatures");
        abvToName.put("MFF", "Mordenkainen's Fiendish Folio");
        abvToName.put("MGELFT", "Muk's Guide To Everything He Learned From Tasha");
        abvToName.put("MM", "Monster Manual");
        abvToName.put("MOT", "Mythic Odysseys of Theros");
        abvToName.put("MPMM", "Mordenkainen Presents: Monsters of the Multiverse");
        abvToName.put("MTF", "Mordenkainen's Tome of Foes");
        abvToName.put("NRH-ASS", NRH_NAME + ": A Sticky Situation");
        abvToName.put("NRH-AT", NRH_NAME + ": Adventure Together");
        abvToName.put("NRH-AVitW", NRH_NAME + ": A Voice in the Wilderness");
        abvToName.put("NRH-AWoL", NRH_NAME + ": A Web of Lies");
        abvToName.put("NRH-CoI", NRH_NAME + ": Circus of Illusions");
        abvToName.put("NRH-TCMC", NRH_NAME + ": The Candy Mountain Caper");
        abvToName.put("NRH-TLT", NRH_NAME + ": The Lost Tomb");
        abvToName.put("NRH", NRH_NAME);
        abvToName.put("OGA", "One Grung Above");
        abvToName.put("OotA", "Out of the Abyss");
        abvToName.put("OoW", "The Orrery of the Wanderer");
        abvToName.put("PHB", "Player's Handbook");
        abvToName.put("PotA", "Princes of the Apocalypse");
        abvToName.put("PSA", PS_PREFIX + "Amonkhet");
        abvToName.put("PSD", PS_PREFIX + "Dominaria");
        abvToName.put("PSI", PS_PREFIX + "Innistrad");
        abvToName.put("PSK", PS_PREFIX + "Kaladesh");
        abvToName.put("PSX", PS_PREFIX + "Ixalan");
        abvToName.put("PSZ", PS_PREFIX + "Zendikar");
        abvToName.put("RMBRE", "The Lost Dungeon of Rickedness: Big Rick Energy");
        abvToName.put("RMR", "Dungeons & Dragons vs. Rick and Morty: Basic Rules");
        abvToName.put("RoT", "The Rise of Tiamat");
        abvToName.put("RoTOS", "The Rise of Tiamat Online Supplement");
        abvToName.put("RtG", "Return to Glory");
        abvToName.put("SAC", "Sage Advice Compendium");
        abvToName.put("SADS", "Sapphire Anniversary Dice Set");
        abvToName.put("SAiS", "Spelljammer: Adventures in Space");
        abvToName.put("SCAG", "Sword Coast Adventurer's Guide");
        abvToName.put("SCC-ARiR", "A Reckoning in Ruins");
        abvToName.put("SCC-CK", "Campus Kerfuffle");
        abvToName.put("SCC-HfMT", "Hunt for Mage Tower");
        abvToName.put("SCC-TMM", "The Magister's Masquerade");
        abvToName.put("SCC_ARiR", "A Reckoning in Ruins");
        abvToName.put("SCC_CK", "Campus Kerfuffle");
        abvToName.put("SCC_HfMT", "Hunt for Mage Tower");
        abvToName.put("SCC_TMM", "The Magister's Masquerade");
        abvToName.put("SCC", "Strixhaven: A Curriculum of Chaos");
        abvToName.put("SCREEN_DUNGEON_KIT", "Dungeon Master's Screen: Dungeon Kit");
        abvToName.put("SCREEN_WILDERNESS_KIT", "Dungeon Master's Screen: Wilderness Kit");
        abvToName.put("SCREEN", "Dungeon Master's Screen");
        abvToName.put("SDW", "Sleeping Dragon's Wake");
        abvToName.put("SjA", "Spelljammer Academy");
        abvToName.put("SJA", "Spelljammer Academy");
        abvToName.put("SKT", "Storm King's Thunder");
        abvToName.put("SLW", "Storm Lord's Wrath");
        abvToName.put("TCE", "Tasha's Cauldron of Everything");
        abvToName.put("TLK", "The Lost Kenku");
        abvToName.put("ToA", "Tomb of Annihilation");
        abvToName.put("ToD", "Tyranny of Dragons");
        abvToName.put("TTP", "The Tortle Package");
        abvToName.put("TftYP", TftYP_NAME);
        abvToName.put("TftYP-AtG", TftYP_NAME + ": Against the Giants");
        abvToName.put("TftYP-DiT", TftYP_NAME + ": Dead in Thay");
        abvToName.put("TftYP-TFoF", TftYP_NAME + ": The Forge of Fury");
        abvToName.put("TftYP-THSoT", TftYP_NAME + ": The Hidden Shrine of Tamoachan");
        abvToName.put("TftYP-ToH", TftYP_NAME + ": Tomb of Horrors");
        abvToName.put("TftYP-TSC", TftYP_NAME + ": The Sunless Citadel");
        abvToName.put("TftYP-WPM", TftYP_NAME + ": White Plume Mountain");
        abvToName.put("TYP", TftYP_NAME);
        abvToName.put("TYP_AtG", TftYP_NAME + ": Against the Giants");
        abvToName.put("TYP_DiT", TftYP_NAME + ": Dead in Thay");
        abvToName.put("TYP_TFoF", TftYP_NAME + ": The Forge of Fury");
        abvToName.put("TYP_THSoT", TftYP_NAME + ": The Hidden Shrine of Tamoachan");
        abvToName.put("TYP_ToH", TftYP_NAME + ": Tomb of Horrors");
        abvToName.put("TYP_TSC", TftYP_NAME + ": The Sunless Citadel");
        abvToName.put("TYP_WPM", TftYP_NAME + ": White Plume Mountain");
        abvToName.put("UA20F", UA_PREFIX + "2020 Feats");
        abvToName.put("UA20POR", UA_PREFIX + "2020 Psionic Options Revisited");
        abvToName.put("UA20SC1", UA_PREFIX + "2020 Subclasses, Part 1");
        abvToName.put("UA20SC2", UA_PREFIX + "2020 Subclasses, Part 2");
        abvToName.put("UA20SC3", UA_PREFIX + "2020 Subclasses, Part 3");
        abvToName.put("UA20SC4", UA_PREFIX + "2020 Subclasses, Part 4");
        abvToName.put("UA20SC5", UA_PREFIX + "2020 Subclasses, Part 5");
        abvToName.put("UA20SMT", UA_PREFIX + "2020 Spells and Magic Tattoos");
        abvToName.put("UA20SCR", UA_PREFIX + "2020 Subclasses Revisited");
        abvToName.put("UA21DO", UA_PREFIX + "2021 Draconic Options");
        abvToName.put("UA21FF", UA_PREFIX + "2021 Folk of the Feywild");
        abvToName.put("UA21GL", UA_PREFIX + "2021 Gothic Lineages");
        abvToName.put("UA21MoS", UA_PREFIX + "2021 Mages of Strixhaven");
        abvToName.put("UA21TotM", UA_PREFIX + "2021 Travelers of the Multiverse");
        abvToName.put("UA22GO", UA_PREFIX + "2022 Giant Options");
        abvToName.put("UA22HoK", UA_PREFIX + "2022 Heroes of Krynn");
        abvToName.put("UA22HoKR", UA_PREFIX + "2022 Heroes of Krynn Revisited");
        abvToName.put("UA22WotM", UA_PREFIX + "2022 Wonders of the Multivers");
        abvToName.put("UA3PE", UA_PREFIX + "Three-Pillar Experience");
        abvToName.put("UAA", UA_PREFIX + "Artificer");
        abvToName.put("UAAR", UA_PREFIX + "Artificer Revisited");
        abvToName.put("UAATOSC", UA_PREFIX + "A Trio of Subclasses");
        abvToName.put("UABAM", UA_PREFIX + "Barbarian and Monk");
        abvToName.put("UABAP", UA_PREFIX + "Bard and Paladin");
        abvToName.put("UABBC", UA_PREFIX + "Bard: Bard Colleges");
        abvToName.put("UABPP", UA_PREFIX + "Barbarian Primal Paths");
        abvToName.put("UACAM", UA_PREFIX + "Centaurs and Minotaurs");
        abvToName.put("UACDD", UA_PREFIX + "Cleric: Divine Domains");
        abvToName.put("UACDW", UA_PREFIX + "Cleric, Druid, and Wizard");
        abvToName.put("UACFV", UA_PREFIX + "Class Feature Variants");
        abvToName.put("UAD", UA_PREFIX + "Druid");
        abvToName.put("UAEAG", UA_PREFIX + "Eladrin and Gith");
        abvToName.put("UAEBB", UA_PREFIX + "Eberron");
        abvToName.put("UAESR", UA_PREFIX + "Elf Subraces");
        abvToName.put("UAF", UA_PREFIX + "Fighter");
        abvToName.put("UAFFR", UA_PREFIX + "Feats for Races");
        abvToName.put("UAFFS", UA_PREFIX + "Feats for Skills");
        abvToName.put("UAFO", UA_PREFIX + "Fiendish Options");
        abvToName.put("UAFRR", UA_PREFIX + "Fighter, Ranger, and Rogue");
        abvToName.put("UAFRW", UA_PREFIX + "Fighter, Rogue, and Wizard");
        abvToName.put("UAFT", UA_PREFIX + "Feats");
        abvToName.put("UAGH", UA_PREFIX + "Gothic Heroes");
        abvToName.put("UAGHI", UA_PREFIX + "Greyhawk Initiative");
        abvToName.put("UAGSS", UA_PREFIX + "Giant Soul Sorcerer");
        abvToName.put("UAKOO", UA_PREFIX + "Kits of Old");
        abvToName.put("UALDR", UA_PREFIX + "Light, Dark, Underdark!");
        abvToName.put("UAM", UA_PREFIX + "Monk");
        abvToName.put("UAMAC", UA_PREFIX + "Mass Combat");
        abvToName.put("UAMC", UA_PREFIX + "Modifying Classes");
        abvToName.put("UAMDM", UA_PREFIX + "Modern Magic");
        abvToName.put("UAOD", UA_PREFIX + "Order Domain");
        abvToName.put("UAOSS", UA_PREFIX + "Of Ships and the Sea");
        abvToName.put("UAP", UA_PREFIX + "Paladin");
        abvToName.put("UAPCRM", UA_PREFIX + "Prestige Classes and Rune Magic");
        abvToName.put("UAR", UA_PREFIX + "Ranger");
        abvToName.put("UARAR", UA_PREFIX + "Ranger and Rogue");
        abvToName.put("UARCO", UA_PREFIX + "Revised Class Options");
        abvToName.put("UARoE", UA_PREFIX + "Races of Eberron");
        abvToName.put("UARoR", UA_PREFIX + "Races of Ravnica");
        abvToName.put("UARSC", UA_PREFIX + "Revised Subclasses");
        abvToName.put("UAS", UA_PREFIX + "Sorcerer");
        abvToName.put("UASAW", UA_PREFIX + "Sorcerer and Warlock");
        abvToName.put("UASIK", UA_PREFIX + "Sidekicks");
        abvToName.put("UASSP", UA_PREFIX + "Starter Spells");
        abvToName.put("UATF", UA_PREFIX + "The Faithful");
        abvToName.put("UATMC", UA_PREFIX + "The Mystic Class");
        abvToName.put("UATOBM", UA_PREFIX + "That Old Black Magic");
        abvToName.put("UATRR", UA_PREFIX + "The Ranger, Revised");
        abvToName.put("UATSC", UA_PREFIX + "Three Subclasses");
        abvToName.put("UAVR", UA_PREFIX + "Variant Rules");
        abvToName.put("UAWA", UA_PREFIX + "Waterborne Adventures");
        abvToName.put("UAWAW", UA_PREFIX + "Warlock and Wizard");
        abvToName.put("UAWGE", "Wayfinder's Guide to Eberron");
        abvToName.put("UAWR", UA_PREFIX + "Wizard Revisited");
        abvToName.put("VD", "Vecna Dossier");
        abvToName.put("VGM", "Volo's Guide to Monsters");
        abvToName.put("VRGR", "Van Richten's Guide to Ravenloft");
        abvToName.put("WBtW", "The Wild Beyond the Witchlight");
        abvToName.put("WDH", "Waterdeep: Dragon Heist");
        abvToName.put("WDMM", "Waterdeep: Dungeon of the Mad Mage");
        abvToName.put("XGE", "Xanathar's Guide to Everything");
        abvToName.put("XMtS", "X Marks the Spot");

        // Aliases: long to short or alternate form

        sourceToAbv.put("ALCurseOfStrahd", "ALCoS");
        sourceToAbv.put("ALElementalEvil", "ALEE");
        sourceToAbv.put("ALRageOfDemons", "ALRoD");

        sourceToAbv.put("ScreenDungeonKit", "SCREEN_DUNGEON_KIT");
        sourceToAbv.put("ScreenWildernessKit", "SCREEN_WILDERNESS_KIT");
        sourceToAbv.put("Screen", "SCREEN");

        sourceToAbv.put("UA2020F", "UA20F");
        sourceToAbv.put("UA2020Feats", "UA20F");
        sourceToAbv.put("UA2020POR", "UA20POR");
        sourceToAbv.put("UA2020PsionicOptionsRevisited", "UA20POR");
        sourceToAbv.put("UA2020SC1", "UA20S1");
        sourceToAbv.put("UA2020SC2", "UA20S2");
        sourceToAbv.put("UA2020SC3", "UA20S3");
        sourceToAbv.put("UA2020SC4", "UA20S4");
        sourceToAbv.put("UA2020SC5", "UA20S5");
        sourceToAbv.put("UA2020SCR", "UA20SCR");
        sourceToAbv.put("UA2020SMT", "UA20SMT");
        sourceToAbv.put("UA2020SpellsAndMagicTattoos", "UA20SMT");
        sourceToAbv.put("UA2020SubclassesPt1", "UA20S1");
        sourceToAbv.put("UA2020SubclassesPt2", "UA20S2");
        sourceToAbv.put("UA2020SubclassesPt3", "UA20S3");
        sourceToAbv.put("UA2020SubclassesPt4", "UA20S4");
        sourceToAbv.put("UA2020SubclassesPt5", "UA20S5");
        sourceToAbv.put("UA2020SubclassesRevisited", "UA20SCR");
        sourceToAbv.put("UA2021DO", "UA21DO");
        sourceToAbv.put("UA2021DraconicOptions", "UA21DO");
        sourceToAbv.put("UA2021FF", "UA21FF");
        sourceToAbv.put("UA2021FolkOfTheFeywild", "UA21FF");
        sourceToAbv.put("UA2021GL", "UA21GL");
        sourceToAbv.put("UA2021GothicLineages", "UA21GL");
        sourceToAbv.put("UA2021MoS", "UA21MoS");
        sourceToAbv.put("UA2021MagesOfStrixhaven", "UA21MoS");
        sourceToAbv.put("UA2021TotM", "UA21TotM");
        sourceToAbv.put("UA2021TravelersOfTheMultiverse", "UA21TotM");
        sourceToAbv.put("UA2022GO", "UA22GO");
        sourceToAbv.put("UA2022GiantOptions", "UA22GO");
        sourceToAbv.put("UA2022HoK", "UA22HoK");
        sourceToAbv.put("UA2022HeroesOfKrynn", "UA22HoK");
        sourceToAbv.put("UA2022HoKR", "UA22HoKR");
        sourceToAbv.put("UA2022HeroesOfKrynnRevisited", "UA22HoKR");
        sourceToAbv.put("UA2022WotM", "UA22WotM");
        sourceToAbv.put("UA2022WondersOfTheMultiverse", "UA22WotM");
        sourceToAbv.put("UAATrioOfSubclasses", "UAATOSC");
        sourceToAbv.put("UAArtificer", "UAA");
        sourceToAbv.put("UAArtificerRevisited", "UAAR");
        sourceToAbv.put("UAArtificerRevisited", "UAAR");
        sourceToAbv.put("UABarbarianAndMonk", "UABAM");
        sourceToAbv.put("UABarbarianPrimalPaths", "UABPP");
        sourceToAbv.put("UABardAndPaladin", "UABAP");
        sourceToAbv.put("UABardBardColleges", "UABBC");
        sourceToAbv.put("UACentaursMinotaurs", "UACAM");
        sourceToAbv.put("UAClassFeatureVariants", "UACFV");
        sourceToAbv.put("UAClassFeatureVariants", "UACFV");
        sourceToAbv.put("UAClericDivineDomains", "UACDD");
        sourceToAbv.put("UAClericDruidWizard", "UACDW");
        sourceToAbv.put("UAClericDruidWizard", "UACDW");
        sourceToAbv.put("UADruid", "UAD");
        sourceToAbv.put("UAEberron", "UAEBB");
        sourceToAbv.put("UAEladrinAndGith", "UAEAG");
        sourceToAbv.put("UAElfSubraces", "UAESR");
        sourceToAbv.put("UAFeats", "UAFT");
        sourceToAbv.put("UAFeatsForRaces", "UAFFR");
        sourceToAbv.put("UAFeatsForSkills", "UAFFS");
        sourceToAbv.put("UAFiendishOptions", "UAFO");
        sourceToAbv.put("UAFighter", "UAF");
        sourceToAbv.put("UAFighterRangerRogue", "UAFRR");
        sourceToAbv.put("UAFighterRogueWizard", "UAFRW");
        sourceToAbv.put("UAGiantSoulSorcerer", "UAGSS");
        sourceToAbv.put("UAGothicHeroes", "UAGH");
        sourceToAbv.put("UAGreyhawkInitiative", "UAGHI");
        sourceToAbv.put("UAKitsOfOld", "UAKOO");
        sourceToAbv.put("UALightDarkUnderdark", "UALDR");
        sourceToAbv.put("UAMassCombat", "UAMAC");
        sourceToAbv.put("UAModernMagic", "UAMDM");
        sourceToAbv.put("UAModifyingClasses", "UAMC");
        sourceToAbv.put("UAMonk", "UAM");
        sourceToAbv.put("UAOfShipsAndSea", "UAOSS");
        sourceToAbv.put("UAOrderDomain", "UAOD");
        sourceToAbv.put("UAPaladin", "UAP");
        sourceToAbv.put("UAPrestigeClassesRunMagic", "UAPCRM");
        sourceToAbv.put("UARacesOfEberron", "UARoE");
        sourceToAbv.put("UARacesOfRavnica", "UARoR");
        sourceToAbv.put("UARanger", "UAR");
        sourceToAbv.put("UARangerAndRogue", "UARAR");
        sourceToAbv.put("UARevisedClassOptions", "UARCO");
        sourceToAbv.put("UARevisedSubclasses", "UARSC");
        sourceToAbv.put("UASidekicks", "UASIK");
        sourceToAbv.put("UASorcerer", "UAS");
        sourceToAbv.put("UASorcererAndWarlock", "UASAW");
        sourceToAbv.put("UAStarterSpells", "UASSP");
        sourceToAbv.put("UAThatOldBlackMagic", "UATOBM");
        sourceToAbv.put("UATheFaithful", "UATF");
        sourceToAbv.put("UATheMysticClass", "UATMC");
        sourceToAbv.put("UATheRangerRevised", "UATRR");
        sourceToAbv.put("UAThreePillarExperience", "UA3PE");
        sourceToAbv.put("UAThreeSubclasses", "UATSC");
        sourceToAbv.put("UAVariantRules", "UAVR");
        sourceToAbv.put("UAWarlockAndWizard", "UAWAW");
        sourceToAbv.put("UAWaterborneAdventures", "UAWA");
        sourceToAbv.put("UAWayfindersGuideToEberron", "UAWGE");
        sourceToAbv.put("UAWizardRevisited", "UAWR");
    }

    public void checkKnown(Json5eTui tui, Set<String> missing) {
        bookSources.forEach(s -> {
            if (abvToName.containsKey(s)) {
                return;
            }
            String alternate = sourceToAbv.get(s);
            if (alternate != null) {
                return;
            }
            if (missing.add(s)) {
                tui.warnf("Source %s is unknown", s);
            }
        });
    }
}
