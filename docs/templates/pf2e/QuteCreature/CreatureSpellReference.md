# CreatureSpellReference

A spell known by the creature. <blockquote> [shadow siphon](#) (acid only) (×2) </blockquote>

## Attributes

[amount](#amount), [link](#link), [name](#name), [notes](#notes)


### name

The name of the spell

### link

A formatted link to the spell's note, or just the spell's name if we couldn't get a link.

### amount

The number of casts available for this spell. A value of 0 represents an at will spell. Use [CreatureSpellReference#formattedAmount()](CreatureSpellReference.md#formattedAmount()) to get this as a formatted string.

### notes

Any notes associated with this spell, e.g. "at will only"
