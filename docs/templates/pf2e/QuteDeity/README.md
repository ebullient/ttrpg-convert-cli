# QuteDeity

Pf2eTools Deity attributes (`deity2md.txt`)

Deities are rendered both standalone and inline (as an admonition block).
The default template can render both.
It uses special syntax to handle the inline case.

Use `%%--` to mark the end of the preamble (frontmatter and
other leading content only appropriate to the standalone case).

Extension of [Pf2eQuteBase](../Pf2eQuteBase.md)

## Attributes

[alignment](#alignment), [altNames](#altnames), [anathema](#anathema), [areasOfConcern](#areasofconcern), [avatar](#avatar), [books](#books), [category](#category), [cleric](#cleric), [edicts](#edicts), [followerAlignment](#followeralignment), [getAliases](#getaliases), [hasSections](#hassections), [intercession](#intercession), [labeledSource](#labeledsource), [name](#name), [pantheon](#pantheon), [reprintOf](#reprintof), [source](#source), [sourceAndPage](#sourceandpage), [sourcesWithFootnote](#sourceswithfootnote), [tags](#tags), [text](#text), [vaultPath](#vaultpath)

### alignment


### altNames


### anathema


### areasOfConcern


### avatar


### books

List of source books using abbreviated name. Fantasy statblocks uses this list format, as an example.

### category


### cleric


### edicts


### followerAlignment


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

### hasSections

True if the content (text) contains sections

### intercession


### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### name

Note name

### pantheon


### reprintOf

List of content superceded by this note (as [Reprinted](../../Reprinted.md))

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../../SourceAndPage.md)

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
