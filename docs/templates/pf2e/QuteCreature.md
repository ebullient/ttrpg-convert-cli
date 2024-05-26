# QuteCreature

Pf2eTools Creature attributes (`creature2md.txt`)

Use `%%--` to mark the end of the preamble (frontmatter and other leading content only appropriate to the standalone case).

Extension of [Pf2eQuteBase](Pf2eQuteBase.md)

## Attributes

[abilities](#abilities), [abilityMods](#abilitymods), [aliases](#aliases), [attacks](#attacks), [defenses](#defenses), [description](#description), [hasSections](#hassections), [items](#items), [labeledSource](#labeledsource), [languages](#languages), [level](#level), [name](#name), [perception](#perception), [senses](#senses), [skills](#skills), [source](#source), [sourceAndPage](#sourceandpage), [speed](#speed), [tags](#tags), [text](#text), [traits](#traits), [vaultPath](#vaultpath)


### abilities

The creature's abilities, as a [CreatureAbilities](QuteCreature/CreatureAbilities.md).

### abilityMods

Ability modifiers as a map of (name, modifier)

### aliases

Aliases for this note (optional)

### attacks

The creature's attacks, as a list of [QuteInlineAttack](QuteInlineAttack.md)

### defenses

Defenses (AC, saves, etc) as [QuteDataDefenses](QuteDataDefenses.md)

### description

Short creature description (optional)

### hasSections

True if the content (text) contains sections

### items

Items held by the creature as a list of strings

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### languages

Languages as [CreatureLanguages](QuteCreature/CreatureLanguages.md)

### level

Creature level (number, optional)

### name

Note name

### perception

Creature perception (number, optional)

### senses

Senses as a list of [CreatureSense](QuteCreature/CreatureSense.md)

### skills

Skill bonuses as [CreatureSkills](QuteCreature/CreatureSkills.md)

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../SourceAndPage.md)

### speed

The creature's speed, as an [QuteDataSpeed](QuteDataSpeed.md)

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### traits

Collection of traits (decorated links, optional)

### vaultPath

Path to this note in the vault
