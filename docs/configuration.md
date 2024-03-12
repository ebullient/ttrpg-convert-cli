# CLI Configuration guide

> [!IMPORTANT]
> 🚀 Respect copyrights and support content creators; use only the sources you own.

This guide introduces you to configuring data transformations using the Command Line Interface (CLI). Whether you're new to command line tools or an experienced user, you'll find helpful information on utilizing configuration files to tailor your experience.

<!-- markdownlint-disable-next-line no-emphasis-as-heading -->
**Table of Contents**

- [Overview](#overview)
    - [Basic configuration example](#basic-configuration-example)
    - [Advanced configuration example](#advanced-configuration-example)
- [Source identifiers](#source-identifiers)
- [Include reference data with the `from` key](#include-reference-data-with-the-from-key)
- [Include complete text with `full-source`](#include-complete-text-with-full-source)
    - [Homebrew](#homebrew)
    - [Additional notes about homebrew](#additional-notes-about-homebrew)
    - [Reporting content errors to 5eTools](#reporting-content-errors-to-5etools)
- [Specify target paths( `paths` key)](#specify-target-paths-paths-key)
- [Refine content choices](#refine-content-choices)
    - [Excluding content matching an `excludePattern`](#excluding-content-matching-an-excludepattern)
    - [Excluding specific content with `exclude`](#excluding-specific-content-with-exclude)
    - [Including specific content with `include`](#including-specific-content-with-include)
- [Use the dice roller plugin](#use-the-dice-roller-plugin)
- [Tag prefix](#tag-prefix)
- [Copying internal images](#copying-internal-images)
- [Copying external images](#copying-external-images)

## Overview

The CLI can be set up using JSON or YAML files. These files allow you to specify your preferences and can be used alongside or in place of command-line options. For examples of configuration file structures in both formats, see [examples/config](../examples/config).

> [!NOTE]
> 📝 JSON and YAML are both file formats for storing data in useful and human-readable ways.
>
> - JSON: If you want to know why the `{}` and `[]` are used in the ways that they are you can read about json *objects* and *arrays* [here](https://www.toolsqa.com/rest-assured/what-is-json/)).
> - YAML: A format where indentation (spaces at the beginning of lines) is important. Learn about YAML's [specification](https://yaml.org/spec/1.2/spec.html).

The following examples are presented in JSON.

### Basic configuration example

Below is a straightforward `config.json` file. In this format, settings are noted in a `"key": "value"` structure.

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

1. **Select Input Sources:** The `from` key lists the sources to be included, identified by their [source identifiers](#source-identifiers).
2. **Define Vault Paths:** The [`paths`](#specify-target-paths-paths-key) key sets the destination paths for the `compendium` and `rules` content. These paths are relative to the output directory set in the CLI command with `-o`.

> [!WARNING]
> **Windows Users**: Replace any `\` with `/` in paths in JSON or YAML files

### Advanced configuration example

Here's a more comprehensive `config.json` file.

```json
{
    "full-source": {
        "adventure": [
            "LMoP",
            "LoX"
        ],
        "book": [
            "PHB"
        ],
        "homebrew": [
            "homebrew/creature/MCDM Productions; Flee, Mortals!.json"
        ]
    },
    "from": [
        "AI",
        "DMG",
        "TCE",
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
    ],
    "include": [
        "race|changeling|mpmm"
    ],
    "useDiceRoller": true,
    "tagPrefix": "ttrpg-cli",
}
```

Additional capabilities:

1. **Incorporate complete sources:** The [`full-source`](#include-complete-text-with-full-source) key is used to include complete text of specified sources.
2. **Select Input Sources:** As before, the [`from`](#include-reference-data-with-the-from-key) key lists the sources to include.
3. **Define Vault Paths:** The [`path`](#specify-target-paths-paths-key) sets the vault path destination for `rules` content.
4. **Targeted exclusion:** [`excludePattern`](#excluding-content-matching-an-excludepattern) and [`exclude`](#excluding-specific-content-with-exclude) leaves out specific content.
5. **Targeted inclusion:** The [`include`](#including-specific-content-with-include) specifies content that is *always included*.
6. **Use the dice roller plugin:** The [`useDiceRoller`](#use-the-dice-roller-plugin) key enables the dice roller plugin.
7. **Tag prefix:** The [`tagPrefix`](#tag-prefix) key sets the prefix for tags generated by the CLI.

> [!WARNING]
> **Windows Users**: Replace any `\` in your paths with '/' in your JSON and YAML files.

## Source identifiers

> 🚀 Remember: Only include content you legally own.

Sources in 5eTools and Pf2eTools are referenced by unique identifiers. Find the identifiers for your sources in the [Source Map](sourceMap.md).

Some are split into multiple files, in which case, you will need to specify each identifier separately. For example, *Tales from the Yawning Portal* is split into seven files. Content appears using any one of the seven (`TftYP-*`), in addition to `TftYP` for common content. If you want to include all of them, you will need to specify each identifier separately.

If you're expecting to see content from a book or adventure and it's not showing up, run the CLI with the `--index` option, and check the `all-index.json` file to see which source identifier you should be using.

## Include reference data with the `from` key

The CLI creates notes for reference items like backgrounds, monsters, classes, and more.

To generate reference notes for your own sources, add their [identifiers](#source-identifiers) to the `from` key.

Here is an example that will create reference notes (spells, classes, etc. but not the full text) for the *Player's Handbook* (PHB) and *Acquisitions Incorporated* (AI):

```json
  "from": [
    "AI",
    "PHB",
    ...
  ]
```

## Include complete text with `full-source`

> [!TIP]
> You only need to list your source once. Either in `from` or in `full-source`, but not both.

Use the `full-source` key to create notes for all content and reference data from your sources. Add the [source identifiers](#source-identifiers) to `full-source` to include them in the vault.

Here is an example that will create notes for the *Player's Handbook* (a book, PHB) and *The Wild Beyond the Witchlight* (an adventure, WBtW):

```json
"full-source": {
    "adventure": [
        "WBtW"
    ],
    "book": [
        "PHB"
    ]
}
```

Adventures are found in the `adventure` subdirectory, while books are found in the `book` subdirectory. Look for `adventure/adventure-<identifier>.json` or `book/book-<identifier>.json` to determine which category to use.

### Homebrew

> [!TIP]
> 🍺 *You only need the particular file you wish to import*.
>
> Homebrew data is different from the 5etools or Pf2eTools data. Each homebrew file is a complete reference. If you compare it to cooking: the 5etools and Pf2eTools mirror repositories are organized by ingredient (all of the carrots, all of the onions, ... ); homebrew data is organized by prepared meal / complete receipe.
>
> Support your content creators! Only use homebrew that you own.

To include Homebrew in your notes, specify the path to the homebrew json file in a `homebrew` section inside of `full-source`.

For example, if you wanted to use Benjamin Huffman's popular homebrewed [Pugilist class](https://www.dmsguild.com/product/184921/The-Pugilist-Class):

1. Download a copy of the [Pugilist json file](https://github.com/TheGiddyLimit/homebrew/blob/master/class/Benjamin%20Huffman%3B%20Pugilist.json).

    Save this file to a well-known location on your computer. It is probably easiest if it sits next your 5eTools or Pf2eTools directory.

2. Add the path to this file to a `homebrew` section under `full-source`:

    ```json
    {
      "full-source": {
        "homebrew": [
            "path/to/Benjamin Huffman; Pugilist.json"
        ]
      }
    }
    ```

In the above example, `path/to/` is a placeholder. If you use a relative path, it will be resolved relative to the current working directory[^1]. An absolute path[^2] will also work.

There are a few ways to figure out the path to a file:

- You may be able to drag and drop the file into the terminal window.
- You may have the ability to right-click on the file and select "Copy Path".

> [!WARNING]
> **Windows Users**: Replace any `\` with `/` in paths in JSON or YAML files

### Additional notes about homebrew

Homebrew json files are not rigorously validated. There may be errors when importing.
I've done what I can to make the errors clear, or to highlight the suspect json, but I can't catch everything.

Here are some examples of what you may see, and how to fix them:

- `Unable to find image 'img/bestiary/MM/Green Hag.jpg'` (or similar)

    This kind of path refers to an "internal" (meaning part of the base 5e corpus of stuff) image. These paths are computed relative to a known base. With mirror2, the base has changed (to a different repository structure, see [Copying "internal" images](#copying-internal-images)), and images have, by and large, been converted from `.jpg` or `.png` to `.webp`. Fixing this kind of error is usually a case of fixing the path.

    - If you can fix the link yourself (change to `.webp`, mirror1 path to revised mirror2 path by removing `img/`), please [report it in 5eTools #brew-conversion](#reporting-content-errors-to-5etools).
    - If you can't find the image that should be used instead, please [ask for CLI help](../README.md#where-to-find-help)and we'll help you find the right one.

- `Unknown spell school Curse in sources[spell|ventus|wandsnwizards]`; similar for item types, item properties, conditions, skills, abilities, etc.

    This kind of error could be caused by a missing companion file (check dependencies listed in the `meta` information at the top of the homebrew file) or a missing definition.

    - If you find the missing definition (or it is already present in the homebrew file), and the error persists, please [ask for CLI help first](../README.md#where-to-find-help) so I can make sure there isn't a bug in the CLI preventing it from being read.
    - If the data really is missing from the homebrew json, please [report it in 5eTools #brew-conversion](#reporting-content-errors-to-5etools).

- You may see messages about missing fields or badly formed tables.

    - If you can fix the error by fixing the json content, please [report it in 5eTools #brew-conversion](#reporting-content-errors-to-5etools).
    - If you can't fix the error yourself, please [ask for CLI help first](../README.md#where-to-find-help) so I can make sure there isn't a bug in the CLI.

### Reporting content errors to 5eTools

> [!NOTE]
> The lovely folks at 5eTools don't understand the CLI, and they don't need to. If you report an issue, keep the details focused on the JSON content (typo, missed definition, etc.). If you aren't sure, [ask for CLI help first](../README.md#where-to-find-help).

If you can fix the error by fixing the json content, please report the error in the 5eTools discord channel.

1. Identify the homebrew source that contains the error in the [homebrew manager](https://wiki.tercept.net/en/5eTools/HelpPages/managebrew).
2. Click the "View Converters" button on the right to find the converter(s).
3. Tag them in the [#⁠brew-conversion](https://discord.com/channels/363680385336606740/493154206115430400) channel, describing what you fixed. If you can't find the right user to tag, or if no one is listed as a converter, post the report anyway but make sure you mention this!

Use the following form for your report.

```text
**Brew:**   Write the name of the homebrew source here  
**Converter:**   @converter's-discord-ID   
**Issue:**   Describe the issue here in clear, concise terms (e.g. "the Red-Spotted Gurgler is listed as having AC 15, when it should be 16")  
**Steps for reproduction:**   If you have to do something specific to make the error appear, describe them here. 
> I go to X and click on Y 
> I expected Z, but instead ...
```

For that last part, you may need to do some digging. Do not report the error using CLI exception messages. Stick to the observed missing links or errors in the data.

## Specify target paths( `paths` key)

The `paths` key specifies vault path for generated content.

- New directories are made if they aren't already present.
- Paths are relative to the CLI's designated output location (`-o`), which correlates to the root of your Obsidian vault.

**Example:**

```json
  "paths": {
    "compendium": "/compendium/",
    "rules": "/rules/"
  }
```

> [!TIP]
> The leading slash is optional. It marks a path starting from the root of your Obsidian vault.

5eTools and Pf2eTools content is organized differently, but in general, information is organized as follows:

- `compendium`: backgrounds, classes, items, spells, monsters, etc.
- `rules`: conditions, weapon properties, variant rules, etc.

> [!WARNING]
> Do not reorganize or edit the generated content. Tuck generated content away in your vault and use it as read-only reference material. It should be cheap and easy to re-run the tool (add more content, errata, etc.). See [Recommendations](../README.md#recommendations-for-using-the-cli) for more information.

## Refine content choices

You can use the following configuration to exclude or include specific data.

Just as source material has an identifier, so does each piece of data. The *Monster Manual* has the identifier `MM`. Each monster in the *Monster Manual* has its own key, such as `monster|black dragon wyrmling|mm` or `item|drow +1 armor|mm`.

The CLI `--index` option compiles two lists of data keys:

- `all-index.json`: Lists all discovered data keys.
- `src-index.json`: Lists the data keys after filters (`full-source`, `from`, and the config options below) have been applied.

### Excluding content matching an `excludePattern`

This option allows you to exclude data entries based on matching patterns.

```json
"excludePattern": [
    "race|.*|dmg"
]
```

### Excluding specific content with `exclude`

Specify the data keys you want to omit.

```json
"exclude": [
    "monster|expert|dc",
    ...
]
```

### Including specific content with `include`

Specify the data keys you want to include.

```json
"include": [
    "race|changeling|mpmm"
]
```

This approach is ideal for content acquired in parts, like individual items from D&D Beyond.

## Use the dice roller plugin

The CLI can generate notes that include inline dice rolls. To enable this feature, set the `useDiceRoller` key to `true`.

If you render dice rolls, set `useDiceRoller` to true to use dice roller strings when replacing dice `{@dice }`, and `{@damage }` strings.

Please note that if you are using a custom template and fantasy statblocks, you do **not** need to set the dice roller in your config. Fantasy statblocks will take care of the rendering itself.

## Tag prefix

The `tagPrefix` key sets the prefix for tags generated by the CLI. This is useful if you want to distinguish between tags generated by the CLI and tags you've created yourself.

For example, the CLI generates tags like `compendium/src/phb` and `spell/level/1`. If you set `tagPrefix` to `5e-cli`, the tags will be `5e-cli/compendium/src/phb` and `5e-cli/spell/level/1`.

## Copying internal images

Internal images are part of the 5eTools or Pf2e tools corpus of content. They are referenced by computed path (like tokens) or by media references marked as "internal".

5eTools mirror-2 moved internal images into a separate repository. Downloads can take some time, and the images repository is quite large.

**By default, no files are downloaded**. Links will reference the remote location, and you will need to be online to view the images. This is a safe, fast, and relatively well-behaving option given that downloads can be quite slow, and you may change your mind about where you want content to be generated.

The following configuration options allow you to change how the CLI treats these internal image references.

- Set `images.copyInternal` to `true` (as shown below) in your configuration file to instruct the CLI to copy "internal" images into your vault. This will make your vault larger, but you will not need to be online to view the images.

    ```json
    "images": {
        "copyInternal": true,
    }
    ```

    With just this setting, the CLI will download each "internal" image it hasn't seen before into your compendium. It is a lot of individual requests. If you have a slow connection, the next option may be better.

- Create a shallow clone of the images repository, and set `images.internalRoot` in your configuration file to tell the CLI where it can locally find "internal" images.

    ```shell
    git clone --depth 1 https://github.com/5etools-mirror-2/5etools-img.git
    ```

    ```json
    "images": {
        "copyInternal": true,
        "internalRoot": "5etools-img"
    }
    ```

    With this setting, the CLI will look for "internal" images in the local directory, which will speed things up (at the cost of a few extra steps). If you use a relative path, it will be resolved relative to the current working directory[^1]. An absolute path[^2] will also work. You will get an error message if that directory doesn't exist (and it will tell you the directory it tried to find).

## Copying external images

External images are images that are not part of the 5eTools or Pf2eTools corpus of content. They are referenced by media references marked as "external", and usually begin with "http://" or "https://". Some homebrew content may use a "file://" URL[^3] to reference a local file.

**By default, external images are not downloaded.** Links will reference the remote location, and you will need to be online to view the images. This is a safe, fast, and relatively well-behaving option given that downloads can be quite slow, and you may change your mind about where you want content to be generated.

To download remote images, set `images.copyExternal` to `true` (as shown below) in your configuration file to instruct the CLI to copy "external" images into your vault. This will make your vault larger, but you will not need to be online to view the images.

```json
"images": {
    "copyExternal": true,
}
```

With this setting, the CLI will copy all "external" images it hasn't seen before into your compendium.

[^1]: The working directory is the directory you were in (in the terminal) when you launched the CLI. See <https://en.wikipedia.org/wiki/Working_directory> for more information
[^2]: Example/explanation of absolute vs. relative path: <https://stackoverflow.com/a/10288252>.
[^3]: A URL is a uniform resource locator, more information at <https://en.wikipedia.org/wiki/URL>.
