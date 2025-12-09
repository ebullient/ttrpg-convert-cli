# QuteNote

Common attributes for simple notes. THese attributes are more
often used by books, adventures, rules, etc.

Notes created from `QuteNote` (or a derivative) will look for a template
named `note2md.txt` by default.

## Attributes

[books](#books), [getAliases](#getaliases), [hasSections](#hassections), [labeledSource](#labeledsource), [name](#name), [reprintOf](#reprintof), [source](#source), [sourceAndPage](#sourceandpage), [sourcesWithFootnote](#sourceswithfootnote), [tags](#tags), [text](#text), [vaultPath](#vaultpath)

### books

List of source books using abbreviated name. Fantasy statblocks uses this list format, as an example.

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

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### name

Note name

### reprintOf

List of content superceded by this note (as [Reprinted](Reprinted.md))

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](SourceAndPage.md)

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
