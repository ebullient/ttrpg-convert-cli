<%*
// Templater script to rename files in the vault before updating to 2.1.0 CLI output.
// This will move files from 1.x or 2.0.x to 2.1.x

// 1. Copy this file into your templates directory
// 2. Update the following settings:
//
// 2a. EDIT Path to the compendium (contains backgrounds, bestiary, etc) from the vault root
const compendium = "compendium";
//
// 2b. EDIT Path to the rules (contains actions, and rule variants) from the vault root
const rules = "rules";
//
// 2c. EDIT How many files to rename at a time?
//     This operation can take a bit (as obsidian updates associated links),
//     so it is best done in batches.
const limit = 50;
//
// 3. Create a new/temporary note, and use the "Templater: Open Insert Template Modal"
//    and choose this template. The note content will be updated with the files that have 
//    been renamed. You can also monitor progress in Developer Tools console 
//    (View -> Toggle Developer Tools)

var f;
let count = 0;

tR += "| File | New Name |\n";
tR += "|------|----------|\n";

const c = slashify(compendium);
const r = slashify(rules);

function slashify(path) {
    if (path.length > 0 && path.endsWith("/")) {
        return path;
    }
    return `${path}/`;
}

async function moveCompendiumFile(path, oldname, newname) {
    const p = slashify(path);
    await moveFile(`${c}${p}${oldname}`, `${c}${p}${newname}`);
}
async function moveRulesFile(path, oldname, newname) {
    const p = slashify(path);
    await moveFile(`${r}${p}${oldname}`, `${r}${p}${newname}`);
}
async function moveCompendiumToRules(oldname, newname) {
    await moveFile(`${c}${oldname}`, `${r}${newname}`);
}
async function moveRulesToCompendium(oldname, newname) {
    await moveFile(`${r}${oldname}`, `${c}${newname}`);
}

async function moveFile(oldname, newname) {
    if (count > limit) {
        return;
    }
    const file = await window.app.metadataCache.getFirstLinkpathDest(oldname, "");
    if (file) {
        console.log(`Moving ${oldname} to ${newname}`)
        await this.app.fileManager.renameFile(file, newname);
        tR += `| ${oldname} | ${newname} |\n`;
        count++; // increment counter after moving something
    }
}

// 1.x to 2.0.x

await moveCompendiumFile("classes", 
        "barbarian-path-of-the-ancestral-guardian", 
        "barbarian-path-of-the-ancestral-guardian-xge");
await moveCompendiumFile("classes", 
        "barbarian-path-of-the-battlerager", 
        "barbarian-path-of-the-battlerager-scag");
await moveCompendiumFile("classes", 
        "barbarian-path-of-the-beast", 
        "barbarian-path-of-the-beast-tce");
await moveCompendiumFile("classes", 
        "barbarian-path-of-the-storm-herald", 
        "barbarian-path-of-the-storm-herald-xge");
await moveCompendiumFile("classes",
        "barbarian-path-of-the-zealot", 
        "barbarian-path-of-the-zealot-xge");
await moveCompendiumFile("classes",
        "barbarian-path-of-wild-magic", 
        "barbarian-path-of-wild-magic-tce");
await moveCompendiumFile("classes",
        "bard-college-of-creation", 
        "bard-college-of-creation-tce");
await moveCompendiumFile("classes",
        "bard-college-of-eloquence", 
        "bard-college-of-eloquence-tce");
await moveCompendiumFile("classes",
        "bard-college-of-glamour", 
        "bard-college-of-glamour-xge");
await moveCompendiumFile("classes",
        "bard-college-of-swords", 
        "bard-college-of-swords-xge");
await moveCompendiumFile("classes",
        "bard-college-of-whispers", 
        "bard-college-of-whispers-xge");
await moveCompendiumFile("classes",
        "cleric-arcana-domain", 
        "cleric-arcana-domain-scag");
await moveCompendiumFile("classes",
        "cleric-forge-domain", 
        "cleric-forge-domain-xge");
await moveCompendiumFile("classes",
        "cleric-grave-domain", 
        "cleric-grave-domain-xge");
await moveCompendiumFile("classes",
        "cleric-order-domain", 
        "cleric-order-domain-tce");
await moveCompendiumFile("classes",
        "cleric-peace-domain", 
        "cleric-peace-domain-tce");
await moveCompendiumFile("classes",
        "cleric-twilight-domain", 
        "cleric-twilight-domain-tce");
await moveCompendiumFile("classes",
        "druid-circle-of-dreams", 
        "druid-circle-of-dreams-xge");
await moveCompendiumFile("classes",
        "druid-circle-of-spores", 
        "druid-circle-of-spores-tce");
await moveCompendiumFile("classes",
        "druid-circle-of-stars", 
        "druid-circle-of-stars-tce");
await moveCompendiumFile("classes",
        "druid-circle-of-the-shepherd", 
        "druid-circle-of-the-shepherd-xge");
await moveCompendiumFile("classes",
        "druid-circle-of-wildfire", 
        "druid-circle-of-wildfire-tce");
await moveCompendiumFile("classes",
        "fighter-arcane-archer", 
        "fighter-arcane-archer-xge");
await moveCompendiumFile("classes",
        "fighter-cavalier", 
        "fighter-cavalier-xge");
await moveCompendiumFile("classes",
        "fighter-echo-knight", 
        "fighter-echo-knight-egw");
await moveCompendiumFile("classes",
        "fighter-psi-warrior", 
        "fighter-psi-warrior-tce");
await moveCompendiumFile("classes",
        "fighter-purple-dragon-knight-banneret", 
        "fighter-purple-dragon-knight-banneret-scag");
await moveCompendiumFile("classes",
        "fighter-rune-knight", 
        "fighter-rune-knight-tce");
await moveCompendiumFile("classes",
        "fighter-samurai", 
        "fighter-samurai-xge");
await moveCompendiumFile("classes",
        "monk-way-of-mercy", 
        "monk-way-of-mercy-tce");
await moveCompendiumFile("classes",
        "monk-way-of-the-ascendant-dragon", 
        "monk-way-of-the-ascendant-dragon-ftd");
await moveCompendiumFile("classes",
        "monk-way-of-the-astral-self", 
        "monk-way-of-the-astral-self-tce");
await moveCompendiumFile("classes",
        "monk-way-of-the-drunken-master", 
        "monk-way-of-the-drunken-master-xge");
await moveCompendiumFile("classes",
        "monk-way-of-the-kensei", 
        "monk-way-of-the-kensei-xge");
await moveCompendiumFile("classes",
        "monk-way-of-the-long-death", 
        "monk-way-of-the-long-death-scag");
await moveCompendiumFile("classes",
        "monk-way-of-the-sun-soul", 
        "monk-way-of-the-sun-soul-xge");
await moveCompendiumFile("classes",
        "paladin-oath-of-conquest", 
        "paladin-oath-of-conquest-xge");
await moveCompendiumFile("classes",
        "paladin-oath-of-glory", 
        "paladin-oath-of-glory-tce");
await moveCompendiumFile("classes",
        "paladin-oath-of-redemption", 
        "paladin-oath-of-redemption-xge");
await moveCompendiumFile("classes",
        "paladin-oath-of-the-crown", 
        "paladin-oath-of-the-crown-scag");
await moveCompendiumFile("classes",
        "paladin-oath-of-the-watchers", 
        "paladin-oath-of-the-watchers-tce");
await moveCompendiumFile("classes",
        "ranger-drakewarden", 
        "ranger-drakewarden-ftd");
await moveCompendiumFile("classes",
        "ranger-fey-wanderer", 
        "ranger-fey-wanderer-tce");
await moveCompendiumFile("classes",
        "ranger-gloom-stalker", 
        "ranger-gloom-stalker-xge");
await moveCompendiumFile("classes",
        "ranger-horizon-walker", 
        "ranger-horizon-walker-xge");
await moveCompendiumFile("classes",
        "ranger-monster-slayer", 
        "ranger-monster-slayer-xge");
await moveCompendiumFile("classes",
        "ranger-swarmkeeper", 
        "ranger-swarmkeeper-tce");
await moveCompendiumFile("classes",
        "rogue-inquisitive", 
        "rogue-inquisitive-xge");
await moveCompendiumFile("classes",
        "rogue-mastermind", 
        "rogue-mastermind-xge");
await moveCompendiumFile("classes",
        "rogue-phantom", 
        "rogue-phantom-tce");
await moveCompendiumFile("classes",
        "rogue-scout", 
        "rogue-scout-xge");
await moveCompendiumFile("classes",
        "rogue-soulknife", 
        "rogue-soulknife-tce");
await moveCompendiumFile("classes",
        "rogue-swashbuckler", 
        "rogue-swashbuckler-xge");
await moveCompendiumFile("classes",
        "sorcerer-aberrant-mind", 
        "sorcerer-aberrant-mind-tce");
await moveCompendiumFile("classes",
        "sorcerer-clockwork-soul", 
        "sorcerer-clockwork-soul-tce");
await moveCompendiumFile("classes",
        "sorcerer-divine-soul", 
        "sorcerer-divine-soul-xge");
await moveCompendiumFile("classes",
        "sorcerer-shadow-magic", 
        "sorcerer-shadow-magic-xge");
await moveCompendiumFile("classes",
        "sorcerer-storm-sorcery", 
        "sorcerer-storm-sorcery-xge");
await moveCompendiumFile("classes",
        "warlock-the-celestial", 
        "warlock-the-celestial-xge");
await moveCompendiumFile("classes",
        "warlock-the-fathomless", 
        "warlock-the-fathomless-tce");
await moveCompendiumFile("classes",
        "warlock-the-genie", 
        "warlock-the-genie-tce");
await moveCompendiumFile("classes",
        "warlock-the-hexblade", 
        "warlock-the-hexblade-xge");
await moveCompendiumFile("classes",
        "warlock-the-undying", 
        "warlock-the-undying-scag");
await moveCompendiumFile("classes",
        "wizard-bladesinging", 
        "wizard-bladesinging-tce");
await moveCompendiumFile("classes",
        "wizard-chronurgy-magic", 
        "wizard-chronurgy-magic-egw");
await moveCompendiumFile("classes",
        "wizard-graviturgy-magic", 
        "wizard-graviturgy-magic-egw");
await moveCompendiumFile("classes",
        "wizard-order-of-scribes", 
        "wizard-order-of-scribes-tce");
await moveCompendiumFile("classes",
        "wizard-war-magic", 
        "wizard-war-magic-xge");

// 2.0.x to 2.1.x

await moveCompendiumFile("adventures", 
        "essentials-kit-storm-lords-wrath/img/023-ynkgo-donnabella.png",
        "essentials-kit-divine-contention/img/023-ynkgo-donnabella.png");
await moveCompendiumFile("adventures", 
        "essentials-kit-storm-lords-wrath/img/024-leeom-galandro.png",
        "essentials-kit-divine-contention/img/024-leeom-galandro.png");
await moveCompendiumFile("adventures", 
        "essentials-kit-storm-lords-wrath/img/025-l3v6r-inverna.png",
        "essentials-kit-divine-contention/img/025-l3v6r-inverna.png");
await moveCompendiumFile("adventures", 
        "essentials-kit-storm-lords-wrath/img/026-3krsm-nib.png",
        "essentials-kit-divine-contention/img/026-3krsm-nib.png");
await moveCompendiumFile("adventures", 
        "essentials-kit-storm-lords-wrath/img/027-pu4f2-pete.png",
        "essentials-kit-divine-contention/img/027-pu4f2-pete.png");
await moveCompendiumFile("adventures", 
        "essentials-kit-storm-lords-wrath/img/028-gttxp-quinn.png",
        "essentials-kit-divine-contention/img/028-gttxp-quinn.png");
await moveCompendiumFile("adventures", 
        "essentials-kit-storm-lords-wrath/img/029-1qyge-ruby.png",
        "essentials-kit-divine-contention/img/029-1qyge-ruby.png");
await moveCompendiumFile("adventures", 
        "essentials-kit-storm-lords-wrath/img/030-nemem-shanjan.png",
        "essentials-kit-divine-contention/img/030-nemem-shanjan.png");
await moveCompendiumFile("adventures", 
        "essentials-kit-storm-lords-wrath/img/031-nrqv1-talon.png",
        "essentials-kit-divine-contention/img/031-nrqv1-talon.png");
await moveCompendiumFile("adventures", 
        "tales-from-the-yawning-portal-dead-in-thay/img/durnan.jpg",
        "tales-from-the-yawning-portal-against-the-giants/img/durnan.jpg");
await moveCompendiumFile("bestiary", 
        "fiend/img/archfiend-of-ifnir.webp",
        "beast/img/archfiend-of-ifnir.webp");
await moveCompendiumFile("bestiary", 
        "humanoid/img/bhaal.webp",
        "beast/img/bhaal.webp");
await moveCompendiumFile("bestiary", 
        "npc/img/four-armed-troll.jpg",
        "giant/img/four-armed-troll.jpg");
await moveCompendiumFile("bestiary", 
        "beast/img/river-serpent.webp",
        "monstrosity/img/river-serpent.webp");
await moveCompendiumFile("bestiary", 
        "humanoid/img/045-05-005-divas-attack.webp",
        "npc/img/045-05-005-divas-attack.webp");
await moveCompendiumFile("bestiary", 
        "construct/img/sacred-statue.webp",
        "undead/img/sacred-statue.webp");
await moveCompendiumFile("books", 
        "dungeon-masters-screen-spelljammer/1-.md",
        "dungeon-masters-screen-spelljammer/1.md");
await moveCompendiumFile("items", 
        "ball-bearings-bag-of-1-000.md",
        "ball-bearings-bag-of-1000.md");
await moveRulesFile("variant-rules", 
        "action-points.md",
        "action-points-uaeberron.md");
await moveRulesFile("variant-rules", 
        "adamantine-weapons.md",
        "adamantine-weapons-xge.md");
await moveRulesFile("variant-rules", 
        "common-languages.md",
        "common-languages-uawge.md");
await moveRulesFile("variant-rules", 
        "crashing-a-ship.md",
        "crashing-a-ship-uaofshipsandsea.md");
await moveRulesFile("variant-rules", 
        "crashing.md",
        "crashing-aag.md");
await moveRulesFile("variant-rules", 
        "creating-magic-items.md",
        "creating-magic-items-uawge.md");
await moveRulesFile("variant-rules", 
        "crew.md",
        "crew-aag.md");
await moveRulesFile("variant-rules", 
        "custom-alignments.md",
        "custom-alignments-uavariantrules.md");
await moveRulesFile("variant-rules", 
        "customizing-your-origin.md",
        "customizing-your-origin-tce.md");
await moveRulesFile("variant-rules", 
        "downtime-activity-buying-a-magic-item.md",
        "downtime-activity-buying-a-magic-item-xge.md");
await moveRulesFile("variant-rules", 
        "downtime-activity-crafting-an-item.md",
        "downtime-activity-crafting-an-item-xge.md");
await moveRulesFile("variant-rules", 
        "downtime-activity-crime.md",
        "downtime-activity-crime-xge.md");
await moveRulesFile("variant-rules", 
        "downtime-activity-gambling.md",
        "downtime-activity-gambling-xge.md");
await moveRulesFile("variant-rules", 
        "downtime-activity-pit-fighting.md",
        "downtime-activity-pit-fighting-xge.md");
await moveRulesFile("variant-rules", 
        "downtime-activity-relaxation.md",
        "downtime-activity-relaxation-xge.md");
await moveRulesFile("variant-rules", 
        "downtime-activity-religious-service.md",
        "downtime-activity-religious-service-xge.md");
await moveRulesFile("variant-rules", 
        "downtime-activity-research.md",
        "downtime-activity-research-xge.md");
await moveRulesFile("variant-rules", 
        "downtime-activity-scribing-a-spell-scroll.md",
        "downtime-activity-scribing-a-spell-scroll-xge.md");
await moveRulesFile("variant-rules", 
        "downtime-activity-selling-a-magic-item.md",
        "downtime-activity-selling-a-magic-item-xge.md");
await moveRulesFile("variant-rules", 
        "downtime-activity-work.md",
        "downtime-activity-work-xge.md");
await moveRulesFile("variant-rules", 
        "downtime-and-franchise-activity-explore-territory.md",
        "downtime-and-franchise-activity-explore-territory-ai.md");
await moveRulesFile("variant-rules", 
        "downtime-and-franchise-activity-franchise-restructuring.md",
        "downtime-and-franchise-activity-franchise-restructuring-ai.md");
await moveRulesFile("variant-rules", 
        "downtime-and-franchise-activity-headquarters-modification.md",
        "downtime-and-franchise-activity-headquarters-modification-ai.md");
await moveRulesFile("variant-rules", 
        "downtime-and-franchise-activity-marketeering.md",
        "downtime-and-franchise-activity-marketeering-ai.md");
await moveRulesFile("variant-rules", 
        "downtime-and-franchise-activity-philanthropic-enterprise.md",
        "downtime-and-franchise-activity-philanthropic-enterprise-ai.md");
await moveRulesFile("variant-rules", 
        "downtime-and-franchise-activity-running-a-franchise.md",
        "downtime-and-franchise-activity-running-a-franchise-ai.md");
await moveRulesFile("variant-rules", 
        "downtime-and-franchise-activity-schmoozing.md",
        "downtime-and-franchise-activity-schmoozing-ai.md");
await moveRulesFile("variant-rules", 
        "downtime-and-franchise-activity-scrutineering.md",
        "downtime-and-franchise-activity-scrutineering-ai.md");
await moveRulesFile("variant-rules", 
        "downtime-and-franchise-activity-shady-business-practice.md",
        "downtime-and-franchise-activity-shady-business-practice-ai.md");
await moveRulesFile("variant-rules", 
        "downtime-and-franchise-activity-team-building.md",
        "downtime-and-franchise-activity-team-building-ai.md");
await moveRulesFile("variant-rules", 
        "downtime-revisited.md",
        "downtime-revisited-xge.md");
await moveRulesFile("variant-rules", 
        "emrakuls-madness.md",
        "emrakuls-madness-psi.md");
await moveRulesFile("variant-rules", 
        "encounters-at-sea.md",
        "encounters-at-sea-gos.md");
await moveRulesFile("variant-rules", 
        "environmental-elements.md",
        "environmental-elements-uawge.md");
await moveRulesFile("variant-rules", 
        "falling.md",
        "falling-xge.md");
await moveRulesFile("variant-rules", 
        "fear-and-stress.md",
        "fear-and-stress-vrgr.md");
await moveRulesFile("variant-rules", 
        "greyhawk-initiative.md",
        "greyhawk-initiative-uagreyhawkinitiative.md");
await moveRulesFile("variant-rules", 
        "haunted-traps.md",
        "haunted-traps-vrgr.md");
await moveRulesFile("variant-rules", 
        "human-languages.md",
        "human-languages-scag.md");
await moveRulesFile("variant-rules", 
        "magic-tattoos.md",
        "magic-tattoos-ua2020spellsandmagictattoos.md");
await moveRulesFile("variant-rules", 
        "mass-combat.md",
        "mass-combat-uamasscombat.md");
await moveRulesFile("variant-rules", 
        "mysterious-islands.md",
        "mysterious-islands-gos.md");
await moveRulesFile("variant-rules", 
        "ocean-environs.md",
        "ocean-environs-gos.md");
await moveRulesFile("variant-rules", 
        "officers-and-crew.md",
        "officers-and-crew-gos.md");
await moveRulesFile("variant-rules", 
        "optional-class-features.md",
        "optional-class-features-tce.md");
await moveRulesFile("variant-rules", 
        "owning-a-ship.md",
        "owning-a-ship-uaofshipsandsea.md");
await moveRulesFile("variant-rules", 
        "players-make-all-rolls.md",
        "players-make-all-rolls-uavariantrules.md");
await moveRulesFile("variant-rules", 
        "prestige-classes.md",
        "prestige-classes-uaprestigeclassesrunmagic.md");
await moveRulesFile("variant-rules", 
        "random-ships.md",
        "random-ships-gos.md");
await moveRulesFile("variant-rules", 
        "rune-magic.md",
        "rune-magic-uaprestigeclassesrunmagic.md");
await moveRulesFile("variant-rules", 
        "shared-campaign-variant-rules.md",
        "shared-campaign-variant-rules-xge.md");
await moveRulesFile("variant-rules", 
        "ship-crew.md",
        "ship-crew-uaofshipsandsea.md");
await moveRulesFile("variant-rules", 
        "ship-officers.md",
        "ship-officers-uaofshipsandsea.md");
await moveRulesFile("variant-rules", 
        "ship-repairs.md",
        "ship-repairs-aag.md");
await moveRulesFile("variant-rules", 
        "ship-stat-blocks.md",
        "ship-stat-blocks-uaofshipsandsea.md");
await moveRulesFile("variant-rules", 
        "ship-to-ship-combat.md",
        "ship-to-ship-combat-aag.md");
await moveRulesFile("variant-rules", 
        "ships-in-combat.md",
        "ships-in-combat-uaofshipsandsea.md");
await moveRulesFile("variant-rules", 
        "sidekicks.md",
        "sidekicks-uasidekicks.md");
await moveRulesFile("variant-rules", 
        "simultaneous-effects.md",
        "simultaneous-effects-xge.md");
await moveRulesFile("variant-rules", 
        "sleep.md",
        "sleep-xge.md");
await moveRulesFile("variant-rules", 
        "spellcasting.md",
        "spellcasting-xge.md");
await moveRulesFile("variant-rules", 
        "superior-ship-upgrades.md",
        "superior-ship-upgrades-gos.md");
await moveRulesFile("variant-rules", 
        "survivors.md",
        "survivors-vrgr.md");
await moveRulesFile("variant-rules", 
        "swapping-racial-languages.md",
        "swapping-racial-languages-uawge.md");
await moveRulesFile("variant-rules", 
        "three-pillar-experience.md",
        "three-pillar-experience-uathreepillarexperience.md");
await moveRulesFile("variant-rules", 
        "tool-proficiencies.md",
        "tool-proficiencies-xge.md");
await moveRulesFile("variant-rules", 
        "travel-at-sea.md",
        "travel-at-sea-uaofshipsandsea.md");
await moveRulesFile("variant-rules", 
        "two-handed-arcane-focuses.md",
        "two-handed-arcane-focuses-uawge.md");
await moveRulesFile("variant-rules", 
        "tying-knots.md",
        "tying-knots-xge.md");
await moveRulesFile("variant-rules", 
        "vitality.md",
        "vitality-uavariantrules.md");
await moveRulesFile("variant-rules", 
        "wild-shape-forms.md",
        "wild-shape-forms-uadruid.md");

if (count == 0) {
    tR += "|  |  |\n\nNothing to rename\n";
}
tR += "\n";
%>
