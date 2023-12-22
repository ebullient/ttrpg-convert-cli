# TTRPG convert CLI
![GitHub all releases](https://img.shields.io/github/downloads/ebullient/ttrpg-convert-cli/total?color=success)

A Command-Line Interface designed to convert TTRPG data from 5eTools and Pf2eTools into crosslinked, tagged, and formatted markdown optimized for [Obsidian.md](https://obsidian.md). 

<table><tr>
<td>Jump</td>
<td><a href="#install-the-ttrpg-convert-cli">⬇ Download</a></td>
<td><a href="#conventions">📝 Conventions</a></td>
<td><a href="#recommended-plugins">🔌 Plugins</a></td>
</tr><tr>
<td><a href="#use-with-5etools-json-data">📖 5eTools</a></td>
<td><a href="#use-with-pf2etools-json-data">📖 Pf2eTools</a></td>
<td><a href="#templates">🎨 Templates</a></td>
<td><a href="CHANGELOG.md">🚜 Changelog</a></td>
</tr></table>

I use [Obsidian](https://obsidian.md) to keep track of my campaign notes. This project parses json sources for materials that I own from the 5etools mirror to create linked and formatted markdown that I can reference in my notes.

> 🚜 Check out [Changes that impact generated templates and files](CHANGELOG.md) and/or release notes for breaking changes and new capabilities. 

## Using the Command Line

This tool works in the command line, which is a text-based way to give instructions to your computer. 
If you're new to it, we have resources to help you get started below.

If you don't have a favorite method already, or you don't know what those words mean, here are some resources to get you started:

- For MacOS / OSX Users:
    - Start with the built-in `Terminal` application.
    - [Learn the Mac OS X Command Line][]
- For Windows Users:
    - Use the native [Command Prompt][]
    - [Open a command prompt in a folder (Windows)][]
    - [Running executables from the command line (Windows)][]
    - To show emoji in Windows Commmand Prompt: `chcp 65001` and choose a font with emoji support (Consolas is one). 

[Learn the Mac OS X Command Line]: https://blog.teamtreehouse.com/introduction-to-the-mac-os-x-command-line
[Command Prompt]: https://www.digitaltrends.com/computing/how-to-use-command-prompt/
[Open a command prompt in a folder (Windows)]: https://www.lifewire.com/open-command-prompt-in-a-folder-5185505
[Running executables from the command line (Windows)]: https://www.techwalla.com/articles/how-to-use-quotcdquot-command-in-command-prompt-window

## Install the TTRPG Convert CLI

There are several options for running `ttrpg-convert`. Choose whichever you are the most comfortable with: 

- [Use a pre-built platform binary](#use-pre-built-platform-binary) (no Java required)
- [Use JBang](docs/alternateRun.md#use-jbang) (hides Java invocation; sets up command aliases)
- [Use Java to run the jar](docs/alternateRun.md#use-java-to-run-the-jar)
- [Build from source](docs/alternateRun.md#build-and-run-from-source)
- **Using Windows?** 
    - See the [Windows README](README-WINDOWS.md)
    - Obsidian TTRPG Tutorials: [TTRPG-Convert-CLI 5e][] or [TTRPG-Convert-CLI PF2e][]

[TTRPG-Convert-CLI 5e]: https://obsidianttrpgtutorials.com/Obsidian+TTRPG+Tutorials/Plugin+Tutorials/TTRPG-Convert-CLI/TTRPG-Convert-CLI+5e
[TTRPG-Convert-CLI PF2e]: https://obsidianttrpgtutorials.com/Obsidian+TTRPG+Tutorials/Plugin+Tutorials/TTRPG-Convert-CLI/TTRPG-Convert-CLI+PF2e

### Use pre-built platform binary

> 📝 Where do these binaries come from?  
> They are built on GitHub managed CI runners using the workflow defined [here](https://github.com/ebullient/ttrpg-convert-cli/blob/main/.github/workflows/release.yml), which compiles a Quarkus application (Java) into a platform-native binary using [GraalVM](https://www.graalvm.org/). I build and upload the mac arm64 binary myself (not supported by GH CI) using [this script](https://github.com/ebullient/ttrpg-convert-cli/blob/main/.github/augment-release.sh). 

[Download the latest release](https://github.com/ebullient/ttrpg-convert-cli/releases/latest) of the zip or tgz for your platform. Extract the archive. A `ttrpg-convert` binary executable will be in the extracted bin directory. 

In a terminal or command shell, navigate to the directory where you extracted the archive and run the command:

```shell
ttrpg-convert --help
```

We'll use this command in the instructions below.

Notes: 
- Folks familar with command line tools can add the `bin` directory to their path to make the command available from anywhere.
- _MacOS permission checking_ (unverified executable): `xattr -r -d com.apple.quarantine <path/to>/ttrpg-convert`

**Looking for a different method?** 

See [Other ways to run the CLI](docs/alternateRun.md) for more options to download and run the CLI.

## 🔮 Recommendations for using the CLI

- 🔐 Treat generated content as a big ball of mud. Stick it in a corner of your vault *treat it as read-only*.

    Trust us, you will want to regenerate content from time to time. It is cheap and easy to do if you don't have your own edits to worry about.

- 🔎 Have the CLI generate output into a separate directory and use a comparison tool to preview changes.  

    You can use `git diff` to compare arbitrary directories, for example:
    ```
    git diff --no-index vault/compendium/bestiary generated/compendium/bestiary
    ```

- 📑 Use a copy tool that only updates modified files, like [rsync][], to avoid unnecessary file copying when updating your vault. Use the checksum option (`-c`) to compare file contents; the file modification date is meaningless given generated files are recreated when the tool is run. We have some suggestions in [discussion #220][sync-discussion], but it is very much a work in progress.

[rsync]: https://stackoverflow.com/a/19540611
[sync-discussion]: https://github.com/ebullient/ttrpg-convert-cli/discussions/220

## Conventions

- **Links.** Documents generated by this plugin will use markdown links rather than wiki links. A [css snippet](https://raw.githubusercontent.com/ebullient/ttrpg-convert-cli/main/examples/css-snippets/hide-markdown-link-url.css) can make these links less invasive in edit mode by hiding the URL portion of the string.

- **File names.** To avoid conflicts and issues with different operating systems, all file names are slugified (all lower case, symbols stripped, and spaces replaced by dashes). This is a familiar convention for those used to jekyll, hugo, or other blogging systems.

  - File names for resources outside of the core books (PHB, MM, and DMG) have the abbreviated source name appended to the end to avoid file collisions.
  - All files have an `aliases` attribute that contains the original name of the resource.

- **Organization.** Files are generated in two roots: `compendium` and `rules`. The location of these roots is [configurable](docs/configuration.md#specify-target-paths-with-the-paths-key). These directories will be populated depending on the sources you have enabled. 

  - `compendium` contains files for items, spells, monsters, etc. 
    The `compendium` directory is further organized into subdirectories for each type of content. For example, all items are in the `compendium/items` directory.  

  - `rules` contains files for conditions, weapon properties, variant rules, etc. 
  
  - `css-snippets` will contain **CSS files for special fonts** used by some content. You will need to copy these snippets into your vault (`.obsidian/snippets`) and enable them (`Appearance -> Snippets`) to ensure all content in your vault is styled correctly. 

- **Styles.** Every document has a `cssclasses` attribute that assigns a CSS class. We have some [CSS snippets](docs/README.md#optional-css-snippets) that you can use to customize elements of the compendium. 
  - 5e tools: `json5e-background`, `json5e-class`, `json5e-deck`, `json5e-deity`, `json5e-feat`, `json5e-hazard`, `json5e-item`, `json5e-monster`, `json5e-note`, `json5e-object`, `json5e-psionic`, `json5e-race`, `json5e-reward`, `json5e-spell`, and `json5e-vehicle`.
  - pf2e tools: `pf2e`, `pf2e-ability`, `pf2e-action`, `pf2e-affliction`, `pf2e-archetype`, `pf2e-background`, `pf2e-book`, `pf2e-delity`, `pf2e-feat`, `pf2e-hazard`, `pf2e-index`, `pf2e-item`, `pf2e-note`, `pf2e-ritual`, `pf2e-sleep`, `pf2e-trait`, 

- **Admonitions.** 
  - `ad-statblock`
  - 5e tools: `ad-flowchart`, `ad-gallery`, `ad-embed-action`, `ad-embed-feat`, `ad-embed-monster`, `ad-embed-object`, `ad-embed-race`, `ad-embed-spell`, `ad-embed-table`
  - pf2e tools: `ad-embed-ability`, `ad-embed-action`, `ad-embed-affliction`, `ad-embed-avatar`, `ad-embed-disease`, `ad-embed-feat`, `ad-embed-item`, `ad-pf2-note`, `ad-pf2-ritual`.

## Recommended plugins 

- **[Admonitions](obsidian://show-plugin?id=obsidian-admonition)**: The admonitions plugin supports a codeblock style that is used for more complicated content like statblocks, where callout syntax would be difficult to manage. See [Admonition plugin notes](docs/README.md#admonitions) for more information.

- **[Force note view mode by front matter](obsidian://show-plugin?id=obsidian-view-mode-by-frontmatter)**: I use this plugin to treat these generated notes as essentially read-only. See [Force note view mode plugin notes](docs/README.md#force-note-view-mode-by-front-matter) for more information.

- **[TTRPG Statblocks](obsidian://show-plugin?id=obsidian-5e-statblocks)**: Templates for rendering monsters can define a `statblock` in the document body or provide a full or abridged yaml monster in the document header to update monsters in the plugin's bestiary. See [Templates](#templates) for more information.

- **[Initiative Tracker](obsidian://show-plugin?id=initiative-tracker)**: Templates for rendering monsters can include information in the header to define monsters that initiative tracker can use when constructing encounters.

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
    - `-o dm` The target output directory (`dm` in this case). Files will be created in this directory.

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
        > 🚀 Note: Only use content you own. Find the abbreviations for your sources in the [Source Map](https://github.com/ebullient/ttrpg-convert-cli/blob/main/docs/sourceMap.md).  

    - `-c dm-sources.json` contains configuration parameters (shown in detail [below](#using-a-configuration-file))
    - Books (`/book/book-aag.json`) and adventures (`/adventure/adventure-lox.json`) to include as well.
    - `my-items.json` defines custom items for my campaign that follow 5etools JSON format.

## Use with Pf2eTools Data

🚜 🚧 🚜 🚧 🚜 🚧 🚜 🚧

1. Download a release of the Pf2eTools mirror, or create a shallow clone of the repo (which can/should be deleted afterwards):

    ```shell
    git clone --depth 1 https://github.com/Pf2eToolsOrg/Pf2eTools.git
    ```

2. Invoke the CLI. In this first example, let's generate indexes and use only SRD content (using the alias set up when [installing the cli](#install-the-ttrpg-convert-cli)):

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


## Use with Homebrew 

The CLI tool also has the ability to import homebrewed content, though this content must still fit the json standards that are set by 5e or PF2e. 

Perhaps the simplest thing to do to import homebrew is to use already existing homebrew data from the 5etools homebrew github repo: https://github.com/TheGiddyLimit/homebrew. 

NOTE: You *do not* need to download the entire homebrew repo to use something from it with the CLI tool. *You only need the particular file you wish to import*. In this aspect homebrew data is different from the 5etools 'official' repo, which has data spread out among a variety of files.

So, for example, if you wanted to use Benjamin Huffman's popular homebrewed [Pugilist class](https://www.dmsguild.com/product/184921/The-Pugilist-Class) (and if so please make sure you have supported Benjamin by purchasing the content!), you would need to have a local copy of the [Pugilist json file](https://github.com/TheGiddyLimit/homebrew/blob/master/class/Benjamin%20Huffman%3B%20Pugilist.json). You would then run the command like so: 

``` shell
ttrpg-convert  --index -s 'SterlingVermin' -o hb-compendium '/path/to/Benjamin Huffman; Pugilist.json' 
```

Note that you need to include both the path to the json file *and* the source name, which you can get from the `json` field, at the top of the file under `_meta` and `sources`. The process is similar for other homebrew, including your own, so long as it is broadly compatible with the [5e-tools json spec](https://wiki.tercept.net/en/Homebrew/FromZeroToHero). If you use the CLI tool with a config, then you would (staying with our example from above) put `SterlingVermin` in the `from` field of the config and add the path to the pugilist json file to your build command. 

## Using A Configuration File 

So far we've seen use of the tool using command line parameters. But this can get very clunky when trying to do any sort of complex command, plus it is a lot of typing! 

You can configure the CLI with a JSON or YAML file, either instead of, or along with, command line parameters. See [examples/config](https://github.com/ebullient/ttrpg-convert-cli/tree/main/examples/config) for the general config file structure in both formats. We'll use JSON as the preferred format, but you could also use YAML instead. It is your choice.

> 📝 JSON and YAML are both file formats for storing data in useful and human-readable ways.
> - If you want to know why the `{}` and `[]` are used in the ways that they are you can read about json *objects* and *arrays* [here](https://www.toolsqa.com/rest-assured/what-is-json/)).
> - YAML (Yet another markup language) has a [specification](https://yaml.org/spec/1.2/spec.html) that describes how it should be used.

Here is an example of a `config.json` file. As a JSON Object, it has a `"key": "value"` structure.

``` json
{
  "from": [
    "DMG",
    "PHB",
    "MM"
  ],
  "paths": {
    "compendium": "z_compendium/",
    "rules": "z_compendium/rules"
  }
}
```

This example performs two basic functions:

1. **Filter Input Sources:** Using the key `from`, we list the sources (that we own) that we want to include.
2. **Target file path:** Using the key `path`, we specify the target path for writing generated `compendium` and `rules` content. These directories will be created if they don't exist, and are relative to the output directory specified on the command line with `-o`.

**For additional information on configuration**, see [Configuration](docs/configuration.md).

## Templates

This application uses the [Qute Templating Engine](https://quarkus.io/guides/qute). You can make simple customizations to markdown output by copying a template from `src/main/resources/templates`, making the desired modifications, and specifying that template in your configuration: 

```json
  "template": {
    "background": "examples/templates/tools5e/images-background2md.txt",
    "monster": "examples/templates/tools5e/monster2md-scores.txt"
  }
```

You would include this in the `config.json` as a base level key-value pair, and the entire file might look something like this: 

``` json
{
  "from": [
    "DMG",
    "PHB",
    "MM"
  ],
  "paths": {
    "compendium": "z_compendium/",
    "rules": "z_compendium/rules"
  },
  "template": {
    "background": "examples/templates/tools5e/images-background2md.txt",
    "monster": "examples/templates/tools5e/monster2md-scores.txt"
  }
}
```

The flag used to specify a template corresponds to the type of template being used. You can find the list of valid template keys in the [source code](https://github.com/ebullient/ttrpg-convert-cli/blob/main/src/main/resources/convertData.json) (look for `templateKeys`).

- Valid template keys for 5etools: `background`, `class`, `deck`, `deity`, `feat`, `hazard`, `item`, `monster`, `note`, `object`, `psionic`, `race`, `reward`, `spell`, `subclass`, `vehicle`.
- Valid template keys for Pf2eTools: `ability`, `action`, `affliction`, `archetype`, `background`, `book`, `deity`, `feat`, `hazard`, `inline-ability`, `inline-affliction`, `inline-attack`, `item`, `note`, `ritual`, `spell`, `trait`.

### Customizing templates

Not everything is customizable. In some cases, indenting, organizing, formatting, and linking text accurately is easier to do inline as a big blob. 

[Documentation](docs/templates/) is generated for template-accessible attributes.

- [Default templates](https://github.com/ebullient/ttrpg-convert-cli/tree/main/src/main/resources/templates)
- [Example templates](https://github.com/ebullient/ttrpg-convert-cli/tree/main/examples/templates)

Of particular note are the varied monster templates: 

- Admonition codeblock: [monster2md.txt](https://github.com/ebullient/ttrpg-convert-cli/tree/main/src/main/resources/templates/tools5e/monster2md.txt)
- Admonition codeblock with alternate score layout: [monster2md-scores.txt](https://github.com/ebullient/ttrpg-convert-cli/tree/main/examples/templates/tools5e/monster2md-scores.txt)
- TTRPG statblock in the body: [monster2md-yamlStatblock-body.txt](https://github.com/ebullient/ttrpg-convert-cli/tree/main/examples/templates/tools5e/monster2md-yamlStatblock-body.txt)
- Admonition codeblock in the body with minimal TTRPG/Initiative tracker YAML metadata in the header: [monster2md-yamlStatblock-header.txt](https://github.com/ebullient/ttrpg-convert-cli/tree/main/examples/templates/tools5e/monster2md-yamlStatblock-header.txt)

## Getting Help

- There is a `#cli-support` thread in the `#tabletop-games` channel of the [Obsidian Discord](https://discord.gg/veuWUTm).
- There is a `TTRPG-convert-help` post in the `obsidian-support` forum of the [Obsidian TTRPG Community Discord](https://discord.gg/Zpmr37Uv).
- There is a TTRPG-convert tutorial (currently aimed at Windows users, but much of it is helpful no matter your OS) at [Obsidian TTRPG Tutorials](https://obsidianttrpgtutorials.com/Obsidian+TTRPG+Tutorials/Plugin+Tutorials/TTRPG-Convert-CLI/TTRPG-Convert-CLI+5e).

### Want to help fix it?

- If you're familiar with the command line and are comfortable running the tool, I hope you'll consider running [unreleased snapshots](docs/alternateRun.md#using-unreleased-snapshots) and reporting issues.
- If you want to contribute, I'll take help of all kinds: documentation, examples, sample templates, stylesheets are just as important as Java code. See [CONTRIBUTING](https://github.com/ebullient/ttrpg-convert-cli/tree/main/CONTRIBUTING.md).

## Other notes

This project uses Quarkus, the Supersonic Subatomic Java Framework. If you want to learn more about Quarkus, please visit its website: https://quarkus.io/.

This project is a derivative of [fc5-convert-cli](https://github.com/ebullient/fc5-convert-cli), which focused on working to and from FightClub5 Compendium XML files. It has also stolen some bits and pieces from [pockets-cli](https://github.com/ebullient/pockets-cli).
