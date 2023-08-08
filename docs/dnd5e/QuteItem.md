# QuteItem

5eTools item attributes (`item2md.txt`)

Extension of [Tools5eQuteBase](Tools5eQuteBase.md).

## Attributes

[armorClass](#armorclass), [cost](#cost), [damage](#damage), [damage2h](#damage2h), [detail](#detail), [fluffImages](#fluffimages), [hasSections](#hassections), [name](#name), [properties](#properties), [range](#range), [source](#source), [stealthPenalty](#stealthpenalty), [strengthRequirement](#strengthrequirement), [tags](#tags), [text](#text), [vaultPath](#vaultpath), [weight](#weight)


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

### name

Note name

### properties

List of item's properties (with links to rules if the source is present)

### range

Item's range, if applicable

### source

Formatted string describing the content's source(es)

### stealthPenalty

True if the item imposes a stealth penalty, if applicable

### strengthRequirement

Strength requirement as a numerical value, if applicable

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### vaultPath

Path to this note in the vault

### weight

Weight of the item (pounds) as a decimal value
