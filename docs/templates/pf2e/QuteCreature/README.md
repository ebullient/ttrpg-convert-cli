# QuteCreature

Pf2eTools Creature attributes (`creature2md.txt`)

Extension of [Pf2eQuteBase](../Pf2eQuteBase.md)

## Attributes

[abilities](#abilities), [abilityMods](#abilitymods), [aliases](#aliases), [attacks](#attacks), [defenses](#defenses), [description](#description), [hasSections](#hassections), [items](#items), [labeledSource](#labeledsource), [languages](#languages), [level](#level), [name](#name), [perception](#perception), [reprintOf](#reprintof), [ritualCasting](#ritualcasting), [senses](#senses), [skills](#skills), [source](#source), [sourceAndPage](#sourceandpage), [speed](#speed), [spellcasting](#spellcasting), [tags](#tags), [text](#text), [traits](#traits), [vaultPath](#vaultpath)


### abilities

The creature's abilities, as a
[CreatureAbilities](CreatureAbilities.md).

### abilityMods

Ability modifiers as a map of (name, modifier)

### aliases

Aliases for this note (optional)

### attacks

The creature's attacks, as a list of [QuteInlineAttack](../QuteInlineAttack/README.md)

### defenses

Defenses (AC, saves, etc) as [QuteDataDefenses](../QuteDataDefenses/README.md)

### description

Short creature description (optional)

### hasSections

True if the content (text) contains sections

### items

Items held by the creature as a list of strings

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### languages

Languages as [CreatureLanguages](CreatureLanguages.md)

### level

Creature level (number, optional)

### name

Note name

### perception

Creature perception (number, optional)

### reprintOf

List of content superceded by this note (as [Reprinted](../../Reprinted.md))

### ritualCasting

The creature's ritual casting capabilities, as a list of [CreatureRitualCasting](CreatureRitualCasting.md)

### senses

Senses as a list of [CreatureSense](CreatureSense.md)

### skills

Skill bonuses as [CreatureSkills](CreatureSkills.md)

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../../SourceAndPage.md)

### speed

The creature's speed, as an [QuteDataSpeed](../QuteDataSpeed.md)

### spellcasting

The creature's spellcasting capabilities, as a list of [CreatureSpellcasting](CreatureSpellcasting.md)

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### traits

Collection of traits (decorated links, optional)

### vaultPath

Path to this note in the vault
