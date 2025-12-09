# QuteDeity

5eTools deity attributes (`deity2md.txt`)

Extension of [Tools5eQuteBase](Tools5eQuteBase.md).

## Attributes

[alignment](#alignment), [altNames](#altnames), [books](#books), [category](#category), [domains](#domains), [fluffImages](#fluffimages), [getAliases](#getaliases), [hasImages](#hasimages), [hasMoreImages](#hasmoreimages), [hasSections](#hassections), [image](#image), [labeledSource](#labeledsource), [name](#name), [pantheon](#pantheon), [province](#province), [reprintOf](#reprintof), [showAllImages](#showallimages), [showMoreImages](#showmoreimages), [showPortraitImage](#showportraitimage), [source](#source), [sourceAndPage](#sourceandpage), [sourcesWithFootnote](#sourceswithfootnote), [symbol](#symbol), [tags](#tags), [text](#text), [title](#title), [vaultPath](#vaultpath)

### alignment

Alignment of this deity

### altNames


### books

List of source books using abbreviated name. Fantasy statblocks uses this list format, as an example.

### category

Category of this deity: Lesser Idols, Prime Deities

### domains

Category of this deity: Nature, Tempest

### fluffImages

List of images as [ImageRef](../ImageRef.md) (optional)

### getAliases

Aliases for this note, including the note name, as quoted/escaped strings.

Example values:
- "+1 All-Purpose Tool"
- "Carl \"The Elder\" Frost"

In templates:
```md
aliases:
{#each resource.aliases}
- {it}
{/each}
```

### hasImages

Return true if any images are present

### hasMoreImages

Return true if more than one image is present

### hasSections

True if the content (text) contains sections

### image

Image or symbol representing this deity (as [ImageRef](../ImageRef.md))

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### name

Note name

### pantheon

Pantheon to which this deity belongs: Celtic

### province

Province of this deity: Discovery, Luck, Storms, Travel, ...

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

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../SourceAndPage.md)

### sourcesWithFootnote

Get Sources as a footnote.

Calling this method will return an italicised string with the primary source
followed by a footnote listing all other sources. Useful for types
that tend to have many sources.

### symbol

Text description of deity's symbol: Wave of white water on green

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### title

Title of this deity

### vaultPath

Path to this note in the vault
