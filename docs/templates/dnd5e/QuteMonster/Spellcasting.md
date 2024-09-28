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

[ability](#ability), [daily](#daily), [desc](#desc), [footerEntries](#footerentries), [headerEntries](#headerentries), [name](#name), [spells](#spells), [will](#will)


### ability


### daily

Map: key = nuber of times per day, value: list of spells (links)

### desc

Formatted description: renders all attributes (other than name)

### footerEntries

Formatted text that should be printed after the list of spells

### headerEntries

Formatted text that should be printed before the list of spells

### name

Name: "Spellcasting" or "Innate Spellcasting"

### spells

Map: key = spell level, value: spell level information as
[Spells](Spells.md)

### will

Spells (links) that can be cast at will
