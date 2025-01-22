# QuteBastion

5eTools background attributes (`bastion2md.txt`).

Extension of [Tools5eQuteBase](../Tools5eQuteBase.md).

## Attributes

[books](#books), [fluffImages](#fluffimages), [hasImages](#hasimages), [hasMoreImages](#hasmoreimages), [hasSections](#hassections), [hirelingDescription](#hirelingdescription), [hirelings](#hirelings), [labeledSource](#labeledsource), [level](#level), [name](#name), [orders](#orders), [prerequisite](#prerequisite), [reprintOf](#reprintof), [showAllImages](#showallimages), [showMoreImages](#showmoreimages), [showPortraitImage](#showportraitimage), [source](#source), [sourceAndPage](#sourceandpage), [sourcesWithFootnote](#sourceswithfootnote), [space](#space), [spaceDescription](#spacedescription), [tags](#tags), [text](#text), [type](#type), [vaultPath](#vaultpath)


### books

List of source books using abbreviated name. Fantasy statblocks uses this list format, as an example.

### fluffImages

List of images as [ImageRef](../../ImageRef.md) (optional)

### hasImages

Return true if any images are present

### hasMoreImages

Return true if more than one image is present

### hasSections

True if the content (text) contains sections

### hirelingDescription

Hirelings as a descriptive string (if hirelings is present)

### hirelings

List of possible hirelings this bastion can have (as [Hireling](Hireling.md),
optional)

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### level

Bastion level (optional)

### name

Note name

### orders

Bastion orders (optional)

### prerequisite

Formatted text listing other prerequisite conditions (optional)

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

### sourcesWithFootnote

Get Sources as a footnote.

Calling this method will return an italicised string with the primary source
followed by a footnote listing all other sources. Useful for types
that tend to have many sources.

### space

List of possible spaces this bastion can occupy (as [Space](Space.md),
optional)

### spaceDescription

Space as a descriptive string (if space is present)

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### type

Type

### vaultPath

Path to this note in the vault
