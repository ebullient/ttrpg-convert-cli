# QuteSpell

5eTools spell attributes (`spell2md.txt`)

Extension of [Tools5eQuteBase](Tools5eQuteBase.md).

## Attributes

[classList](#classlist), [classes](#classes), [components](#components), [duration](#duration), [fluffImages](#fluffimages), [hasImages](#hasimages), [hasMoreImages](#hasmoreimages), [hasSections](#hassections), [labeledSource](#labeledsource), [level](#level), [name](#name), [range](#range), [reprintOf](#reprintof), [ritual](#ritual), [school](#school), [showAllImages](#showallimages), [showMoreImages](#showmoreimages), [showPortraitImage](#showportraitimage), [source](#source), [sourceAndPage](#sourceandpage), [tags](#tags), [text](#text), [time](#time), [vaultPath](#vaultpath)


### classList

List of class names that can use this spell. May be incomplete or empty.

### classes

String: rendered list of links to classes that can use this spell. May be incomplete or empty.

### components

Formatted: spell components

### duration

Formatted: spell range

### fluffImages

List of images as [ImageRef](../ImageRef.md) (optional)

### hasImages

Return true if any images are present

### hasMoreImages

Return true if more than one image is present

### hasSections

True if the content (text) contains sections

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### level

Spell level

### name

Note name

### range

Formatted: spell range

### reprintOf

List of content superceded by this note (as [Reprinted](../Reprinted.md))

### ritual

true for ritual spells

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

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### time

Formatted: casting time

### vaultPath

Path to this note in the vault
