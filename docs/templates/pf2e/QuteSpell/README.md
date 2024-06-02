# QuteSpell

Pf2eTools Spell attributes (`spell2md.txt`)

Extension of [Pf2eQuteBase](../Pf2eQuteBase.md)

## Attributes

[aliases](#aliases), [amp](#amp), [castDuration](#castduration), [components](#components), [cost](#cost), [domains](#domains), [duration](#duration), [hasSections](#hassections), [heightened](#heightened), [labeledSource](#labeledsource), [level](#level), [name](#name), [requirements](#requirements), [save](#save), [source](#source), [sourceAndPage](#sourceandpage), [spellLists](#spelllists), [spellType](#spelltype), [subclass](#subclass), [tags](#tags), [targeting](#targeting), [text](#text), [traditions](#traditions), [traits](#traits), [trigger](#trigger), [vaultPath](#vaultpath)


### aliases

Aliases for this note

### amp

Psi amp behavior as [QuteSpellAmp](QuteSpellAmp.md)

### castDuration

The time it takes to cast the spell, as a [QuteDataDuration](../QuteDataDuration.md) which is either a [QuteDataActivity](../QuteDataActivity.md) or a [QuteDataTimedDuration](../QuteDataTimedDuration.md).

### components

The required spell components as a list of formatted strings (maybe empty). Use [QuteSpell#formattedComponents()](../QuteSpell.md#formattedComponents()) to get a pre-formatted representation.

### cost

The material cost of the spell as a formatted string (optional)

### domains

List of spell domains (links)

### duration

Spell duration, as [QuteDataTimedDuration](../QuteDataTimedDuration.md)

### hasSections

True if the content (text) contains sections

### heightened

Heightened spell effects as a list of [NamedText](../../NamedText.md)

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### level

A spell’s overall power, from 1 to 10.

### name

Note name

### requirements

The requirements to cast the spell (optional)

### save

Spell save, as [QuteSpellSave](QuteSpellSave.md)

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../../SourceAndPage.md)

### spellLists

Spell lists containing this spell

### spellType

Type: spell, cantrip, or focus

### subclass

List of category (Bloodline or Mystery) to Subclass (Sorcerer or Oracle). Link to class (if present) as a list of [NamedText](../../NamedText.md).

### tags

Collected tags for inclusion in frontmatter

### targeting

Spell target attributes as [QuteSpellTarget](QuteSpellTarget.md)

### text

Formatted text. For most templates, this is the bulk of the content.

### traditions

List of spell traditions (trait links)

### traits

Collection of traits (decorated links)

### trigger

The activation trigger for the spell as a formatted string (optional)

### vaultPath

Path to this note in the vault
