# QuteItem

5eTools item attributes (`item2md.txt`)

Extension of [Tools5eQuteBase](../Tools5eQuteBase.md).

## Attributes

[armorClass](#armorclass), [books](#books), [cost](#cost), [costCp](#costcp), [damage](#damage), [damage2h](#damage2h), [detail](#detail), [fluffImages](#fluffimages), [hasImages](#hasimages), [hasMoreImages](#hasmoreimages), [hasSections](#hassections), [labeledSource](#labeledsource), [mastery](#mastery), [name](#name), [prerequisite](#prerequisite), [properties](#properties), [range](#range), [reprintOf](#reprintof), [rootVariant](#rootvariant), [showAllImages](#showallimages), [showMoreImages](#showmoreimages), [showPortraitImage](#showportraitimage), [source](#source), [sourceAndPage](#sourceandpage), [sourcesWithFootnote](#sourceswithfootnote), [stealthPenalty](#stealthpenalty), [strengthRequirement](#strengthrequirement), [subtypeString](#subtypestring), [tags](#tags), [text](#text), [variantAliases](#variantaliases), [variantSectionLinks](#variantsectionlinks), [variants](#variants), [vaultPath](#vaultpath), [weight](#weight)


### armorClass

Changes to armor class provided by the item, if applicable

### books

List of source books using abbreviated name. Fantasy statblocks uses this list format, as an example.

### cost

Cost of the item (gp, sp, cp). Optional.

### costCp

Cost of the item (cp) as number. Optional.

### damage

One-handed Damage string, if applicable. Contains dice formula and damage type

### damage2h

Two-handed Damage string, if applicable. Contains dice formula and damage type

### detail

Formatted string of item details. Will include some combination of tier, rarity, category, and attunement

### fluffImages

List of images as [ImageRef](../../ImageRef.md) (optional)

### hasImages

Return true if any images are present

### hasMoreImages

Return true if more than one image is present

### hasSections

True if the content (text) contains sections

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### mastery

Formatted string listing applicable item mastery (with links to rules if the source is present)

### name

Note name

### prerequisite

Formatted text listing other prerequisite conditions (optional)

### properties

Formatted string listing item's properties (with links to rules if the source is present)

### range

Item's range, if applicable

### reprintOf

List of content superceded by this note (as [Reprinted](../../Reprinted.md))

### rootVariant

Detailed information about this item as [Variant](Variant.md)

### showAllImages

Return embedded wikilinks for all images
If there is more than one, they will be displayed in a gallery.

### showMoreImages

Return embedded wikilinks for all but the first image
If there is more than one, they will be displayed in a gallery.

### showPortraitImage

Return an embedded wikilink to the first image
Will have the "right" anchor tag.

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../../SourceAndPage.md)

### sourcesWithFootnote

Get Sources as a footnote.

Calling this method will return an italicised string with the primary source
followed by a footnote listing all other sources. Useful for types
that tend to have many sources.

### stealthPenalty

True if the item imposes a stealth penalty, if applicable

### strengthRequirement

Strength requirement as a numerical value, if applicable

### subtypeString

Formatted string of additional item attributes. Optional.

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### variantAliases

String: list (`- "alias"`) of aliases for variants. Use in YAML frontmatter with `aliases:`.
Will return an empty string if there are no variants

### variantSectionLinks

String: list (`- [name](#anchor)`) of links to variant sections.
Will return an empty string if there are no variants.

### variants

List of magic item variants as [Variant](Variant.md). Optional.

### vaultPath

Path to this note in the vault

### weight

Weight of the item (pounds) as a decimal value
