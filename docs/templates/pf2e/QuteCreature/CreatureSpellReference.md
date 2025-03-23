# CreatureSpellReference

A spell known by the creature.

```md
[shadow siphon](#) (acid only) (×2)
```

## Attributes

[amount](#amount), [link](#link), [name](#name), [notes](#notes)

### amount

The number of casts available for this spell. A value of 0 represents an at will spell. Use
[CreatureSpellReference#formattedAmount](#formattedamount) to get this as a formatted string.

### link

A formatted link to the spell's note, or just the spell's name if we couldn't get a link.

### name

The name of the spell

### notes

Any notes associated with this spell, e.g. "at will only"
