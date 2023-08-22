# ShipAcHp

5eTools vehicle armor class and hit points attributes

This data object provides a default mechanism for creating a marked up string based on the attributes that are present. To use it, reference it directly.

## Attributes

[ac](#ac), [acText](#actext), [cost](#cost), [dt](#dt), [hitDice](#hitdice), [hp](#hp), [hpText](#hptext), [mt](#mt)


### ac

Armor class (number)

### acText

Additional armor class text. May link to related items

### cost

Cost (per unit); preformatted string

### dt

Damage threshold; number

### hitDice

Hit dice formula as formatted string: `7d10 + 14` (creatures)

### hp

Hit points. If using the dice roller plugin is enabled, this will be a dice roll formula.

### hpText

Additional hit point text. In the case of summoned creatures, this will contain notes for how hit points should be calculated relative to the player's modifiers.

### mt

Infernal War Machine mishap threshold; number
