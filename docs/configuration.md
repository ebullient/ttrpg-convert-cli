# CLI Configuration guide

> [!IMPORTANT]
> 🚀 Respect copyrights and support content creators; use only the sources you own.

This guide introduces you to configuring data transformations using the Command Line Interface (CLI). Whether you're new to command line tools or an experienced user, you'll find helpful information on utilizing configuration files to tailor your experience.

**Table of Contents**

- [Overview](#overview)
    - [Basic configuration example](#basic-configuration-example)
    - [Advanced configuration example](#advanced-configuration-example)
- [Source identifiers](#source-identifiers)
- [Include complete text with `full-source`](#include-complete-text-with-full-source)
    - [Homebrew](#homebrew)
- [Include reference data with the `from` key](#include-reference-data-with-the-from-key)
- [Specify target paths( `paths` key)](#specify-target-paths-paths-key)
- [Refine content choices](#refine-content-choices)
    - [Excluding content matching an `excludePattern`](#excluding-content-matching-an-excludepattern)
    - [Excluding specific content with `exclude`](#excluding-specific-content-with-exclude)
    - [Including specific content with `include`](#including-specific-content-with-include)

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
    ]
}
```

Additional capabilities:

1. **Incorporate complete sources:** The [`full-source`](#include-complete-text-with-full-source) key is used to include complete text of specified sources.
2. **Select Input Sources:** As before, the [`from`](#include-reference-data-with-the-from-key) key lists the sources to include.
3. **Define Vault Paths:** The [`path`](#specify-target-paths-paths-key) sets the vault path destination for `rules` content.
4. **Targeted exclusion:** [`excludePattern`](#excluding-content-matching-an-excludepattern) and [`exclude`](#excluding-specific-content-with-exclude) leaves out specific content.
5. **Targeted inclusion:** The [`include`](#including-specific-content-with-include) specifies content that is *always included*.

## Source identifiers

> 🚀 Remember: Only include content you legally own.

Sources in 5eTools and Pf2eTools are referenced by unique identifiers. Find the identifiers for your sources in the [Source Map](sourceMap.md).

Some are split into multiple files, in which case, you will need to specify each identifier separately. For example, *Tales from the Yawning Portal* is split into seven files. Content appears using any one of the seven (`TftYP-*`), in addition to `TftYP` for common content. If you want to include all of them, you will need to specify each identifier separately.

If you're expecting to see content from a book or adventure and it's not showing up, run the CLI with the `--index` option, and check the `allIndex.json` file to see which source identifier you should be using.

## Include complete text with `full-source`

By default, the CLI generates notes for reference items like backgrounds, monsters (bestiary), classes, decks (and cards), deities, feats, items, and so on.

Use the `full-source` key if you want to generate notes for the text of the source you own. Any [source identifer](#source-identifiers) specified in `full-source` will be included in the generated vault. In other words, if a source identifier is present in `full-source`, `from` is implied.

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

In the above example, `path/to/` is a placeholder. There are a few ways to figure out the path to a file:

- You may be able to drag and drop the file into the terminal window.
- You may have the ability to right-click on the file and select "Copy Path".
- **Windows users**: When pasting the path into your config file, use find/replace to replace all `\` with `/`.

## Include reference data with the `from` key

The `from` key allows you to choose which sources to use. List the [identifiers](#source-identifiers) for your chosen sources here.

**Example:**

```json
  "from": [
    "AI",
    "PHB",
    ...
  ]
```

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
> Do not reorganize or edit the generated content. Tuck generated content away in your vault and use it as read-only reference material. It should be cheap and easy to re-run the tool (add more content, errata, etc.)

## Refine content choices

You can use the following configuration to exclude or include specific data.

Just as source material has an identifier, so does each piece of data. The *Monster Manual* has the identifier `MM`. Each monster in the *Monster Manual* has its own key, such as `monster|black dragon wyrmling|mm` or `item|drow +1 armor|mm`.

The CLI `--index` option compiles two lists of data keys:

- `allIndex.json`: Lists all discovered data keys.
- `allSourceIndex.json`: Lists the data keys after filters (`full-source`, `from`, and the config options below) have been applied.

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
