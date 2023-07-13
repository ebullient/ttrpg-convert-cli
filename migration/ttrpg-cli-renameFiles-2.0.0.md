<%*
// Templater script to rename files in the vault before updating to 2.0.0 CLI output
// This will move files from 1.x to 2.0.x

// NOTE: There are a lot of files: this process can take awhile
// It goes faster if you disable sync and plugins that monitor files for changes

// 1. Copy this file into your templates directory

// 2. Update the following paths to match your setup: 

// 2a. EDIT Path to the compendium (contains backgrounds, bestiary, etc) from the vault root
const compendium = "/compendium";

// 2b. EDIT Path to the rules (contains actions, and rule variants) from the vault root
const rules = "/rules";

// 3. EDIT How many files to rename at a time?
const limit = 100;

// 4. Create a new/temporary note, and use the "Templater: Open Insert Template Modal"
// to insert this template into a note. It will update the note content with the files 
// that have been renamed. 


var f;
let count = 0;

tR += "| File | New Name |\n";
tR += "|------|----------|\n";

async function moveFile(path, oldname, newname) {
    await moveFile(compendium, path, oldname, newname);
}
async function moveRulesFile(path, oldname, newname) {
    await moveFile(rules, path, oldname, newname);
}

async function moveFile(base, path, oldname, newname) {
    if (count > limit) {
        return;
    }

    if (path) {
        path = path + "/";
    } else {
        path = "";
    }

    const oldpath = `${base}/${path}${oldname}`
    const file = await window.app.metadataCache.getFirstLinkpathDest(oldpath, "");

    if (file) {
        const newpath = `${base}/${path}${newname}.md`
        await this.app.fileManager.renameFile(
            file,
            newpath
        );
        tR += `| ${oldname} | ${newname} |\n`;

        count++; // increment counter after moving something
    }
}

// 5e files that will be renamed if present

await moveFile("classes", "barbarian-path-of-the-ancestral-guardian", "barbarian-path-of-the-ancestral-guardian-xge");
await moveFile("classes", "barbarian-path-of-the-battlerager", "barbarian-path-of-the-battlerager-scag");
await moveFile("classes", "barbarian-path-of-the-beast", "barbarian-path-of-the-beast-tce");
await moveFile("classes", "barbarian-path-of-the-storm-herald", "barbarian-path-of-the-storm-herald-xge");
await moveFile("classes", "barbarian-path-of-the-zealot", "barbarian-path-of-the-zealot-xge");
await moveFile("classes", "barbarian-path-of-wild-magic", "barbarian-path-of-wild-magic-tce");
await moveFile("classes", "bard-college-of-creation", "bard-college-of-creation-tce");
await moveFile("classes", "bard-college-of-eloquence", "bard-college-of-eloquence-tce");
await moveFile("classes", "bard-college-of-glamour", "bard-college-of-glamour-xge");
await moveFile("classes", "bard-college-of-swords", "bard-college-of-swords-xge");
await moveFile("classes", "bard-college-of-whispers", "bard-college-of-whispers-xge");
await moveFile("classes", "cleric-arcana-domain", "cleric-arcana-domain-scag");
await moveFile("classes", "cleric-forge-domain", "cleric-forge-domain-xge");
await moveFile("classes", "cleric-grave-domain", "cleric-grave-domain-xge");
await moveFile("classes", "cleric-order-domain", "cleric-order-domain-tce");
await moveFile("classes", "cleric-peace-domain", "cleric-peace-domain-tce");
await moveFile("classes", "cleric-twilight-domain", "cleric-twilight-domain-tce");
await moveFile("classes", "druid-circle-of-dreams", "druid-circle-of-dreams-xge");
await moveFile("classes", "druid-circle-of-spores", "druid-circle-of-spores-tce");
await moveFile("classes", "druid-circle-of-stars", "druid-circle-of-stars-tce");
await moveFile("classes", "druid-circle-of-the-shepherd", "druid-circle-of-the-shepherd-xge");
await moveFile("classes", "druid-circle-of-wildfire", "druid-circle-of-wildfire-tce");
await moveFile("classes", "fighter-arcane-archer", "fighter-arcane-archer-xge");
await moveFile("classes", "fighter-cavalier", "fighter-cavalier-xge");
await moveFile("classes", "fighter-echo-knight", "fighter-echo-knight-egw");
await moveFile("classes", "fighter-psi-warrior", "fighter-psi-warrior-tce");
await moveFile("classes", "fighter-purple-dragon-knight-banneret", "fighter-purple-dragon-knight-banneret-scag");
await moveFile("classes", "fighter-rune-knight", "fighter-rune-knight-tce");
await moveFile("classes", "fighter-samurai", "fighter-samurai-xge");
await moveFile("classes", "monk-way-of-mercy", "monk-way-of-mercy-tce");
await moveFile("classes", "monk-way-of-the-ascendant-dragon", "monk-way-of-the-ascendant-dragon-ftd");
await moveFile("classes", "monk-way-of-the-astral-self", "monk-way-of-the-astral-self-tce");
await moveFile("classes", "monk-way-of-the-drunken-master", "monk-way-of-the-drunken-master-xge");
await moveFile("classes", "monk-way-of-the-kensei", "monk-way-of-the-kensei-xge");
await moveFile("classes", "monk-way-of-the-long-death", "monk-way-of-the-long-death-scag");
await moveFile("classes", "monk-way-of-the-sun-soul", "monk-way-of-the-sun-soul-xge");
await moveFile("classes", "paladin-oath-of-conquest", "paladin-oath-of-conquest-xge");
await moveFile("classes", "paladin-oath-of-glory", "paladin-oath-of-glory-tce");
await moveFile("classes", "paladin-oath-of-redemption", "paladin-oath-of-redemption-xge");
await moveFile("classes", "paladin-oath-of-the-crown", "paladin-oath-of-the-crown-scag");
await moveFile("classes", "paladin-oath-of-the-watchers", "paladin-oath-of-the-watchers-tce");
await moveFile("classes", "ranger-drakewarden", "ranger-drakewarden-ftd");
await moveFile("classes", "ranger-fey-wanderer", "ranger-fey-wanderer-tce");
await moveFile("classes", "ranger-gloom-stalker", "ranger-gloom-stalker-xge");
await moveFile("classes", "ranger-horizon-walker", "ranger-horizon-walker-xge");
await moveFile("classes", "ranger-monster-slayer", "ranger-monster-slayer-xge");
await moveFile("classes", "ranger-swarmkeeper", "ranger-swarmkeeper-tce");
await moveFile("classes", "rogue-inquisitive", "rogue-inquisitive-xge");
await moveFile("classes", "rogue-mastermind", "rogue-mastermind-xge");
await moveFile("classes", "rogue-phantom", "rogue-phantom-tce");
await moveFile("classes", "rogue-scout", "rogue-scout-xge");
await moveFile("classes", "rogue-soulknife", "rogue-soulknife-tce");
await moveFile("classes", "rogue-swashbuckler", "rogue-swashbuckler-xge");
await moveFile("classes", "sorcerer-aberrant-mind", "sorcerer-aberrant-mind-tce");
await moveFile("classes", "sorcerer-clockwork-soul", "sorcerer-clockwork-soul-tce");
await moveFile("classes", "sorcerer-divine-soul", "sorcerer-divine-soul-xge");
await moveFile("classes", "sorcerer-shadow-magic", "sorcerer-shadow-magic-xge");
await moveFile("classes", "sorcerer-storm-sorcery", "sorcerer-storm-sorcery-xge");
await moveFile("classes", "warlock-the-celestial", "warlock-the-celestial-xge");
await moveFile("classes", "warlock-the-fathomless", "warlock-the-fathomless-tce");
await moveFile("classes", "warlock-the-genie", "warlock-the-genie-tce");
await moveFile("classes", "warlock-the-hexblade", "warlock-the-hexblade-xge");
await moveFile("classes", "warlock-the-undying", "warlock-the-undying-scag");
await moveFile("classes", "wizard-bladesinging", "wizard-bladesinging-tce");
await moveFile("classes", "wizard-chronurgy-magic", "wizard-chronurgy-magic-egw");
await moveFile("classes", "wizard-graviturgy-magic", "wizard-graviturgy-magic-egw");
await moveFile("classes", "wizard-order-of-scribes", "wizard-order-of-scribes-tce");
await moveFile("classes", "wizard-war-magic", "wizard-war-magic-xge");

// Pf2e files that will be renamed if present

await moveRulesFile("core-rulebook", "conditions-appendix", "appendix-a-conditions-appendix");

if (count == 0) {
    tR += "|  |  |\n\nNothing to rename\n";
}
tR += "\n"
%>
