# Changelog

[examples/config]: examples/config
[ex-snippets]: examples/css-snippets
[ex-templates]: examples/templates
[def-templates]: src/main/resources/templates/

**Note:** Entries marked with "🔥" indicate crucial or breaking changes that might affect your current setup.

## 🔥✨ 3.0.0: Moving the things

Support for the 2024 ruleset caused a lot of ripples. There is nothing small about this release.

- *Specifying Sources*
    - Removed `-s` option. Sources must be specified in config files.
    - The [Source Map](./docs/sourceMap.md) for 5e sources now includes `book` or `adventure` status
    - Combined `from` and `full-source` into unified `sources` key with four types:

        ```json
        {
            "sources": {
                "reference": [...],  // Reference-only content
                "book": [...],       // Complete book content
                "adventure": [...],  // Complete adventure content 
                "homebrew": [...]    // Custom content
            }
        }
        ```

    - *Reprint handling*: CLI will default to the most recent version available for your selected sources.
        - Use `include`/`exclude` config to override.
        - For SRD/Basic rules for 2014 content, use: "srd", "basicrules"
        - For SRD/Basic rules for 2024 content, use: "srd52", "freerules2024"
- *Generated content*
    - Changed Players Handbook directory to `players-handbook-2014`
    - Dice roller uses the `|text` flag to make text more consistent
      and readable (dice roll occurs on hover)
    - Added a `lists` folder in the compendium:
        - Lists of optional features
        - Lists of spells by class, school, subclass, background, feat...
- **5e Template updates**
    - All templates:
        - Added `books`: Abbreviated source book list
        - Added `sourcesWithFootnote`: Primary source with additional sources as footnotes
        - Image display (see [image examples](examples/templates/tools5e))
            - hasImages - true if any images are present
            - hasMoreImages - true if more than one image is present
            - showAllImages - rendered wikilinks for all images. If there is more than one, they will be shown as a gallery.
            - showMoreImages - rendered wikilinks for all but the first image. If there is more than one, they will be shown as a gallery.
            - showPortraitImage - rendered wikilink for the first image with the '#right' anchor tag to float it to the right side.
    - Added [Bastion template](docs/templates/dnd5e/QuteBastion/) (2024 rules)
    - Class updates:
        - Unified class progression tables
        - [Hit Point Die](docs/templates/dnd5e/QuteClass/HitPointDie.md)
        - [Starting Equipment](docs/templates/dnd5e/QuteClass/StartingEquipment.md)
        - Improved [Multiclassing](docs/templates/dnd5e/QuteClass/Multiclassing.md)
        - isClassic/classic: true if 2014 class
        - isSidekick/sidekick: true if sidekick class
    - Spells: Added `references` list linking to related content

Note: Path changes may affect existing content links. All path updates support dual-edition content structure.

## 🔖 ✨ 2.3.14: Improvements to Pathfinder rendering

@miscoined has made significant contributions to improve support for Pf2e creatures (the bestiary). 🙏🎉💖

## 🔖 🐛 2.3.13: Fallback paths for images

In the event you have a bad image reference and the copy fails, you can set a [fallback path](./docs/configuration.md#fallback-paths) for an image that should be used instead.

```json
"images": {
    "fallbackPaths": {
        "img/bestiary/MM/Green Hag.jpg": "img/bestiary/MM/Green Hag.webp"
    },
}
```

- The key (original path) must match what is used by the JSON Source.
- The value (replacement path) should be either: a valid path to a local file or a valid URL to a remote file.

## 🔖 🐛 2.3.11: Fixes for downloading images

Downloading and/or copying [internal](./docs/configuration.md#copying-internal-images) or [external](./docs/configuration.md#copying-external-images) images has been a whack-a-mole exercise with path encodings across platforms and sources. Everything should finally be fixed now.

## 🔖 ✨ 2.3.9: Fixes for Dice Roller and Statblock

If you use the Dice Roller plugin, `useDiceRoller: true` will add dice rolls to tables and in text.

If you are using the Fantasy Statblocks plugin to render your statblocks, you can now add `yamlStatblocks: true` to your configuration. This will strip backticks and other formatting related to dice rolls from statblock text (which will make Fantasy Statblocks happier), whether you have dice roller plugin enabled or not.

## 🔖 ✨ 2.3.0: 5eTools moving to mirror2

The 5eTools Mirror now uses a separate repository for images. When you run `ttrpg-convert` against 5eTools content, images will not be copied into your vault by default. Links to external images will be used instead. This can greatly reduce the size of your vault, but you will need to be connected to the internet for images to render.

See [Copying "internal" images](./docs/configuration.md#copying-internal-images) for options on how to copy tokens and other item/spell/monster fluff images into your vault.

See [Copying "external" images](./docs/configuration.md#copying-external-images) to copy additional images referenced in general text into your vault.

## 🔖 ✨ 2.2.12: 5e support for generic and magic item variants

Items may have variants, which are defined as a list in the `variants` attribute.

- Use `resource.variantAliases` to get a list of aliases for variants
- Use `resource.variantSectionLinks` to get a list of links to variant sections
- Iterate over the section list to generate sections (`##`) for each variant

See the following examples:

- [Default `item2md.txt`](src/main/resources/templates/tools5e/item2md.txt)
- [Example `examples/templates/tools5e/images-item2md.txt`](examples/templates/tools5e/images-item2md.txt)

## 🔖 ✨ 2.2.5: New templates for decks (and cards), legendary groups, and psionics

- **New templates**: `deck2md.txt`, `legendaryGroup2md.txt`, `psionic2md.txt`
    - Decks, when present, will be generated under `compendium/decks`. Cards are part of decks.
    - Legendary groups, when present, will be generated under `bestiary/legendary-groups`
    - Psionics, when present, will be generated under `compendium/psionics`.
- `feat2md.txt` is now also used for optional features.
- The default `monster2md.txt` template has been updated to embed the legendary group.
- CSS snippets have been updated to support legendary groups embedded in statblocks.

## 🔖 🔥 2.1.0: File name and path changes, template docs and attribute changes

1. 🔥 **Variant rules include the source in the file name**: this avoids duplicates (and there were some).
2. 🔥 **5eTools changed the classification for some creatures**, which moves them in the bestiary. Specifically: the Four-armed troll is a giant (and not an npc), a river serpent is a monstrosity rather than a beast, and ogre skeletons and red dracoliches are both undead.
3. 🔥 Better support for table rendering has superceded dedicated/hand-tended random name tables. All of the tables are still present, just in different units more directly related to source material.
4. 🔥 **Change to monster template attributes:** Legendary group attributes have been simplified to `name` and `desc`, just like other traits. See the [default monster template](https://github.com/ebullient/ttrpg-convert-cli/blob/0736c3929a6d90fe01860692f487b8523b57e60d/src/main/resources/templates/tools5e/monster2md.txt#L80) for an example.

> ***If you use the Templater plugin***, you can use [a templater script](https://github.com/ebullient/ttrpg-convert-cli/blob/main/migration/ttrpg-cli-renameFiles-5e-2.1.0.md) to **rename files in your vault before merging** with freshly generated content. View the contents of the template before running it, and adjust parameters at the top to match your Vault.

✨ **New template documentation** is available in [docs](docs). Content is generated from javadoc in the various *.qute packages (for template-accessible fields and methods). It may not be complete.. PRs to improve it are welcome.

## 🔖 🔥 2.0.0: File name and path changes, and styles

1. 🔥 **A leading slash (`/`) is no longer used at the beginning of compendium and root paths**. This should allow you to move these two directories around more easily.
    - I recommend that you keep the compendium and rules sections together as big balls of mud.
    - If you do want to further move files around, do so from within obsidian, so obsidian can update its links.

2. 🔥 **D&D 5e subclasses now use the source of the subclass in the file name**.

    > ***If you use the Templater plugin***, you can use [a templater script](https://github.com/ebullient/ttrpg-convert-cli/blob/main/migration/ttrpg-cli-renameFiles-2.0.0.md) to rename files in your vault before merging with freshly generated content. View the contents of the template before running it, and adjust parameters at the top to match your Vault.

3. 🎨 CSS styles for D&D 5e and Pathfinder are now available in `examples/css-snippets`.

4. 📝 Admonitions are also available for import:
    - 🎨 [admonitions-5e.json](examples/admonitions/admonitions-5e.json)
    - [admonitions-pf2e-v3.json](examples/admonitions/admonitions-pf2e-v3.json)
    - 🎨 [other-admonitions.json](examples/admonitions/other-admonitions.json)

    Note: `admonitions-5e.json` and `other-admonitions.json` use colors from CSS snippets to adjust for light and dark mode.

## 🔖 1.1.1: Dice roller in statblocks and text

If you are using the default templates and want to render dice rolls, set
`useDiceRoller` to true to use dice roller strings when replacing dice `{@dice
}`, and `{@damage }` strings. This can be set differently for either "5e" or
"pf2e" configurations. Please note that if you are using a custom template
and fantasy statblocks, you do **not** need to set the dice roller in your
config. Fantasy statblocks will take care of the rendering itself.

See [examples/config][] for the general structure of config.

## 🔖 1.1.0: Images for backgrounds, items, monsters, races, and spells

The conversion tool downloads fluff images into `img` directories within each type, e.g. `backgrounds/img` or `bestiary/aberration/img`. These images are unordered, and are not referenced in entry text. Templates must be modified to include them.

To display all images, you can do something like this:

```md
{#each resource.fluffImages}![{it.caption}]({it.path})  
{/each}
```

Note that the line above ends with two spaces, which serves as a line break when you have strict line endings enabled. You may need something a little different to get things to wrap the way you want in the case that there are multiple images (which is infrequent for these types).

You can also use two separate blocks, such that the first image is used at the top of the document, and any others are included later:

```md
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
- This configuration is in the [compendium.css snippet][ex-snippets].
- There is an example for each type in the [example templates directory][ex-templates] directory. Relevant file names start with `images-`.

## 🔖  1.0.18: You can put more things in json input now

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

## 🔖  1.0.16: Sections in Spell text

Text for changes to spells at higher levels is added to spells a little differently depending on how complicated the spell is.

Some spells effectively have subsections. Create or Destroy Water, from the PHB, has one subsection describing how water is created, and another describing how it is destroyed. In many layouts, there is just a bit of bold text to visually highlight this information. I've opted to make these proper sections (with a heading) instead, because you can then embed/transclude just the variant you want into your notes where that is relevant.

If a spell has sections, then "At Higher Levels" will be added as an additional section. Otherwise, it will be appended with `**At Higher Levels.**` as leading eyecatcher text.

The [default spell template (spell2md.txt)][def-templates] will test for sections in the spell text, and if so, now inserts a `## Summary` header above the Classes/Sources information, to ensure that the penultimate section can be embedded cleanly.

## 🔖  1.0.15: Flowcharts, optfeature in text, styled rows

- `optfeature` text is rendered (Tortle package)
- `flowcharts` is rendered as a series of `flowchart` callouts  
    Use the admonition plugin to create a custom `flowchart` callout with an icon of your choice.
- The adventuring gear tables from the PHB have been corrected

## 🔖  1.0.14: Ability Scores

As shown in [monster2md-scores.txt][ex-templates], you can now access ability scores directly to achieve alternate layouts in templates, for example:

```md
- STR: {resource.scores.str} `dice: 1d20 {resource.scores.strMod}`
- DEX: {resource.scores.dex} `dice: 1d20 {resource.scores.dexMod}`
- CON: {resource.scores.con} `dice: 1d20 {resource.scores.conMod}`
- INT: {resource.scores.int} `dice: 1d20 {resource.scores.intMod}`
- WIS: {resource.scores.wis} `dice: 1d20 {resource.scores.wisMod}`
- CHA: {resource.scores.cha} `dice: 1d20 {resource.scores.chaMod}`
```

## 🔖 1.0.13: Item property tags are now sorted

Property tags on items are now sorted (not alphabetically) to stabilize their order in generated files. This should be a one-time bit of noise as you cross this release (using a version before to using some version after).

## 🔖 🔥 1.0.12: File name and image reference changes

### 🔥 File name changes

Each file name will now contain an abbreviation of the primary source to avoid conflicts (for anything that does not come from phb, mm, dmg).

***If you use the Templater plugin***, you can use [a templater script](https://github.com/ebullient/ttrpg-convert-cli/blob/main/migration/json5e-cli-renameFiles-1.0.12.md) to rename files in your vault before merging with freshly generated content. View the contents of the template before running it, and adjust parameters at the top as necessary.

### 🔥 1.0.12: Deity symbols and Bestiary tokens

Symbols and tokens have changed in structure. Custom templates will need a bit of adjustment.

For bestiary tokens:

```md
{#if resource.token}
![{resource.token.caption}]({resource.token.path}#token){/if}
```

For deities:

```md
{#if resource.image}
![{resource.image.caption}]({resource.image.path}#symbol){/if}
```
