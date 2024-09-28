# QuteAffliction

Pf2eTools Affliction attributes (inline/embedded, `inline-affliction2md.txt`)

Extension of [Pf2eQuteNote](../Pf2eQuteNote.md)

## Attributes

[aliases](#aliases), [category](#category), [effect](#effect), [hasSections](#hassections), [isEmbedded](#isembedded), [labeledSource](#labeledsource), [level](#level), [maxDuration](#maxduration), [name](#name), [notes](#notes), [onset](#onset), [reprintOf](#reprintof), [savingThrow](#savingthrow), [source](#source), [sourceAndPage](#sourceandpage), [stages](#stages), [tags](#tags), [temptedCurse](#temptedcurse), [text](#text), [traits](#traits), [vaultPath](#vaultpath)


### aliases

Aliases for this note. Only populated if not embedded.

### category

Category of affliction (Curse or Disease). Usually shown alongside the level.

### effect

Formatted text. Affliction effect, may be multiple lines.

### hasSections

True if the content (text) contains sections

### isEmbedded

If true, then this affliction is embedded into a larger note.

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### level

Integer from 1 to 10. Level of the affliction.

### maxDuration

Formatted text. Maximum duration of the infliction.

### name

Note name

### notes

Any additional notes associated with the affliction.

### onset

Formatted text. Maximum duration of the infliction.

### reprintOf

List of content superceded by this note (as [Reprinted](../../Reprinted.md))

### savingThrow

The saving throw required to not contract or advance the affliction as
[QuteAfflictionSave](QuteAfflictionSave.md)

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../../SourceAndPage.md)

### stages

Affliction stages: map of name to stage data as
[QuteAfflictionStage](QuteAfflictionStage.md)

### tags

Collected tags for inclusion in frontmatter

### temptedCurse

A description of the tempted version of the curse

### text

Formatted text. For most templates, this is the bulk of the content.

### traits

Collection of traits (decorated links)

### vaultPath

Path to this note in the vault
