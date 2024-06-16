# CreatureSpellcasting

Information about a type of spellcasting available to this creature.

## Attributes

[attackBonus](#attackbonus), [constantRanks](#constantranks), [customName](#customname), [dc](#dc), [focusPoints](#focuspoints), [notes](#notes), [preparation](#preparation), [ranks](#ranks), [tradition](#tradition)


### customName

A custom name for this set of spells, e.g. "Champion Devotion Spells". Use [CreatureSpellcasting#name()](CreatureSpellcasting.md#name()) to get a name which takes this into account if it exists.

### preparation

The type of preparation for these spells, as a [SpellcastingPreparation](SpellcastingPreparation.md)

### tradition

The tradition for these spells, as a [SpellcastingTradition](SpellcastingTradition.md)

### focusPoints

The number of focus points available to this creature for these spells. Present only if these are focus spells.

### attackBonus

The spell attack bonus for these spells (integer)

### dc

The spell save DC for these spells (integer)

### notes

Any notes associated with these spells

### ranks

The spells for each rank, as a list of [CreatureSpells](CreatureSpells.md).

### constantRanks

The constant spells for each rank, as a list of [CreatureSpells](CreatureSpells.md)
