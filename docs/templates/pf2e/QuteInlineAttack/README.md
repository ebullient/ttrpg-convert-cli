# QuteInlineAttack

Pf2eTools Attack attributes (inline/embedded, `inline-attack2md.txt`)

When used directly, renders according to `inline-attack2md.txt`

## Attributes

[activity](#activity), [attackBonus](#attackbonus), [damage](#damage), [damageTypes](#damagetypes), [effects](#effects), [multilineEffect](#multilineeffect), [name](#name), [notes](#notes), [rangeType](#rangetype), [traits](#traits)


### activity

Number/type of action cost ([QuteDataActivity](../QuteDataActivity.md))

### attackBonus

The to-hit bonus for the attack (integer)

### damage

Damage if the attack hits (formatted string), e.g. "1d8 bludgeoning plus grab". This will include damage types and non-multiline effects.

### damageTypes

The damage types caused by the attack. Will be included in either [damage](../QuteInlineAttack.md#damage) or in [multilineEffect](../QuteInlineAttack.md#multilineEffect).

### effects

Any additional effects associated with the attack e.g. grab (list of strings). Effects listed here may be repeated in [damage](../QuteInlineAttack.md#damage).

### multilineEffect

A multi-line effect. Formatted string, will be null if there is no multiline effect.

### name

The name of the attack e.g. "fist" (string)

### notes

Any notes associated with the attack e.g. "no multiple attack penalty" (list of strings)

### rangeType

The range of the attack ([AttackRangeType](AttackRangeType.md) enum)

### traits

Any traits associated with the attack (collection of decorated links)
