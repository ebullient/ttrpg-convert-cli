# AcHp

5eTools armor class and hit points attributes

This data object provides a default mechanism for creating a marked up string based on the attributes that are present. To use it, reference it directly.

## Attributes

[ac](#ac), [acText](#actext), [hitDice](#hitdice), [hp](#hp), [hpText](#hptext)


### ac

Armor class (number)

### acText

Additional armor class text. May link to related items

### hitDice

Hit dice formula as formatted string: `7d10 + 14` (creatures)

### hp

Hit points. If using the dice roller plugin is enabled, this will be a dice roll formula.

### hpText

Additional hit point text. In the case of summoned creatures, this will contain notes for how hit points should be calculated relative to the player's modifiers.
