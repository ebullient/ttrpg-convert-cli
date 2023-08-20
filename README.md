# TTRPG convert CLI
![GitHub all releases](https://img.shields.io/github/downloads/ebullient/ttrpg-convert-cli/total?color=success)

The tool formally known as Json 5e convert CLI.

CLI to convert 5eTools (🚧 and Pf2eTools) JSON into crosslinked, tagged, and formatted (with templates) markdown for use with [Obsidian.md](https://obsidian.md).

The instructions below still reference the released `ttrpg-convert` CLI. New stuff is on the way!

<table><tr>
<td>Jump</td>
<td><a href="##install-the-command-line-utility">⬇ Download</a></td>
<td><a href="#build-and-run-optional">⚙️ Build</a></td>
<td><a href="#conventions">📝 Conventions</a></td>
</tr><tr>
<td><a href="#recommended-plugins">🔌 Plugins</a></td>
<td><a href="#use-with-5etools-json-data">📖 5etools Data</a></td>
<td><a href="#templates">🎨 Templates</a></td>
<td><a href="#changes-that-impact-generated-templates-and-files">🚜 Changelog</a></td>
</tr></table>

I use [Obsidian](https://obsidian.md) to keep track of my campaign notes. This project parses json sources for materials that I own from the 5etools mirror to create linked and formatted markdown that I can reference in my notes.

> 🚜 Check out [Changes that impact generated templates and files](#changes-that-impact-generated-templates-and-files) and/or release notes for breaking changes and new capabilities. 

## Install the command line utility

There are several options for running `ttrpg-convert`. Choose whichever you are the most comfortable with: 

- [Use pre-built platform binary](#use-pre-built-platform-binary) (no Java required)
- [Use JBang](#use-jbang)
- [Use Java to run the jar](#use-java-to-run-the-jar)
- [Build from source](#build-and-run)
- **Using Windows?** See the [Windows README](README-WINDOWS.md)

### Use pre-built platform binary

[Download the latest release](https://github.com/ebullient/ttrpg-convert-cli/releases/latest) of the zip or tgz for your platform. Extract the archive. A `ttrpg-convert` binary executable will be in the extracted bin directory. 

```shell
ttrpg-convert --help
```

Use this binary in the instructions below. Continue to notes about [Conventions](#conventions).

Notes:

- [Open a command prompt in a folder (Windows) ](https://www.lifewire.com/open-command-prompt-in-a-folder-5185505)
- [Running executables from the command line (Windows)](https://www.techwalla.com/articles/how-to-use-quotcdquot-command-in-command-prompt-window)
- To show emoji in Windows Commmand Prompt: `chcp 65001` and choose a font with emoji support (Consolas is one). You can also try the new Windows Terminal (`wt.exe`).
- MacOS permission checking (unverified executable): `xattr -r -d com.apple.quarantine <path/to>/ttrpg-convert`

### Use JBang

1. Install JBang: https://www.jbang.dev/documentation/guide/latest/installation.html

2. Install the pre-built release: 

    ```shell
    jbang app install --name ttrpg-convert --force --fresh https://github.com/ebullient/ttrpg-convert-cli/releases/download/2.1.0/ttrpg-convert-cli-2.1.0-runner.jar
    ```

    If you want the latest _unreleased snapshot_ (may not match this doc!): 

    ```shell
    jbang app install --name ttrpg-convert --force --fresh https://jitpack.io/dev/ebullient/ttrpg-convert-cli/299-SNAPSHOT/ttrpg-convert-cli-299-SNAPSHOT-runner.jar
    ```

    There may be a pause if you download the snapshot; it is rebuilt on demand.

    > 🔹 Feel free to use an alternate alias by replacing the value specified as the name: `--name ttrpg-convert`, and adjust the commands shown below accordingly.

3. Verify the install by running the command: 

    ```shell
    ttrpg-convert --help
    ```

Continue to notes about [Conventions](#conventions).

### Use Java to run the jar

1. Install Java 17: https://adoptium.net/, or with your favorite package manager.

2. Download the latest [ttrpg-convert-cli jar](https://github.com/ebullient/ttrpg-convert-cli/releases/download/2.1.0/ttrpg-convert-cli-2.1.0-runner.jar):  

3. Verify the install by running the command: 

    ```shell
    java -jar ttrpg-convert-cli-2.1.0-runner.jar --help
    ```

To run commands listed below, replace `ttrpg-convert` with `java -jar ttrpg-convert-cli-2.1.0-runner.jar`

Continue to notes about [Conventions](#conventions).

### Build and run

1. Clone this repository
2. Ensure you have [Java installed on your system](https://adoptium.net/installation/) and active in your path.
3. Build this project: `quarkus build` or `./mvnw install`
4. Verify the build: `java -jar target/ttrpg-convert-cli-299-SNAPSHOT-runner.jar --help`

To run commands listed below, either: 

- Replace `ttrpg-convert` with `java -jar target/ttrpg-convert-cli-299-SNAPSHOT-runner.jar`, or
- Use JBang to create an alias that points to the built jar: 

    ```shell
    jbang app install --name ttrpg-convert --force --fresh ~/.m2/repository/dev/ebullient/ttrpg-convert-cli/299-SNAPSHOT/ttrpg-convert-cli-299-SNAPSHOT-runner.jar
    ```

    > 🔹 Feel free to use an alternate alias by replacing the value specified as the name: `--name ttrpg-convert`, and adjust the commands shown below accordingly.

## Conventions

- **Links.** Documents generated by this plugin will use markdown links rather than wiki links. A css snippet can make these links less invasive in edit mode by hiding the URL portion of the string.

- **File names.** To avoid conflicts and issues with different operating systems, all file names are slugified (all lower case, symbols stripped, and spaces replaced by dashes). This is a familiar convention for those used to jekyll, hugo, or other blogging systems.

  File names for resources outside of the core books (PHB, MM, and DMG) have the abbreviated source name appended to the end to avoid file collisions.

- **Organization.** Files are generated in two roots: `compendium` and `rules`. The location of these roots is configurable (see below).   

  The following directories may be created in the `compendium` directory depending on what sources you have enabled: `backgrounds`, `bestiary` (with contents organized by monster type), `classes` (separate documents for classes and subclasses), `deities`, `feats`, `items`, `names`, `races`, and `spells`.

- **Styles.** Every document has a `cssclass` attribute that you can use to further tweak how page elements render. `css-snippets` has some snippets you can use to customize elements of the compendium. 
  - 5e tools: `json5e-background`, `json5e-class`, `json5e-deity`, `json5e-feat`, `json5e-item`, `json5e-monster`, `json5e-names`, `json5e-note`, `json5e-race`, and `json5e-spell`.
  - pf2e tools: `pf2e`, `pf2e-ability`, `pf2e-action`, `pf2e-affliction`, `pf2e-archetype`, `pf2e-background`, `pf2e-book`, `pf2e-delity`, `pf2e-feat`, `pf2e-hazard`, `pf2e-index`, `pf2e-item`, `pf2e-note`, `pf2e-ritual`, `pf2e-sleep`, `pf2e-trait`, 

- **Admonitions.** 
  - `ad-statblock`
  - 5e tools: `ad-flowchart`, `ad-gallery`
  - pf2e tools: `ad-embed-ability`, `ad-embed-action`, `ad-embed-affliction`, `ad-embed-avatar`, `ad-embed-disease`, `ad-embed-feat`, `ad-embed-item`, `ad-pf2-note`, `ad-pf2-ritual`.


## Recommended plugins 

- **[Admonitions](obsidian://show-plugin?id=obsidian-admonition)**: The admonitions plugin is used to render the statblocks and other admonitions. It also supports a codeblock style that is used for more complicated content like statblocks, where callout syntax would be difficult to manage. 

    Import one or more admonition json files in the [examples](https://github.com/ebullient/ttrpg-convert-cli/tree/main/examples) directory to create the custom admonition types used for converted content:

    - [admonitions-5e.json](https://raw.githubusercontent.com/ebullient/ttrpg-convert-cli/main/examples/admonitions-5e.json) for 5e tools
    - [admonitions-pf2e.json](https://raw.githubusercontent.com/ebullient/ttrpg-convert-cli/main/examples/admonitions-pf2e.json) for pf2e tools
    - [other-admonitions.json](https://raw.githubusercontent.com/ebullient/ttrpg-convert-cli/main/examples/other-admonitions.json) if they are interesting

- **[Force note view mode by front matter](obsidian://show-plugin?id=obsidian-view-mode-by-frontmatter)**: I use this plugin to treat these generated notes as essentially read-only.  

    Ensure the plugin has the following options enabled: 

    - "Ignore force view when not in front matter": the plugin will only change the view mode if `obsidianUIMode` is defined in the front matter.    
    - "Ignore open files": the plugin won't try to change the view mode if the file is already open.

- **[TTRPG Statblocks](obsidian://show-plugin?id=obsidian-5e-statblocks)**: Templates for rendering monsters can define a `statblock` in the document body or provide a full or abridged yaml monster in the document header to update monsters in the plugin's bestiary. See [Templates](#templates) for more information.

- **[Initiative Tracker](obsidian://show-plugin?id=initiative-tracker)**: Templates for rendering monsters can include information in the header to define monsters that initiative tracker can use when constructing encounters.

### Optional CSS Snippets

Within the `examples/css-snippets` folder, you will find some CSS snippets that have been created to further customize the look of the generated content. They include:
- Functionality to float token images to side of a statblock.
- Further enhancement of the admonition styles.
- _PF2 Only_: More realistic looking Statblocks
- _PF2 Only_: Link Test to display action icons.
- _PF2 Only_: Light styling of pages as defined through css-classes.
- And much more.

#### Statblocks

Compendium (`*-compendium`) snippets include styles for statblocks.

If you aren't using a `*-compendium` snippet, you may want to download either `dnd5e-only-statblocks.css` or `pf2-only-statblocks.css` to style your statblocks.

> :warning: Do not use an `*-only-statblock.css` snippet and a `*-compendium.css` snippet together.


## Use with 5eTools JSON data

1. Create a shallow clone of the 5eTools mirror repo (which can/should be deleted afterwards):

    ```shell
    git clone --depth 1 https://github.com/5etools-mirror-1/5etools-mirror-1.github.io.git
    ```

2. Invoke the CLI. In this first example, let's generate indexes and markdown for SRD content:

    ```shell
    ttrpg-convert \
      --index \
      -o dm \
      5etools-mirror-1.github.io/data
    ```

    - `--index` Create `all-index.json` containing all of the touched artifact ids, and `src-index.json` that shows the filtered/allowed artifact ids. These files are useful when tweaking exclude rules (as shown below).
    - `-o dm` The target output directory. Files will be created in this directory.

    The rest of the command-line specifies input files:

    - `5etools-mirror-1.github.io/data` Path to the 5etools `data` directory (from a clone or release of the repo)

    This should produce a set of markdown files in the `dm` directory.

3. Invoke the command again, this time including sources and custom items:

    ```shell
    ttrpg-convert \
        --index \
        -o dm \
        -s PHB,DMG,SCAG \
        -c dm-sources.json \
        5etools-mirror-1.github.io/data \
        5etools-mirror-1.github.io/data/adventure/adventure-lox.json \
        5etools-mirror-1.github.io/data/book/book-aag.json \
        my-items.json 
    ```

    - `-s PHB,DMG,SCAG` Will include content from the Player's Handbook, the Dungeon Master's Guide, and the Sword Coast Adventurer's Guide (all sources I own).  
        > 🔸 **Source abbreviations** are found in the [source code (around line 138)](https://github.com/ebullient/ttrpg-convert-cli/blob/main/examples/config/sourceMap.md). Only use sources you own.  

    - `-c dm-sources.json` contains configuration parameters (shown in detail [below](#additional-parameters))
    - Books (`/book/book-aag.json`) and adventures (`/adventure/adventure-lox.json`) to include as well.
    - `my-items.json` defines custom items for my campaign that follow 5etools JSON format.

> 💭 I recommend running the CLI against a separate directory, and then using a comparison tool of your choice to preview changes before you copy or merge them in.
>
> You can use `git diff` to compare arbitrary directories:
> ```
> git diff --no-index vault/compendium/bestiary generated/compendium/bestiary
> ```

## Use with Pf2eTools Data

🚜 🚧 🚜 🚧 🚜 🚧 🚜 🚧

1. Download a release of the Pf2eTools mirror, or create a shallow clone of the repo (which can/should be deleted afterwards):

    ```shell
    git clone --depth 1 https://github.com/Pf2eToolsOrg/Pf2eTools.git
    ```

2. Invoke the CLI. In this first example, let's generate indexes and use only SRD content (using the alias set up when [installing the cli](#install-the-command-line-utility)):

    ```shell
    ttrpg-convert \
      -g pf2e \
      --index \
      -o dm \
      Pf2eTools/data
    ```

    - `-g p2fe` The game system! Pathfinder 2e!
    - `--index` Create `all-index.json` containing all of the touched artifact ids, and `src-index.json` that shows the filtered/allowed artifact ids. These files are useful when tweaking exclude rules (as shown below).
    - `-o dm` The target output directory. Files will be created in this directory.

    The rest of the command-line specifies input files: 

    - `Pf2eTools/data` Path to the Pf2eTools `data` directory (from a clone or release of the repo)


## Additional parameters

Configuration can also be provided as a JSON or YAML file instead of using command line parameters. See [examples/config](examples/config) for the general config file structure.

I use something like this:

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

    > 🔸 **Source abbreviations** are found in the [source code (around line 138)](https://github.com/ebullient/ttrpg-convert-cli/blob/main/examples/config/sourceMap.md). Only use sources you own.

- `paths` allows you to redefine vault paths for cross-document links, and to link to documents defining conditions, and weapon/item properties. By default, items, spells, monsters, backgrounds, races, and classes are in `/compendium/`, while files defining conditions and weapon properties are in `/rules/`. You can reconfigure either of these path roots in this block: 

    ```json
    "paths": {
      "compendium": "/compendium/",
      "rules": "/rules/"
    },
    ```
    > 🔹 Note: the leading slash indicates the path starting at the root of your vault.

- `exclude` and `excludePattern`: Exclude a single identifier (as listed in the generated index files), or all identifiers matching a pattern. In the above example, I'm excluding all of the race variants from the DMG, and the monster-form of the expert sidekick from the Essentials Kit. As it happens, I own these materials, but I don't want these variants in the formatted bestiary.

- `include` (as of 1.0.13): Include a single identifier (as listed in the generated index files). 
This allows you to include a specific resource without including the whole source and excluding everything else. Useful for single resources (classes, backgrounds, races, items, etc.) purchased from D&D Beyond. To include the Changeling race from _Mordenkainen Presents: Monsters of the Multiverse_, for example, you would add the folowing: 

    ```json
    "include": [
        "race|changeling|mpmm"
    ]
    ```

- `convert` (as of 1.0.18): specify books or adventures to import into the compendium (which will allow cross-linking, etc.). Either provide the full relative path to the adventure or book json file, or specify its Id (as found in the [source code](https://github.com/ebullient/ttrpg-convert-cli/blob/main/examples/config/sourceMap.md)): 

    ```json
    "convert": {
        "adventure": [
            "WBtW",
            "tftyp-wpm", 
        ],
        "book": [
            "5etools-mirror-1.github.io/data/book/book-phb.json"
        ]
    }
    ```

Note that some adventures, like _Tales from the Yawning Portal_, are treated as a collection of standalone modules. The generated index contains these as either `adventure` or `book` items. If you're unsure, check the generated index file (`allIndex.json`): _Tales from the Yawning Portal: The Forge of Fury_ is an adventure (`adventure|adventure-tftyp-tfof`). _Acquisitions Incorporated_ is a book (`book|book-ai`). 

### Additional example

To generate player-focused reference content for a Wild Beyond the Witchlight campaign, I constrained things further. I am pulling from a smaller set of sources. I included Elemental Evil Player's Companion (Genasi) and Volo's Guide to Monsters (Tabaxi), but also used `exclude` and `excludePattern` to remove elements from these sourcebooks that I don't want my players to use in this campaign (some simplification for beginners). 

The JSON looks like this:

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
```

## Templates

This application uses the [Qute Templating Engine](https://quarkus.io/guides/qute). You can make simple customizations to markdown output by copying a template from `src/main/resources/templates`, making the desired modifications, and then specifying that template on the command line.

```
ttrpg-convert 5etools \
  --background examples/templates/tools5e/images-background2md.txt \
  --index -o dm dm-sources.json ~/git/dnd/5etools-mirror-1.github.io/data my-items.json
```

Additional templates can also be specified in your configuration file: 

```json
  "template": {
    "background": "examples/templates/tools5e/images-background2md.txt",
    "monster": "examples/templates/tools5e/monster2md-scores.txt"
  }
```

The flag used to specify a template (either on the command line or in a config file) corresponds to the type of template being used. In general, take the file name of a [default templates](https://github.com/ebullient/ttrpg-convert-cli/tree/main/src/main/resources/templates) and remove the `2md.txt` suffix.

- Valid keys for 5etools: `background`, `class`, `deity`, `feat`, `hazard`, `item`, `monster`, `note`, `race`, `reward`, `spell`, `subclass`.
- Valid keys for Pf2eTools: `ability`, `action`, `affliction`, `archetype`, `background`, `book`, `deity`, `feat`, `hazard`, `inline-ability`, `inline-affliction`, `inline-attack`, `item`, `note`, `ritual`, `spell`, `trait`.

### Built-in / example templates

Not everything is customizable. In some cases, indenting, organizing, formatting, and linking text accurately is easier to do inline as a big blob. 

[Documentation](https://github.com/ebullient/ttrpg-convert-cli/tree/main/docs) is generated for template-accessible attributes.

- [Default templates](https://github.com/ebullient/ttrpg-convert-cli/tree/main/src/main/resources/templates)
- [Example templates](https://github.com/ebullient/ttrpg-convert-cli/tree/main/examples/templates)

Of particular note are the varied monster templates: 

- Admonition codeblock: [monster2md.txt](https://github.com/ebullient/ttrpg-convert-cli/tree/main/src/main/resources/templates/tools5e/monster2md.txt)
- Admonition codeblock with alternate score layout: [monster2md-scores.txt](https://github.com/ebullient/ttrpg-convert-cli/tree/main/examples/templates/tools5e/monster2md-scores.txt)
- TTRPG statblock in the body: [monster2md-yamlStatblock-body.txt](https://github.com/ebullient/ttrpg-convert-cli/tree/main/examples/templates/tools5e/monster2md-yamlStatblock-body.txt)
- Admonition codeblock in the body with minimal TTRPG/Initiative tracker YAML metadata in the header: [monster2md-yamlStatblock-header.txt](https://github.com/ebullient/ttrpg-convert-cli/tree/main/examples/templates/tools5e/monster2md-yamlStatblock-header.txt)

## Changes that impact generated templates and files

### 🔥 2.1.0: File name and path changes, template docs and attribute changes

1. 🔥 **Variant rules include the source in the file name**: this avoids duplicates (and there were some).
2. 🔥 **5eTools changed the classification for some creatures**, which moves them in the bestiary. Specifically: the Four-armed troll is a giant (and not an npc), a river serpent is a monstrosity rather than a beast, and ogre skeletons and red dracoliches are both undead.
3. 🔥 Better support for table rendering has superceded dedicated/hand-tended random name tables. All of the tables are still present, just in different units more directly related to source material.
4. 🔥 **Change to monster template attributes:** Legendary group attributes have been simplified to `name` and `desc`, just like other traits. See the [default monster template](https://github.com/ebullient/ttrpg-convert-cli/blob/0736c3929a6d90fe01860692f487b8523b57e60d/src/main/resources/templates/tools5e/monster2md.txt#L80) for an example.

> ***If you use the Templater plugin***, you can use [a templater script](https://raw.githubusercontent.com/ebullient/ttrpg-convert-cli/main/migration/ttrpg-cli-renameFiles-5e-2.1.0.md) to **rename files in your vault before merging** with freshly generated content. View the contents of the template before running it, and adjust parameters at the top to match your Vault.

✨ **New template documentation** is available in [docs](https://github.com/ebullient/ttrpg-convert-cli/tree/main/docs). Content is generated from javadoc in the various *.qute packages (for template-accessible fields and methods). It may not be complete.. PRs to improve it are welcome.


### 🔥 2.0.0: File name and path changes, and styles!

1. 🔥 **A leading slash (`/`) is no longer used at the beginning of compendium and root paths**. This should allow you to move these two directories around more easily. 
    - I recommend that you keep the compendium and rules sections together as big balls of mud.
    - If you do want to further move files around, do so from within obsidian, so obsidian can update its links.

2. 🔥 **D&D 5e subclasses now use the source of the subclass in the file name**.

   > ***If you use the Templater plugin***, you can use [a templater script](https://raw.githubusercontent.com/ebullient/ttrpg-convert-cli/main/migration/ttrpg-cli-renameFiles-1.3.0.md) to rename files in your vault before merging with freshly generated content. View the contents of the template before running it, and adjust parameters at the top to match your Vault.

3. 🎨 CSS styles for D&D 5e and Pathfinder are now available in `examples/css-snippets`. 

4. 📝 Admonitions are also available for import:
    - 🎨 [admonitions-5e.json](https://raw.githubusercontent.com/ebullient/ttrpg-convert-cli/main/examples/admonitions-5e.json)
    - [admonitions-pf2e-v3.json](https://raw.githubusercontent.com/ebullient/ttrpg-convert-cli/main/examples/admonitions-pf2e-v3.json)
    - 🎨 [other-admonitions.json](https://raw.githubusercontent.com/ebullient/ttrpg-convert-cli/main/examples/other-admonitions.json)

    Note: `admonitions-5e.json` and `other-admonitions.json` use colors from CSS snippets to adjust for light and dark mode.

### 🔖 1.1.1: Dice roller in statblocks and text

If you are using the default templates and want to render dice rolls, set
`useDiceRoller` to true to use dice roller strings when replacing dice `{@dice
}`, and `{@damage }` strings. This can be set differently for either "5e" or
"pf2e" configurations. Please note that if you are using a custom template and fantasy statblocks, you do **not** need to set the dice roller in your config. Fantasy statblocks will take care of the rendering itself. 

See [examples/config](examples/config) for the general structure of config.

### 🔖 1.1.0: Images for backgrounds, items, monsters, races, and spells

The conversion tool downloads fluff images into `img` directories within each type, e.g. `backgrounds/img` or `bestiary/aberration/img`. These images are unordered, and are not referenced in entry text. Templates must be modified to include them.

To display all images, you can do something like this: 

```
{#each resource.fluffImages}![{it.caption}]({it.path})  
{/each}
```

Note that the line above ends with two spaces, which serves as a line break when you have strict line endings enabled. You may need something a little different to get things to wrap the way you want in the case that there are multiple images (which is infrequent for these types).

You can also use two separate blocks, such that the first image is used at the top of the document, and any others are included later: 

```
{#if resource.fluffImages && resource.fluffImages.size > 0 }{#let first=resource.fluffImages.get(0)}
![{first.title}]({first.vaultPath}#right)  
{/let}{/if}
...

{#each resource.fluffImages}{#if it_index != 0}![{it.caption}]({it.path}#center)  
{/if}{/each}
```

Notice the `#right` and `#center` anchor tags in the example above. The following CSS snippet defines formatting for two anchor tags: `#right` (which will float the image to the right) and `#center` (which will display the image as a centered block). 

```css
.json5e-background div[src$="#center"],
.json5e-item div[src$="#center"],
.json5e-monster div[src$="#center"],
.json5e-race div[src$="#center"],
.json5e-spell div[src$="#center"] {
  text-align: center;
}
.json5e-background div[src$="#center"] img,
.json5e-item div[src$="#center"] img,
.json5e-monster div[src$="#center"] img,
.json5e-race div[src$="#center"] img,
.json5e-spell div[src$="#center"] img {
  height: 300px;
}
.json5e-background div[src$="#right"],
.json5e-item div[src$="#right"],
.json5e-monster div[src$="#right"],
.json5e-race div[src$="#right"],
.json5e-spell div[src$="#right"] {
  float: right;
  margin-left: 5px;
}
.json5e-background div[src$="#right"] img,
.json5e-item div[src$="#right"] img,
.json5e-monster div[src$="#right"] img,
.json5e-race div[src$="#right"] img,
.json5e-spell div[src$="#right"] img {
  height: 300px;
}

.rendered-widget .admonition-statblock-parent,
.markdown-rendered .admonition-statblock-parent,
.markdown-preview-section .admonition-statblock-parent {
  clear: both;
}
```

Notes: 

- I recommend constraining the image height (rather than the width) in your CSS snippet for images. 
- The above snippet also adds a `clear` setting to the admonition parent. Some text descriptions are shorter than the constrained image height. Setting `clear: both` on `admonition-parent` ensures that images floated to the right do not impact the `statblock` display.
- This configuration is in the [compendium.css snippet](https://github.com/ebullient/ttrpg-convert-cli/tree/main/examples/css-snippets/compendium.css).
- There is an example for each type in the [example templates directory](https://github.com/ebullient/ttrpg-convert-cli/tree/main/examples/templates/tools5e/) directory. Relevant file names start with `images-`.


### 🔖  1.0.18: You can put more things in json input now!

Use `convert` to import source text for books and adventures that you own: 

```json
  "convert": {
    "adventure": [
      "WBtW"
    ],
    "book": [
      "PHB"
    ]
  }
```

Specify templates in json:

```json
  "template": {
    "background": "path/to/template.txt",
  }
```

Be careful of paths here. Relative paths will be resolved depending on where the command is run from. Absolute paths will be machine specific (most likely). Use forward slashes for path segments, even if you're working on windows. 

You can place this configuration one file or several, your choice. 

### 🔖  1.0.16: Sections in Spell text

Text for changes to spells at higher levels is added to spells a little differently depending on how complicated the spell is.

Some spells effectively have subsections. Create or Destroy Water, from the PHB, has one subsection describing how water is created, and another describing how it is destroyed. In many layouts, there is just a bit of bold text to visually highlight this information. I've opted to make these proper sections (with a heading) instead, because you can then embed/transclude just the variant you want into your notes where that is relevant.

If a spell has sections, then "At Higher Levels" will be added as an additional section. Otherwise, it will be appended with `**At Higher Levels.**` as leading eyecatcher text.

The [default spell template](https://github.com/ebullient/ttrpg-convert-cli/tree/main/src/main/resources/templates/tools5e/spell2md.txt) has also been amended. It will test for sections in the spell text, and if so, now inserts a `## Summary` header above the Classes/Sources information, to ensure that the penultimate section can be embedded cleanly.

### 🔖  1.0.15: Flowcharts, optfeature in text, styled rows

- `optfeature` text is rendered (Tortle package)
- `flowcharts` is rendered as a series of `flowchart` callouts  
    Use the admonition plugin to create a custom `flowchart` callout with an icon of your choice.
- The adventuring gear tables from the PHB have been corrected

### 🔖  1.0.14: Ability Scores

As shown in [monster2md-scores.txt](https://github.com/ebullient/ttrpg-convert-cli/tree/main/examples/templates/tools5e/monster2md-scores.txt), you can now access ability scores directly to achieve alternate layouts in templates, for example: 

```
- STR: {resource.scores.str} `dice: 1d20 {resource.scores.strMod}`
- DEX: {resource.scores.dex} `dice: 1d20 {resource.scores.dexMod}`
- CON: {resource.scores.con} `dice: 1d20 {resource.scores.conMod}`
- INT: {resource.scores.int} `dice: 1d20 {resource.scores.intMod}`
- WIS: {resource.scores.wis} `dice: 1d20 {resource.scores.wisMod}`
- CHA: {resource.scores.cha} `dice: 1d20 {resource.scores.chaMod}`
```

### 🔖  1.0.13: Item property tags are now sorted

Property tags on items are now sorted (not alphabetically) to stabilize their order in generated files. This should be a one-time bit of noise as you cross this release (using a version before to using some version after).

### 🔥 1.0.12: File name changes

Each file name will now contain an abbreviation of the primary source to avoid conflicts (for anything that does not come from phb, mm, dmg).

***If you use the Templater plugin***, you can use [a templater script](https://github.com/ebullient/ttrpg-convert-cli/blob/main/migration/json5e-cli-renameFiles-1.0.12.md) to rename files in your vault before merging with freshly generated content. View the contents of the template before running it, and adjust parameters at the top as necessary.

### 🔥 1.0.12: Deity symbols and Bestiary Tokens

Symbols and tokens have changed in structure. Custom templates will need a bit of adjustment.

For bestiary tokens:

```
{#if resource.token}
![{resource.token.caption}]({resource.token.path}#token){/if}
```

For deities:

```
{#if resource.image}
![{resource.image.caption}]({resource.image.path}#symbol){/if}
```

## Other notes

This project uses Quarkus, the Supersonic Subatomic Java Framework. If you want to learn more about Quarkus, please visit its website: https://quarkus.io/.

This project is a derivative of [fc5-convert-cli](https://github.com/ebullient/fc5-convert-cli), which focused on working to and from FightClub5 Compendium XML files. It has also stolen some bits and pieces from [pockets-cli](https://github.com/ebullient/pockets-cli).
