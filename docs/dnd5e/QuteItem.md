# QuteItem

5eTools item attributes (`item2md.txt`)

Extension of [Tools5eQuteBase](Tools5eQuteBase.md).

## Attributes

[armorClass](#armorclass), [cost](#cost), [damage](#damage), [damage2h](#damage2h), [detail](#detail), [fluffImages](#fluffimages), [hasSections](#hassections), [labeledSource](#labeledsource), [name](#name), [prerequisite](#prerequisite), [properties](#properties), [range](#range), [source](#source), [sourceAndPage](#sourceandpage), [stealthPenalty](#stealthpenalty), [strengthRequirement](#strengthrequirement), [tags](#tags), [text](#text), [variantAliases](#variantaliases), [variantSectionLinks](#variantsectionlinks), [variants](#variants), [vaultPath](#vaultpath), [weight](#weight)


### armorClass

Changes to armor class provided by the item, if applicable

### cost

Cost of the item (gp, sp, cp). Usually missing for magic items.

### damage

One-handed Damage string, if applicable. Contains dice formula and damage type

### damage2h

Two-handed Damage string, if applicable. Contains dice formula and damage type

### detail

Item details: tier, rarity, category, attunement

### fluffImages

List of images for this item (as [ImageRef](../ImageRef.md))

### hasSections

True if the content (text) contains sections

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### name

Note name

### prerequisite

Formatted text listing other prerequisite conditions (optional)

### properties

List of item's properties (with links to rules if the source is present)

### range

Item's range, if applicable

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../SourceAndPage.md)

### stealthPenalty

True if the item imposes a stealth penalty, if applicable

### strengthRequirement

Strength requirement as a numerical value, if applicable

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### variantAliases

String: list (`- "alias"`) of aliases for variants. Use in YAML frontmatter with `aliases:`. Will return an empty string if there are no variants

### variantSectionLinks

String: list (`- [name](#anchor)`) of links to variant sections. Will return an empty string if there are no variants.

### variants

List of magic item variants (as [Variant](QuteItem/Variant.md), optional)

### vaultPath

Path to this note in the vault

### weight

Weight of the item (pounds) as a decimal value
