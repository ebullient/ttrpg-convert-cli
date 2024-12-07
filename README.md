# TTRPG convert CLI

![GitHub all releases](https://img.shields.io/github/downloads/ebullient/ttrpg-convert-cli/total?color=success)

A Command-Line Interface designed to convert TTRPG data from 5eTools and Pf2eTools into crosslinked, tagged, and formatted markdown optimized for [Obsidian.md](https://obsidian.md).

<!-- markdownlint-disable no-inline-html -->
<table><tr>
<td>Jump</td>
<td><a href="#install-the-ttrpg-convert-cli">⬇ Download</a></td>
<td><a href="docs/configuration.md">⚙️ Configuration</a></td>
<td><a href="examples/">🎨 Examples</a></td>
<td><a href="examples/templates">🎨 Templates</a></td>
</tr><tr>
<td><a href="CHANGELOG.md">🚜 Changelog</a></td>
<td><a href="docs/sourceMap.md">🗺️ Source Map</a></td>
<td><a href="#convert-5etools-json-data">📖 5eTools</a></td>
<td><a href="#convert-pf2etools-json-data">📖 Pf2eTools</a></td>
<td><a href="#convert-homebrew-json-data">📖 Homebrew</a></td>
</tr></table>

I use [Obsidian](https://obsidian.md) to keep track of my campaign notes. This project parses JSON sources for materials that I own from the 5etools mirror to create linked and formatted markdown that I can reference in my notes.

> [!TIP]
>
> - 🚜 [**Review the changelog**](CHANGELOG.md) for new capabilities (✨) and breaking changes (🔥💥).
> - 🔮 Check out [**Conventions**](#conventions) and  [**Recommendations**](#recommendations-for-using-the-cli).
> - 🔥 Support for the 5e 2024 ruleset is [in progress](https://github.com/ebullient/ttrpg-convert-cli/discussions/586).  

## Using the Command Line

This tool works in the command line, which is a text-based way to give instructions to your computer.
If you're new to it, we have resources to help you get started below.

If you don't have a favorite method already, or you don't know what those words mean, here are some resources to get you started:

- For macOS / OSX users:
    - Start with the built-in `Terminal` application.
    - [Learn the macOS Command Line][]
- For Windows users:
    - [A Beginner's Guide to the Windows Command Line][]
    - See the [Windows README](README-WINDOWS.md)

[Learn the macOS Command Line]: https://blog.teamtreehouse.com/introduction-to-the-mac-os-x-command-line
[A Beginner's Guide to the Windows Command Line]: https://www.makeuseof.com/tag/a-beginners-guide-to-the-windows-command-line/

## Install the TTRPG Convert CLI

There are several options for running `ttrpg-convert`.
Choose the one you are most comfortable with:

- **Using Windows?** See the [Windows README](README-WINDOWS.md).
- `jbang`: [Use JBang!][jbang] (hides Java; sets up command aliases).
- `brew`: [Use Homebrew (macOS or Linux)][brew] (uses platform binaries).
- `bin`: [Use a pre-built platform binary][bin] (no Java required).
- `jar`: [Use Java to run the jar][jar].
- `src`: [Build from source][src].

| Platform       | Options  |
|----------------|----------|
|  Linux         | [jbang][], [brew][], [bin][], [jar][], [src][] |
|  Mac (Arm)     | [brew][], [jbang][], [bin][], [jar][], [src][] |
|  Mac (Intel)   | [brew][], [jbang][], [bin][], [jar][], [src][] |
|  Windows       | [📝](README-WINDOWS.md), [jbang][], [bin][], [jar][], [src][]  |
|  Windows (Old) | [📝](README-WINDOWS.md), [jbang][], [jar][], [src][]  |
|  Windows (WSL) | [brew][], [jbang][], [jar][], [src][] |

[jbang]: ./docs/alternateRun.md#use-jbang
[brew]: ./docs/alternateRun.md#use-homebrew
[bin]: ./docs/alternateRun.md#use-pre-built-platform-binary
[jar]: ./docs/alternateRun.md#use-java-to-run-the-jar
[src]: ./docs/alternateRun.md#build-and-run-from-source

## Recommendations for using the CLI

- 🔐 Treat generated content as a big ball of mud. Stick it in a corner of your vault and *treat it as read-only*.

    Trust us, you will want to regenerate content from time to time. It is cheap and easy to do if you don't have your own edits to worry about.

- 🔎 Have the CLI generate output into a separate directory and use a comparison tool to preview changes.

    You can use `git diff` to compare arbitrary directories. For example:

    ```bash
    git diff --no-index vault/compendium/bestiary generated/compendium/bestiary
    ```

- 📑 Use a copy tool that only updates modified files, like [rsync][], to avoid unnecessary file copying when updating your vault. Use the checksum option (`-c`) to compare file contents; the file modification date is meaningless given generated files are recreated when the tool is run. We have some suggestions in [discussion #220][sync-discussion], but it is very much a work in progress.

[rsync]: https://stackoverflow.com/a/19540611
[sync-discussion]: https://github.com/ebullient/ttrpg-convert-cli/discussions/220

### Required Plugins

- **Admonitions** ([git](https://github.com/javalent/admonitions)/[obsidian](obsidian://show-plugin?id=obsidian-admonition)): The Admonitions plugin supports a codeblock style admonition used for more complex embedded content. See [Admonition plugin notes](docs/README.md#admonitions) for more recommendations.

### Recommended Plugins

- **Force Note View Mode by Front Matter** ([git](https://github.com/bwydoogh/obsidian-force-view-mode-of-note)/[obsidian](obsidian://show-plugin?id=obsidian-view-mode-by-frontmatter)): I use this plugin to treat these generated notes as essentially read-only. See [Force Note View Mode plugin settings](docs/README.md#force-note-view-mode-by-front-matter) for recommendations.

- **Fantasy Statblocks** ([git](https://github.com/javalent/fantasy-statblocks)/[obsidian](obsidian://show-plugin?id=obsidian-5e-statblocks)): Templates for rendering monsters can define a `statblock` in the document body or provide a full or abridged YAML monster in the document header to update monsters in the plugin's bestiary.
    - See [Fantasy Statblocks plugin settings](docs/README.md#fantasy-statblocks) for recommendations.
    - See [Templates](examples/templates) for related template customization.

- **Initiative Tracker** ([git](https://github.com/javalent/initiative-tracker)/[obsidian](obsidian://show-plugin?id=initiative-tracker)): Templates for rendering monsters can include information in the header to define monsters that the Initiative Tracker can use when constructing encounters. See [Initiative Tracker plugin settings](docs/README.md#initiative-tracker) for recommendations.

- **Dataview** ([git](https://github.com/blacksmithgu/obsidian-dataview)/[obsidian](obsidian://show-plugin?id=dataview)): This plugin can be used to create custom views of the data and to create custom queries to find and display data in your vault. See [Working with Dataview](docs/README.md#working-with-dataview) for recommendations.

## Conventions

- **Links.** Documents generated by this plugin will use markdown links rather than wiki links. A [CSS snippet](examples/css-snippets/hide-markdown-link-url.css) can make these links less intrusive in edit mode by hiding the URL portion of the string.

- **File names.** To avoid conflicts and issues with different operating systems, all file names are slugified (all lowercase, symbols stripped, and spaces replaced by dashes). This is a familiar convention for those used to Jekyll, Hugo, or other blogging systems.

    - File names for resources outside of the core books (PHB, MM, and DMG) have the abbreviated source name appended to the end to avoid file name collisions.
    - All files have an `aliases` attribute that contains the original name of the resource.

- **Organization.** Files are generated in two roots: `compendium` and `rules`. The location of these roots is [configurable](docs/configuration.md#specify-target-paths-paths-key). These directories will be populated based on the sources you have enabled.

    - `compendium` contains files for items, spells, monsters, etc. The `compendium` directory is further organized into subdirectories for each type of content. For example, all items are in the `compendium/items` directory.

    - `rules` contains files for conditions, weapon properties, variant rules, etc.

    - `css-snippets` will contain **CSS files for special fonts** used by some content. You will need to copy these snippets into your vault (`.obsidian/snippets`) and enable them (`Appearance -> Snippets`) to ensure all content in your vault is styled correctly.

- **Styles.** Every document has a `cssclasses` attribute that assigns a CSS class. We have some [CSS snippets](examples/css-snippets/) that you can use to customize elements of the compendium.
    - 5eTools: `json5e-background`, `json5e-class`, `json5e-deck`, `json5e-deity`, `json5e-feat`, `json5e-hazard`, `json5e-item`, `json5e-monster`, `json5e-note`, `json5e-object`, `json5e-psionic`, `json5e-race`, `json5e-reward`, `json5e-spell`, and `json5e-vehicle`.
    - Pf2eTools: `pf2e`, `pf2e-ability`, `pf2e-action`, `pf2e-affliction`, `pf2e-archetype`, `pf2e-background`, `pf2e-book`, `pf2e-deity`, `pf2e-feat`, `pf2e-hazard`, `pf2e-index`, `pf2e-item`, `pf2e-note`, `pf2e-ritual`, `pf2e-spell`, `pf2e-trait`.

- **Admonitions.** Generated content uses code-block-style [Admonitions](docs/README.md#admonitions) in addition to Obsidian callouts. We have [Admonition definitions](examples/admonitions/) that you can import to ensure these admonition/callout types are defined.
    - `ad-statblock`
    - 5eTools: `ad-flowchart`, `ad-gallery`, `ad-embed-action`, `ad-embed-feat`, `ad-embed-monster`, `ad-embed-object`, `ad-embed-race`, `ad-embed-spell`, `ad-embed-table`
    - Pf2eTools: `ad-embed-ability`, `ad-embed-action`, `ad-embed-affliction`, `ad-embed-avatar`, `ad-embed-disease`, `ad-embed-feat`, `ad-embed-item`, `ad-pf2-note`, `ad-pf2-ritual`.

## Convert 5eTools JSON data

> [!NOTE]
> Instructions here use backslashes to wrap lines for readability (a common practice for Linux-based command shells).
>
> *If you are using Windows*, you will need to remove the backslashes and put the command on a single line. You may also need to append `.exe` to the command name (though it should work without).

1. Invoke the CLI with the `--version` option.

    ```shell
    ttrpg-convert --version
    ```

    You should see output similar to the following:

    ```shell
    ttrpg-convert version 2.3.18
    Git commit: ed56f76
    ```

    If you do, we know that the CLI is working!

    If you don't, there may be something wrong with your installation. Windows users, see the [Windows README for help](./README-WINDOWS.md#uh-oh-something-went-wrong).

2. Invoke the CLI to generate indexes and markdown for SRD content:

    ```shell
    ttrpg-convert \
      --index \
      -o dm \
      <5etools-data-dir>
    ```

    - `--index` generates two index files: `all-index.json` and `src-index.json`.

        > 🚀 TIP:
        > - Use `all-index.json` to see the reference keys for all discovered content. This can confirm that an included source was actually read.
        > - Use `src-index.json` to see the reference keys for content that was included in the generated output. Use this to confirm that your source selection is working as expected.

    - `-o dm` The target output directory (`dm` in this case). Files will be created in this directory.

    - `<5etools-data-dir>` is a placeholder for the location of downloaded 5eTools source data directory.

    This should produce a set of markdown files in the `dm` directory that contains only SRD content.

3. Next, you'll want to create a [configuration file](docs/configuration.md) to set up your sources.

    The configuration is provided to the CLI using the `-c` option:

    ```shell
    ttrpg-convert \
        --index \
        -o dm \
        -c my-config.json \
        <5etools-data-dir>
    ```

    > 🚀 Note: Only include content you own. Find the identifier for your sources in the [Source Map](./docs/sourceMap.md#source-name-mapping-for-5etools).

Next step:

- Create your own [configuration file](docs/configuration.md).

## Convert Pf2eTools JSON data

🚜 🚧 🚜 🚧 🚜 🚧 🚜 🚧

> [!NOTE]
> Instructions here use backslashes to wrap lines for readability (a common practice for Linux-based command shells).
>
> *If you are using Windows*, you will need to remove the backslashes and put the command on a single line. You may also need to append `.exe` to the command name (though it should work without).

1. Invoke the CLI with the `--version` option:

    ```shell
    ttrpg-convert --version
    ```

    You should see output similar to the following:

    ```shell
    ttrpg-convert version 2.3.18
    Git commit: ed56f76
    ```

    If you do, we know that the CLI is working!

    If you don't, there may be something wrong with your installation. Windows users, see the [Windows README for help](./README-WINDOWS.md#uh-oh-something-went-wrong).

2. Invoke the CLI. In this first example, let's generate indexes and markdown for default content:

    ```shell
    ttrpg-convert \
      -g pf2e \
      --index \
      -o dm \
      <Pf2eTools-data-dir>
    ```

    - `-g pf2e` The game system! Pathfinder 2e!
    - `--index` generates two index files: `all-index.json` and `src-index.json`.

      > 🚀 TIP:
      > - Use `all-index.json` to see the reference keys for all discovered content. This can confirm that an included source was actually read.
      > - Use `src-index.json` to see the reference keys for content that was included in the generated output. Use this to confirm that your source selection is working as expected.

    - `-o dm` The target output directory. Files will be created in this directory.

    - `<Pf2eTools-data-dir>` is a placeholder for the location of downloaded 5eTools source data directory.

3. Next, you'll want to create a [configuration file](docs/configuration.md) to set up your sources.

    The configuration is provided to the CLI using the `-c` option:

    ```shell
    ttrpg-convert \
        -g pf2e \
        --index \
        -o dm \
        -c my-config.json
        <Pf2eTools-data-dir>
    ```

    > 🚀 Note: Only include content you own. Find the identifier for your sources in the [Source Map](./docs/sourceMap.md#source-name-mapping-for-pf2etools).

Next step:

- Create your own [configuration file](docs/configuration.md).

## Convert Homebrew JSON data

The CLI tool can also import homebrewed content, though this content must still fit the JSON standards set by the [5eTools JSON spec][5etools JSON] or the PF2eTools JSON spec (coming soon, similar to 5eTools).

Perhaps the simplest way to import homebrew is to use existing homebrew data from the 5eTools homebrew GitHub repo: <https://github.com/TheGiddyLimit/homebrew>.

> [!TIP]
> 🍺 *You only need the specific file you wish to import*.
>
> Homebrew data is different from the 5eTools data. Each homebrew file is a complete reference. If you compare it to cooking: the 5eTools mirror repo is organized by ingredient (all of the carrots, all of the onions, ...); homebrew data is organized by prepared meal / complete recipe.

Adding homebrew content is easiest if you use a [configuration file](./docs/configuration.md). We will assume a file named `my-config.json` for the example below, but you can use any name you like.

> [!IMPORTANT]
> 🚀 Respect copyrights and support content creators; use only the sources you own.

For example, if you want to use Benjamin Huffman's popular homebrewed [Pugilist class](https://www.dmsguild.com/product/184921/The-Pugilist-Class):

1. Download a copy of the [Pugilist JSON file](https://github.com/TheGiddyLimit/homebrew/blob/master/class/Benjamin%20Huffman%3B%20Pugilist.json).

    Save this file to a well-known location on your computer. It is probably easiest if it sits next to your 5eTools or Pf2eTools directory.

2. Update your [configuration file](docs/configuration.md) to add a `homebrew` section under `sources`:

    ```json
    {
        "sources": {
            ...
            "homebrew": [
                "path/to/Benjamin Huffman; Pugilist.json"
            ]
        }
    }
    ```

    - `path/to/` is a placeholder for a relative or absolute path to the file[^1]. Here are a few ways to determine the path to a file:
        - You may be able to drag and drop the file into the terminal window.
        - You may be able to right-click on the file and select "Copy Path".
        - *Windows users*: When pasting the path into a text editor, use find/replace to replace all `\` with `/`.

3. Run the command like so (for 5e homebrew):

    ```shell
    ttrpg-convert \
        --index \
        -o hb-compendium \
        -c my-config.json \
        <5etools-data-dir>
    ```

    - `-o hb-compendium` specifies the output directory for generated content.
    - `-c my-config.json` specifies the name and/or path to your configuration file.
    - `<5etools-data-dir>` is a placeholder for the 5eTools source data directory.

See [configuration](docs/configuration.md) for more details on how to configure the CLI.

The process is similar for other homebrew, including your own, as long as it is broadly compatible with the [5eTools JSON spec](https://wiki.tercept.net/en/Homebrew/FromZeroToHero).

## Where to find help

- There is a `#cli-support` thread in the `#tabletop-games` channel of the [Obsidian Discord](https://discord.gg/veuWUTm).
- There is a `TTRPG-convert-help` post in the `obsidian-support` forum of the [Obsidian TTRPG Community Discord](https://discord.gg/Zpmr37Uv).
- There is a TTRPG-convert tutorial (currently aimed at Windows users, but much of it is helpful no matter your OS) at [Obsidian TTRPG Tutorials](https://obsidianttrpgtutorials.com/Obsidian+TTRPG+Tutorials/Plugin+Tutorials/TTRPG-Convert-CLI/TTRPG-Convert-CLI+5e).
- If you open an issue for an error, run with the `--debug` and `--log` options, and attach the log file to the issue.

### Want to help fix it?

- If you're familiar with the command line and are comfortable running the tool, please consider running [unreleased snapshots](docs/alternateRun.md#using-unreleased-snapshots) and reporting issues.
- If you want to contribute, I'll take help of all kinds: documentation, examples, sample templates, and stylesheets are just as important as Java code. See [CONTRIBUTING](CONTRIBUTING.md).

## Other notes

This project uses Quarkus, the Supersonic Subatomic Java Framework. To learn more about Quarkus, please visit its website: <https://quarkus.io/>.

This project is a derivative of [fc5-convert-cli](https://github.com/ebullient/fc5-convert-cli), which focused on working with FightClub5 Compendium XML files. It has also borrowed some bits and pieces from [pockets-cli](https://github.com/ebullient/pockets-cli).

[5eTools JSON]: https://wiki.tercept.net/en/Homebrew/FromZeroToHero

<a href="https://www.buymeacoffee.com/ebullient" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/v2/default-blue.png" alt="Buy Me A Coffee" style="height: 60px !important;width: 217px !important;"></a>

[^1]: Description of relative vs absolute file paths: <https://stackoverflow.com/a/10288252>. If you use a relative path, it will be resolved relative to the current working directory, as described here: <https://en.wikipedia.org/wiki/Working_directory>.
