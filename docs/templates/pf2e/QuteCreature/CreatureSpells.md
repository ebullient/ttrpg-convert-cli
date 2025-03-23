# CreatureSpells

A collection of spells with some additional information.

```md
**Cantrips (9th)** [daze](#), [shadow siphon](#) (acid only) (×2)
```

```md
**4th** [confusion](#), [phantasmal killer](#) (2 slots)
```

## Attributes

[cantripRank](#cantriprank), [cantrips](#cantrips), [knownRank](#knownrank), [slots](#slots), [spells](#spells)

### cantripRank

The rank that these spells are auto-heightened to. Present only for cantrips.

### cantrips

True if these are cantrip spells

### knownRank

The rank that these spells are known at (0 for cantrips). May be absent for rituals.

### slots

The number of slots available for these spells. Not present for constant spells or rituals.

### spells

A list of spells, as a list of [CreatureSpellReference](CreatureSpellReference.md)
