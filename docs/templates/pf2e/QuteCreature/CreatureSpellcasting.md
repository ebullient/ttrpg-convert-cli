# CreatureSpellcasting

Information about a type of spellcasting available to this creature.

## Attributes

[attackBonus](#attackbonus), [constantRanks](#constantranks), [customName](#customname), [dc](#dc), [focusPoints](#focuspoints), [formattedStats](#formattedstats), [name](#name), [notes](#notes), [preparation](#preparation), [ranks](#ranks), [tradition](#tradition)

### attackBonus

The spell attack bonus for these spells (integer)

### constantRanks

The constant spells for each rank, as a list of [CreatureSpells](CreatureSpells.md)

### customName

A custom name for this set of spells, e.g. "Champion Devotion Spells". Use
[CreatureSpellcasting#name](#name) to get a name which takes this into account
if it exists.

### dc

The spell save DC for these spells (integer)

### focusPoints

The number of focus points available to this creature for these spells. Present only if these
are focus spells.

### formattedStats

Stats for this kind of spellcasting, including the DC, attack bonus, and any focus points.

```md
DC 20, attack +25, 2 Focus Points
```

### name

The name for this set of spells. This is either the custom name, or derived from the tradition and
preparation - e.g. "Occult Prepared Spells", or "Divine Innate Spells".

### notes

Any notes associated with these spells

### preparation

The type of preparation for these spells, as a [SpellcastingPreparation](SpellcastingPreparation.md)

### ranks

The spells for each rank, as a list of [CreatureSpells](CreatureSpells.md).

### tradition

The tradition for these spells, as a [SpellcastingTradition](SpellcastingTradition.md)
