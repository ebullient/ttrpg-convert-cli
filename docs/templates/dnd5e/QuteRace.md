# QuteRace

5eTools race attributes (`race2md.txt`)

Extension of [Tools5eQuteBase](Tools5eQuteBase.md).

## Attributes

[ability](#ability), [books](#books), [description](#description), [fluffImages](#fluffimages), [hasImages](#hasimages), [hasMoreImages](#hasmoreimages), [hasSections](#hassections), [labeledSource](#labeledsource), [name](#name), [reprintOf](#reprintof), [showAllImages](#showallimages), [showMoreImages](#showmoreimages), [showPortraitImage](#showportraitimage), [size](#size), [source](#source), [sourceAndPage](#sourceandpage), [sourcesWithFootnote](#sourceswithfootnote), [speed](#speed), [spellcasting](#spellcasting), [tags](#tags), [text](#text), [traits](#traits), [type](#type), [vaultPath](#vaultpath)

### ability

Ability scores associated with this race (comma-separated list of scores or choices)

### books

List of source books using abbreviated name. Fantasy statblocks uses this list format, as an example.

### description

Formatted text describing the race. Optional. Same as {resource.text}

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

### name

Note name

### reprintOf

List of content superceded by this note (as [Reprinted](../Reprinted.md))

### showAllImages

Return embedded wikilinks for all images
If there is more than one, they will be displayed in a gallery.

### showMoreImages

Return embedded wikilinks for all but the first image
If there is more than one, they will be displayed in a gallery.

### showPortraitImage

Return an embedded wikilink to the first image
Will have the "right" anchor tag.

### size

Size: Small or Medium

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../SourceAndPage.md)

### sourcesWithFootnote

Get Sources as a footnote.

Calling this method will return an italicised string with the primary source
followed by a footnote listing all other sources. Useful for types
that tend to have many sources.

### speed

Speed: 30 ft. May include additional values, like flight or swim speed.

### spellcasting

Spellcasting ability score

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### traits

Formatted text with subsections describing racial traits

### type

type of race or subrace (humanoid, ooze, undead, etc.)

### vaultPath

Path to this note in the vault
