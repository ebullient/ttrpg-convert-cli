# QuteRitual

Pf2eTools Ritual attributes (`ritual2md.txt`)

Extension of [Pf2eQuteBase](../Pf2eQuteBase.md)

## Attributes

[aliases](#aliases), [books](#books), [casting](#casting), [checks](#checks), [duration](#duration), [hasSections](#hassections), [heightened](#heightened), [labeledSource](#labeledsource), [level](#level), [name](#name), [reprintOf](#reprintof), [requirements](#requirements), [ritualType](#ritualtype), [source](#source), [sourceAndPage](#sourceandpage), [sourcesWithFootnote](#sourceswithfootnote), [tags](#tags), [targeting](#targeting), [text](#text), [traits](#traits), [vaultPath](#vaultpath)


### aliases

Aliases for this note

### books

List of source books using abbreviated name. Fantasy statblocks uses this list format, as an example.

### casting

Casting attributes as [QuteRitualCasting](QuteRitualCasting.md)

### checks

Casting attributes as [QuteRitualChecks](QuteRitualChecks.md)

### duration

Formated text. Ritual duration

### hasSections

True if the content (text) contains sections

### heightened

Heightened spell effects as a list of [Traits](../../NamedText.md)

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### level

A spell’s overall power, from 1 to 10.

### name

Note name

### reprintOf

List of content superceded by this note (as [Reprinted](../../Reprinted.md))

### requirements

Formatted text. Ritual requirements

### ritualType

Type: Ritual (usually)

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

### targeting

Casting attributes as [QuteSpellTarget](../QuteSpell/QuteSpellTarget.md)

### text

Formatted text. For most templates, this is the bulk of the content.

### traits

Collection of traits (decorated links)

### vaultPath

Path to this note in the vault
