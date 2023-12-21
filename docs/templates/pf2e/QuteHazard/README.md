# QuteHazard

Pf2eTools Hazard attributes (`hazard2md.txt`)

Hazards are rendered both standalone and inline (as an admonition block). The default template can render both. It contains some special syntax to handle the inline case.

Use `%%--` to mark the end of the preamble (frontmatter and other leading content only appropriate to the standalone case).

Extension of [Pf2eQuteBase](../Pf2eQuteBase.md)

## Attributes

[abilities](#abilities), [actions](#actions), [complexity](#complexity), [defenses](#defenses), [disable](#disable), [hasSections](#hassections), [labeledSource](#labeledsource), [level](#level), [name](#name), [perception](#perception), [reset](#reset), [routine](#routine), [routineAdmonition](#routineadmonition), [source](#source), [sourceAndPage](#sourceandpage), [stealth](#stealth), [tags](#tags), [text](#text), [traits](#traits), [vaultPath](#vaultpath)


### abilities


### actions


### complexity


### defenses


### disable


### hasSections

True if the content (text) contains sections

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### level


### name

Note name

### perception


### reset


### routine


### routineAdmonition


### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../../SourceAndPage.md)

### stealth


### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### traits

Collection of traits (decorated links)

### vaultPath

Path to this note in the vault
