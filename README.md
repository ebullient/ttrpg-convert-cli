# json5e-convert-cli

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

I use [Obsidian](https://obsidian.md) to keep track of my campaign notes.
 The goal of this project is to parse json sources for materials I own and create formatted markdown that I can reference in my notes.
 
Note: This project is a derivative of [fc5-convert-cli](ebullient/fc5-convert-cli), which focused on working to and from FightClub5 Compendium XML files. It has also stolen some bits and pieces from [pockets-cli](ebullient/pockets-cli).

## To run without building yourself

1. Install JBang: https://www.jbang.dev/documentation/guide/latest/installation.html
2. Install the snapshot jar: 
  ```
  jbang app install --name 5e-convert --force --fresh https://jitpack.io/dev/ebullient/json5e-convert-cli/1.0.0-SNAPSHOT/json5e-convert-cli-1.0.0-SNAPSHOT-runner.jar
  ```
3. Run the command: 
  ```
  5e-convert --help
  ```

  Note: feel free to use an alternate alias here.

## To build (optional)

1. Clone this repository
2. Build this project: `quarkus build` or `./mvnw install`
3. `java -jar target/json5e-convert-cli-1.0.0-SNAPSHOT-runner.jar --help`


To run commands listed below, either: 

- Replace `5e-convert` with `java -jar target/json5e-convert-cli-1.0.0-SNAPSHOT-runner.jar`
- Use JBang to create an alias that points to the built jar: 
  ```
  jbang app install --name 5e-convert --force --fresh ~/.m2/repository/dev/ebullient/json5e-convert-cli/1.0.0-SNAPSHOT/json5e-convert-cli-1.0.0-SNAPSHOT-runner.jar
    ```

## Starting with 5eTools JSON data

An example invocation (based on sources I own): 

```
5e-convert 5etools \
  --index \
  -o dm \
  dm-sources.json ~/git/dnd/5etools-mirror-1.github.io/data my-items.json
```

- `--index` Create `all-index.json` containing all of the touched artifact ids, and `src-index.json` that shows the filtered/allowed artifact ids. These files are useful when tweaking exclude rules (as shown below).
- `-o dm` The target output directory. Files will be created in this directory.

The rest of the command-line specifies input files: 

- `dm-sources.json` Additional parameters (shown in detail below)
- `~/git/dnd/5etools-mirror-1.github.io/data` Path to the data directory containing 5etools files
- `my-items.json` Custom items

### Additional parameters

Additional instructions for dealing with 5etools data can be supplied in a json file like this (based on sources I own): 

```json
{
  "from": [
    "AI",
    "PHB",
    "DMG",
    "TCE",
    "XGE",

    "SCAG",
    "EGW",
    "FS",
    "MaBJoV",

    "AWM",
    "CM",
    "LMoP",
    "ESK",
    "DIP",
    "SLW",
    "SDW",
    "DC",
    "IDRotF",
    "PotA",
    "EEPC",
    "GoS",
    "OoW",
    "TftYP",
    "TftYP-AtG",
    "TftYP-DiT",
    "TftYP-TFoF",
    "TftYP-THSoT",
    "TftYP-TSC",
    "TftYP-ToH",
    "TftYP-WPM",
    "WBtW",
    "WDH",
    "WDMM",

    "FTD",
    "MM",
    "MTF",
    "VGM"
  ],
  "paths": {
    "rules": "/compendium/rules/"
  },
  "excludePattern": [
    "race|.*|dmg"
  ],
  "exclude": [
    "monster|expert|dc",
    "monster|expert|sdw",
    "monster|expert|slw"
  ]
}
```

- `from` defines the array of all sources. Only include content from sources you own. If you omit this parameter (and don't specify any other sources on the command line), this tool will use content from the SRD. 
- `paths` allows you to specify a document path for cross-document links, and to find conditions, and weapon/item properties. By default, items, spells, monsters, backgrounds, races, and classes are in `/compendium/`, while files defining conditions and properties are in `/rules/`.
- `exclude`, `include`, `includePattern` and `excludePattern` work against the identifiers (listed in generated index files). They allow you to further tweak/constrain what is emitted in formatted markdown. In the above example, I'm excluding all of the race variants from the DMG, and the monster-form of the expert sidekick from the Essentials Kit. I own both of these books, but I don't want those creatures in the formatted bestiary.

To generate player-focused reference content for a Wild Beyond the Witchlight campaign, I've constrained things further. 
I am pulling from a much smaller set of sources. I included Elemental Evil Player's Companion (Genasi) and Volo's Guide to Monsters (Tabaxi), but then added exclude patterns to remove elements from these sourcebooks that I don't want my players to use in this campaign (some simplification for beginners):

```json
{
  "from": [
    "PHB",
    "DMG",
    "XGE",
    "TCE",
    "EEPC",
    "WBtW",
    "VGM"
  ],
  "includeGroups": [
    "familiars"
  ],
  "excludePattern": [
    ".*sidekick.*",
    "race|.*|dmg",
    "race|(?!tabaxi).*|vgm",
    "subrace|.*|aasimar|vgm",
    "item|.*|vgm",
    "monster|.*|tce",
    "monster|.*|dmg",
    "monster|.*|vgm",
    "monster|.*|wbtw",
    "monster|animated object.*|phb"
  ],
  "exclude": [
    "race|aarakocra|eepc",
    "feat|actor|phb",
    "feat|artificer initiate|tce",
    "feat|athlete|phb",
    "feat|bountiful luck|xge",
    "feat|chef|tce",
    "feat|dragon fear|xge",
    "feat|dragon hide|xge",
    "feat|drow high magic|xge",
    "feat|durable|phb",
    "feat|dwarven fortitude|xge",
    "feat|elven accuracy|xge",
    "feat|fade away|xge",
    "feat|fey teleportation|xge",
    "feat|fey touched|tce",
    "feat|flames of phlegethos|xge",
    "feat|gunner|tce",
    "feat|heavily armored|phb",
    "feat|heavy armor master|phb",
    "feat|infernal constitution|xge",
    "feat|keen mind|phb",
    "feat|lightly armored|phb",
    "feat|linguist|phb",
    "feat|lucky|phb",
    "feat|medium armor master|phb",
    "feat|moderately armored|phb",
    "feat|mounted combatant|phb",
    "feat|observant|phb",
    "feat|orcish fury|xge",
    "feat|piercer|tce",
    "feat|poisoner|tce",
    "feat|polearm master|phb",
    "feat|prodigy|xge",
    "feat|resilient|phb",
    "feat|second chance|xge",
    "feat|shadow touched|tce",
    "feat|skill expert|tce",
    "feat|slasher|tce",
    "feat|squat nimbleness|xge",
    "feat|tavern brawler|phb",
    "feat|telekinetic|tce",
    "feat|telepathic|tce",
    "feat|weapon master|phb",
    "feat|wood elf magic|xge",
    "item|iggwilv's cauldron|wbtw"
  ]
}
```


## Templates

This applicaiton uses the [Qute Templating Engine](https://quarkus.io/guides/qute). Simple customizations to markdown output can be achieved by copying a template from src/main/resources/templates, making the desired modifications, and then specifying that template on the command line.

```
5e-convert 5etools \
  --background src/main/resources/templates/background2md.txt \
  --index -o dm dm-sources.json ~/git/dnd/5etools-mirror-1.github.io/data my-items.json
```

### Template examples

- [Default templates](https://github.com/ebullient/fc5-convert-cli/tree/main/src/main/resources/templates)
- [Alternative templates](https://github.com/ebullient/fc5-convert-cli/tree/main/src/test/resources/customTemplates)

