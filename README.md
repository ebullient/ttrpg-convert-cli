# Json 5e convert CLI

This project uses Quarkus, the Supersonic Subatomic Java Framework. If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

I use [Obsidian](https://obsidian.md) to keep track of my campaign notes. This project parses json sources for materials that I own from the 5etools mirror to create linked and formatted markdown that I can reference in my notes.
 
Note: This project is a derivative of [fc5-convert-cli](ebullient/fc5-convert-cli), which focused on working to and from FightClub5 Compendium XML files. It has also stolen some bits and pieces from [pockets-cli](ebullient/pockets-cli).

## Recommended plugins 

- **[Admonitions](obsidian://show-plugin?id=obsidian-admonition)**: One of the templates for rendering monsters uses the code-style format supported by the admonitions plugin to render human-readable text (and avoid blockquote line wrapping).
  - Create a custom admonition called `statblock`
  - Either set the appearance/icon for the style in the plugin, or use/modify [this snippet](css-css-snippets/admonition_callout.css) to style the admonition itself
  - The [statblock css snippet](css-snippets/statblock.css) defines styles for the contents of the statblock. 

- **[Force note view mode by front matter](obsidian://show-plugin?id=obsidian-view-mode-by-frontmatter)**: I use this plugin to treat these generated notes as essentially read only. Specifically, I ensure the plugin has the following options enabled: "Ignore open files" (so that if I have toggled to edit mode, it doesn't fight with me over it), and "Ignore force view when not in front matter" (so that the setting isn't applied to documents that don't have the front matter tag).

- **[TTRPG Statblocks](obsidian://show-plugin?id=obsidian-5e-statblocks)**: Templates for rendering monsters can define a `statblock` in the document body or provide a full or abridged yaml monster in the document header to update monsters in the plugin's bestiary.
  - Note that in the process of creating this converter, I've discovered some corner cases that the TTRPG plugin does not handle well. I've adapted as best I can to work within what the TTRPG statblock format understands. Information will be rendered correctly in the plain statblock text for these cases.

- **[Initiative Tracker](obsidian://show-plugin?id=initiative-tracker)**: Templates for rendering monsters can include information in the header to define monsters that initiative tracker can use when constructing encounters.

## Conventions

- **Links** Documents generated by this plugin will use markdown links rather than wiki links. A css snippet can make these links less invasive in edit mode by hiding the URL portion of the string.

- **File names** To avoid conflicts and issues with different operating systems, all file names are slugified (all lower case, symbols stripped, and spaces replaced by dashes). This is a familiar convention for those used to jekyll, hugo, or other blogging systems.

- **Organization** Files are generated in two roots: `compendium` and `rules`. The location of these roots is configurable (see below). The following directories may be created in the `compendium` directory depending on what sources you have enabled: `backgrounds`, `bestiary` (with contents organized by monster type), `classes` (separate documents for classes and subclasses), `deities`, `feats`, `items`, `names`, `races`, and `spells`.

- **Styles** 
  - `css-snippets` has some snippets you can use to customize elements of the compendium.
  - Every document has a `cssclass` attribute that you can use to further tweak how page elements render: `json5e-background`, `json5e-deity`, `json5e-monster`, `json5e-class`, `json5e-feat`, `json5e-item`, `json5e-names`, `json5e-race`, and `json5e-spell`.

## To run without building yourself

1. Install JBang: https://www.jbang.dev/documentation/guide/latest/installation.html

2. Install the pre-built jar: 

    ```shell
    jbang app install --name 5e-convert --force --fresh https://jitpack.io/dev/ebullient/json5e-convert-cli/1.0.10/json5e-convert-cli-1.0.10-runner.jar
    ```

3. Run the command: 

    ```shell
    5e-convert --help
    ```

    > Feel free to use an alternate alias by replacing the value specified as the name: `--name 5e-convert`, and adjust the commands shown below accordingly.

## To build (optional)

1. Clone this repository
2. Build this project: `quarkus build` or `./mvnw install`
3. Verify the build: `java -jar target/json5e-convert-cli-199-SNAPSHOT-runner.jar --help`

To run commands listed below, either: 

- Replace `5e-convert` with `java -jar target/json5e-convert-cli-199-SNAPSHOT-runner.jar`, or
- Use JBang to create an alias that points to the built jar: 

    ```shell
    jbang app install --name 5e-convert --force --fresh ~/.m2/repository/dev/ebullient/json5e-convert-cli/199-SNAPSHOT/json5e-convert-cli-199-SNAPSHOT-runner.jar
    ```

    > Feel free to use an alternate alias by replacing the value specified as the name: `--name 5e-convert`, and adjust the commands shown below accordingly.

## Starting with 5eTools JSON data

1. Download a release of the 5e tools mirror, or create a shallow clone of the repo (which can/should be deleted afterwards):

    ```shell
    git clone --depth 1 https://github.com/5etools-mirror-1/5etools-mirror-1.github.io.git
    ```

2. Invoke the CLI. In this first example, let's generate indexes and use only SRD content:

    ```shell
    5e-convert \
      --index \
      -o dm \
      5etools-mirror-1.github.io/data
    ```

    - `--index` Create `all-index.json` containing all of the touched artifact ids, and `src-index.json` that shows the filtered/allowed artifact ids. These files are useful when tweaking exclude rules (as shown below).
    - `-o dm` The target output directory. Files will be created in this directory.

    The rest of the command-line specifies input files: 

    - `~/git/dnd/5etools-mirror-1.github.io/data` Path to the data directory containing 5etools files (a clone or release of the mirror repo)

3. Invoke the command again, this time including sources and custom items:

    ```shell
    5e-convert \
    --index \
    -o dm \
    -s PHB,DMG,SCAG \
    5etools-mirror-1.github.io/data \
    my-items.json dm-sources.json
    ```
    
    - `-s PHB,DMG,SCAG` Will include content from the Player's Handbook, the Dungeon Master's Guide, and the Sword Coast Adventurer's Guide, all of which I own. Source abbreviations are found in the [source code](https://github.com/ebullient/json5e-convert-cli/blob/55fe9139fe56a27b3148f8faa0834f3e34aa95ec/src/main/java/dev/ebullient/json5e/tools5e/CompendiumSources.java#L130).
    - `my-items.json` Custom items that I've created for my campaign that follow 5etools JSON format.
    - `dm-sources.json` Additional parameters (shown in detail below)

### Additional parameters

I use a json file to provide detailed configuration for sources, as doing so with command line arguments becomes tedious and error-prone. I use something like this:

```json
{
  "from": [
    "AI",
    "PHB",
    "DMG",
    "TCE",
    "LMoP",
    "ESK",
    "DIP",
    "XGE",
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

- `from` defines the array of sources that should be included. Only include content from sources you own. If you omit this parameter (and don't specify any other sources on the command line), this tool will only include content from the SRD.  

    - **Source abbreviations** are found in the [source code](https://github.com/ebullient/json5e-convert-cli/blob/1a2b43ac25324caffb253b377a04b2a463f61d57/src/main/java/dev/ebullient/json5e/tools5e/CompendiumSources.java#L138)

- `paths` allows you to redefine vault paths for cross-document links, and to link to documents defining conditions, and weapon/item properties. By default, items, spells, monsters, backgrounds, races, and classes are in `/compendium/`, while files defining conditions and weapon properties are in `/rules/`. You can reconfigure either of these path roots in this block: 

    ```json
    "paths": {
      "compendium": "/compendium/",
      "rules": "/rules/"
    },
    ```
    > Note: the leading slash indicates the path starting at the root of your vault.

- `exclude`, and `excludePattern` work against the identifiers (listed in the generated index files). They allow you to further tweak/constrain what is emitted as formatted markdown. In the above example, I'm excluding all of the race variants from the DMG, and the monster-form of the expert sidekick from the Essentials Kit. I own both of these books, but I don't want those creatures in the formatted bestiary.

For example, to generate player-focused reference content for a Wild Beyond the Witchlight campaign, I've constrained things further: I am pulling from a much smaller set of sources. I included Elemental Evil Player's Companion (Genasi) and Volo's Guide to Monsters (Tabaxi), but then added exclude patterns to remove elements from these sourcebooks that I don't want my players to use in this campaign (some simplification for beginners). My JSON looks like this:

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

> Not everything is customizable. In some cases, formatting heading and text accurately is easier to do inline as a big blob. The example templates show what is available to tweak.

### Template examples

- [Default templates](https://github.com/ebullient/json5e-convert-cli/tree/main/src/main/resources/templates)

Of particular note are the varied monster templates: 

- Admonition codeblock: https://github.com/ebullient/json5e-convert-cli/blob/main/src/main/resources/templates/monster2md.txt
- TTRPG statblock in the body: https://github.com/ebullient/json5e-convert-cli/blob/main/src/main/resources/templates/monster2md-yamlStatblock-body.txt
- Admonition codeblock in the body with minimal TTRPG/Initiative tracker YAML metadata in the header: https://github.com/ebullient/json5e-convert-cli/blob/main/src/main/resources/templates/monster2md-yamlStatblock-header.txt 

