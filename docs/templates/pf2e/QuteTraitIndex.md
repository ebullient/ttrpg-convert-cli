# QuteTraitIndex

Pf2eTools Trait index attributes (`indexTrait.md`)

This replaces the index usually generated for folders.
The default template for the trait consructs a list of links to
traits grouped by category.

Extension of [Pf2eQuteNote](Pf2eQuteNote.md)

## Attributes

[books](#books), [categoryLinks](#categorylinks), [categoryToTraits](#categorytotraits), [hasSections](#hassections), [labeledSource](#labeledsource), [name](#name), [reprintOf](#reprintof), [source](#source), [sourceAndPage](#sourceandpage), [sourcesWithFootnote](#sourceswithfootnote), [tags](#tags), [text](#text), [vaultPath](#vaultpath)

### books

List of source books using abbreviated name. Fantasy statblocks uses this list format, as an example.

### categoryLinks

List of category anchor links

### categoryToTraits

Map of category to a list of traits

### hasSections

True if the content (text) contains sections

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### name

Note name

### reprintOf

List of content superceded by this note (as [Reprinted](../Reprinted.md))

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../SourceAndPage.md)

### sourcesWithFootnote

Get Sources as a footnote.

Calling this method will return an italicised string with the primary source
followed by a footnote listing all other sources. Useful for types
that tend to have many sources.

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### vaultPath

Path to this note in the vault
