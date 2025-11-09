# QuteSpell

5eTools spell attributes (`spell2md.txt`)

Extension of [Tools5eQuteBase](Tools5eQuteBase.md).

## Attributes

[abilityChecks](#abilitychecks), [affectsCreatureTypes](#affectscreaturetypes), [areaTags](#areatags), [backgrounds](#backgrounds), [books](#books), [classList](#classlist), [classes](#classes), [components](#components), [conditionImmune](#conditionimmune), [conditionInflict](#conditioninflict), [damageImmune](#damageimmune), [damageInflict](#damageinflict), [damageResist](#damageresist), [damageVulnerable](#damagevulnerable), [duration](#duration), [feats](#feats), [fluffImages](#fluffimages), [hasImages](#hasimages), [hasMoreImages](#hasmoreimages), [hasSections](#hassections), [higherLevels](#higherlevels), [labeledSource](#labeledsource), [level](#level), [miscTags](#misctags), [name](#name), [optionalfeatures](#optionalfeatures), [races](#races), [range](#range), [references](#references), [reprintOf](#reprintof), [ritual](#ritual), [savingThrows](#savingthrows), [scalingLevelDice](#scalingleveldice), [school](#school), [showAllImages](#showallimages), [showMoreImages](#showmoreimages), [showPortraitImage](#showportraitimage), [source](#source), [sourceAndPage](#sourceandpage), [sourcesWithFootnote](#sourceswithfootnote), [spellAttacks](#spellattacks), [tags](#tags), [text](#text), [time](#time), [vaultPath](#vaultpath)

### abilityChecks

Formatted: Ability checks

### affectsCreatureTypes

Formatted: Creature types

### areaTags

Formatted/mapped: Areas

### backgrounds

String: rendered list of links to classes that grant access to this spell. May be incomplete or empty.

### books

List of source books using abbreviated name. Fantasy statblocks uses this list format, as an example.

### classList

List of class names (not links) that can use this spell.

### classes

String: rendered list of links to classes that can use this spell. May be incomplete or empty.

### components

Formatted: spell components

### conditionImmune

Formatted: Condition immunities

### conditionInflict

Formatted: Conditions

### damageImmune

Formatted: Damage immunities

### damageInflict

Formatted: Damage types

### damageResist

Formatted: Damage resistances

### damageVulnerable

Formatted: Damage vulnerabilities

### duration

Formatted: spell range

### feats

String: rendered list of links to feats that grant acccess to this spell. May be incomplete or empty.

### fluffImages

List of images as [ImageRef](../ImageRef.md) (optional)

### hasImages

Return true if any images are present

### hasMoreImages

Return true if more than one image is present

### hasSections

True if the content (text) contains sections

### higherLevels

At higher levels text

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### level

Spell level

### miscTags

Formatted/mapped: Misc tags

### name

Note name

### optionalfeatures

String: rendered list of links to optional features that grant access to this spell. May be incomplete or empty.

### races

String: rendered list of links to races that can use this spell. May be incomplete or empty.

### range

Formatted: spell range

### references

List of links to resources (classes, subclasses, feats, etc.) that have access to this spell

### reprintOf

List of content superceded by this note (as [Reprinted](../Reprinted.md))

### ritual

true for ritual spells

### savingThrows

Formatted: Saving throws

### scalingLevelDice

Formatted: Scaling damage dice entries

### school

Spell school

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

Book sources as list of [SourceAndPage](../SourceAndPage.md)

### sourcesWithFootnote

Get Sources as a footnote.

Calling this method will return an italicised string with the primary source
followed by a footnote listing all other sources. Useful for types
that tend to have many sources.

### spellAttacks

Formatted: Spell attack forms

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### time

Formatted: casting time

### vaultPath

Path to this note in the vault
