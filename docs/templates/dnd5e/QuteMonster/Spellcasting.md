# Spellcasting

5eTools creature spellcasting attributes.

This data object provides a default mechanism for creating
a marked up string based on the attributes that are present.

To use it, reference it directly:

```md
{#for spellcasting in resource.spellcasting}
{spellcasting}
{/for}
```

or, using `{#each}` instead:

```md
{#each resource.spellcasting}
{it}
{/each}
```

## Attributes

[ability](#ability), [desc](#desc), [displayAs](#displayas), [fixed](#fixed), [footerEntries](#footerentries), [headerEntries](#headerentries), [hidden](#hidden), [name](#name), [spells](#spells), [variable](#variable)

### ability


### desc

Formatted description: renders all attributes (except name) unless the trait is hidden

### displayAs

Attribute should be displayed as specified trait type. Values: `trait` (default), `action`, `bonus`, `reaction`,
`legendary`

### fixed

Spells (links) that can be cast a fixed number of times (constant), at will (will), or as a ritual

### footerEntries

Formatted text that should be printed after the list of spells

### headerEntries

Formatted text that should be printed before the list of spells

### hidden

Groups that should be hidden. Values: `constant`, `will`, `rest`, `restLong`, `daily`, `weekly`, `monthly`, `yearly`,
`ritual`, `spells`, `charges`, `recharge`, `legendary`

### name

Name: "Spellcasting" or "Innate Spellcasting"

### spells

Map: key = spell level, value: spell level information as
[Spells](Spells.md)

### variable

Map of frequency to spells (links).

Frequencies (key)
- charges
- daily
- legendary
- monthly
- recharge
- rest
- restLong
- weekly
- yearly

Value is another map containing additional key/value pairs, where the key is a number,
and the value is a list of spells (links).

If the key ends with `e` (like `1e` or `2e`), each will be appended, e.g. "1/day each"
to specify that each spell can be cast once per day.
