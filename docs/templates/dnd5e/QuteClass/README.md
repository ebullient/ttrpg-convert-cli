# QuteClass

5eTools class attributes (`class2md.txt`)

Extension of [Tools5eQuteBase](../Tools5eQuteBase.md).

## Attributes

[classProgression](#classprogression), [fluffImages](#fluffimages), [hasImages](#hasimages), [hasMoreImages](#hasmoreimages), [hasSections](#hassections), [hitDice](#hitdice), [hitPointDie](#hitpointdie), [hitRollAverage](#hitrollaverage), [labeledSource](#labeledsource), [multiclassing](#multiclassing), [name](#name), [primaryAbility](#primaryability), [reprintOf](#reprintof), [showAllImages](#showallimages), [showMoreImages](#showmoreimages), [showPortraitImage](#showportraitimage), [source](#source), [sourceAndPage](#sourceandpage), [startingEquipment](#startingequipment), [tags](#tags), [text](#text), [vaultPath](#vaultpath)


### classProgression

Formatted callout containing class and feature progressions.

### fluffImages

List of images as [ImageRef](../../ImageRef.md) (optional)

### hasImages

Return true if any images are present

### hasMoreImages

Return true if more than one image is present

### hasSections

True if the content (text) contains sections

### hitDice

Hit dice for this class as a single digit: 8

### hitPointDie

Hit point die for this class as
[HitPointDie](HitPointDie.md)

### hitRollAverage

Average Hit dice roll as a single digit

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### multiclassing

Multiclassing requirements and proficiencies for this class as
[Multiclassing](Multiclassing.md)

### name

Note name

### primaryAbility

Formatted string describing the primary abilities for this class

### reprintOf

List of content superceded by this note (as [Reprinted](../../Reprinted.md))

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

### startingEquipment

Formatted text describing starting equipment as
[StartingEquipment](StartingEquipment.md)

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### vaultPath

Path to this note in the vault
