# CLI Configuration guide

> 🚀 Respect copyrights and support content creators; use only the sources you own.

The Command Line Interface (CLI) provides a means to tailor data transformation using configuration files. In this guide, we'll walk through the configuration options and their practical uses.

You can configure the CLI with a JSON or YAML file, either instead of, or along with, command line parameters. See [examples/config](../examples/config) for the general config file structure in both formats. 

> 📝 JSON and YAML are both file formats for storing data in useful and human-readable ways.
> 
> - JSON: If you want to know why the `{}` and `[]` are used in the ways that they are you can read about json *objects* and *arrays* [here](https://www.toolsqa.com/rest-assured/what-is-json/)).
> - YAML (Yet another markup language) is described by a [specification](https://yaml.org/spec/1.2/spec.html). Leading whitespace (indentation) matters.


- [Overview](#overview)
- [Select data sources with the `from` key](#select-data-sources-with-the-from-key)
- [Specify target paths with the `paths` key](#specify-target-paths-with-the-paths-key)
- [Refine content choices](#refine-content-choices)
    - [Exclude items matching an `excludePattern`](#exclude-items-matching-an-excludepattern)
    - [Exclude items with `exclude`](#exclude-items-with-exclude)
    - [Include items with `include`](#include-items-with-include)
    - [Include source-book content with `full-source`](#include-source-book-content-with-full-source)

## Overview

The following examples use JSON.

### Basic configuration example

Here is a simple example of a `config.json` file. As a JSON Object, it has a `"key": "value"` structure.

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
2. **Target vault path:** Using the key `path`, we specify the target path for writing generated `compendium` and `rules` content. These directories will be created if they don't exist, and are relative to the output directory specified on the command line with `-o`.

### Extended configuration example

Below is a more detailed example of a `config.json` file. 
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
        "VGM",
        "LoX"
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
    "full-source": {
        "adventure": [
            "LMoP",
            "LoX"
        ],
        "book": [
            "5etools-mirror-1.github.io/data/book/book-phb.json"
        ]
    }
}
```

This example performs additional functions:

1. **Filter Input Sources:** Using the key `from`, we list the sources (that we own) that we want to include.
2. **Target vault path:** Using the key `path`, we specify the target path for writing generated `rules` content. These directories will be created if they don't exist, and are relative to the output directory specified on the command line with `-o`.
3. **Fine-grained exclusion:** Specific content is excluded using [`excludePattern`](#exclude-items-matching-an-excludepattern) and [`exclude`](#exclude-items-with-exclude).
4. **Fine-grained inclusion:** Specific content is *always included* using [`include`](#include-items-with-include).
5. **Incorporate complete sources:** Using [`full-source`](#include-source-book-content-with-full-source), we specify that we want to incorporate the complete text of the *Player's Handbook*  (book), the *Lost Mine of Phandelver* (adventure) and *Light of Xaryxis* (adventure).

## Select data sources with the `from` key

The `from` key lets you specify which sources to draw data from. 
List the codes or abbreviations for your chosen sources.

**Example:**

```json
  "from": [
    "AI",
    "PHB",
    ...
  ]
```

> 🚀 Note: Only include content you own. Find the abbreviations for your sources in the [Source Map](https://github.com/ebullient/ttrpg-convert-cli/blob/main/docs/sourceMap.md).
> 
> To find the abbreviation or reference for homebrew sources, look in the source file: 

## Specify target paths with the `paths` key

The `paths` key informs the CLI where to store the generated content.

- New directories are made if they aren't already present.
- Paths are relative to the CLI's designated output location (`-o`).

Content typically falls into:

- `compendium`: most backgrounds, classes, items, spells, monsters, etc.
- `rules`: conditions, weapon properties, variant rules, etc.

**Example:**

```json
  "paths": {
    "compendium": "/compendium/",
    "rules": "/rules/"
  }
```

🔹 The leading slash is optional. It marks a path starting from the root of your Obsidian vault.

## Refine content choices

You can achieve more precision in content selection by utilizing data keys.

The CLI `--index` option compiles two lists of data keys:
- `allIndex.json`: Lists all potential data keys.
- `allSourceIndex.json`: Lists the data keys actually used in your output.

### Exclude items matching an `excludePattern`

This option allows you to exclude data entries based on matching patterns.

```json
"excludePattern": [
    "race|.*|dmg"
]
```

### Exclude items with `exclude`

Specify the data keys you want to omit.

```json
"exclude": [
    "monster|expert|dc",
    ...
]
```

### Include items with `include`

Specify the data keys you want to include.

```json
"include": [
    "race|changeling|mpmm"
]
```

This is particularly useful for content acquired piecemeal, like individual items from D&D Beyond.

### Include source-book content with `full-source`

By default, the CLI generates notes for specific items like monsters or spells. 

Use the `full-source` key if you want to generate notes for the text of the source you own, either book or adventure. To fully incorporate a source, its abbreviation should appear in both the `from` list and the `full-source` section.

> 📝 The `convert` configuration attribute is an older (less intuitive) name for `full-source`.

Here is an example that will create notes for the *Player's Handbook* (a book, PHB) and *The Wild Beyond the Witchlight* (an adventure, WBtW):

```json
"from": {
    "PHB",
    "WBtW"
    ...
}
...
"full-source": {
    "adventure": [
        "WBtW",
        ...
    ],
    "book": [
        "5etools-mirror-1.github.io/data/book/book-phb.json"
    ]
}
```

Note that you can either use the resource identifier ("WBtW") or specify the path to the resource JSON (".../book-phb.json") file in the `full-source` section.

Some adventures may be broken down into individual modules. If there's any confusion about how to reference them (book or adventure), the generated index (`allIndex.json`) can be a helpful reference.
