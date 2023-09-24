# CLI Configuration guide

The Command Line Interface (CLI) provides a means to tailor data transformation using configuration files. 
These files, written in JSON or YAML, establish guidelines for how data is converted.

In this guide, we'll walk through the configuration options and their practical uses.

> 🚀 Remember to respect copyrights, and use only the sources you own.

## Overview

Below is a detailed example of a `config.json` file. This configuration aims to:

- Choose specific data sources.
- Set target paths for the resulting content.
- Designate which content to include or exclude.
- Incorporate complete sources, such as entire books or adventures.

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

## Selecting Data Sources: The `from` Key

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

> 🚀 Note: Ensure you own the content you're including. Find the abbreviations for your sources in the [Source Map](https://github.com/ebullient/ttrpg-convert-cli/blob/main/docs/sourceMap.md).

## Specifying Target Paths: The `paths` Key

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

## Refining Content Choices: Detailed Filtering

You can achieve more precision in content selection by utilizing data keys.

The CLI `--index` option compiles two lists of data keys:
- `allIndex.json`: Lists all potential data keys.
- `allSourceIndex.json`: Lists the data keys actually used in your output.

### Using `excludePattern`

This option allows you to exclude data entries based on matching patterns.

```json
"excludePattern": [
    "race|.*|dmg"
]
```

### Direct Exclusion with `exclude`

Specify the data keys you want to omit.

```json
"exclude": [
    "monster|expert|dc",
    ...
]
```

### Direct Inclusion with `include`

Specify the data keys you want to include.

```json
"include": [
    "race|changeling|mpmm"
]
```

This is particularly useful for content acquired piecemeal, like individual items from D&D Beyond.

## Incorporating Complete Sources with `full-source`

By default, the CLI extracts specific entries, such as monsters or spells. 
Use the `full-source` or `convert` key if you want to generate notes for entire texts, like complete books or adventures.

```json
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

To fully incorporate a source, its abbreviation should appear in both the `from` list and the `full-source` section.

Keep in mind that certain adventures may be broken down into individual modules. If there's any confusion about how to reference them, the generated index (`allIndex.json`) can be a helpful reference.
