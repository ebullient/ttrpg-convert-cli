# QuteHazard

Pf2eTools Hazard attributes (`hazard2md.txt`)

Hazards are rendered both standalone and inline (as an admonition block).
The default template can render both.
It uses special syntax to handle the inline case.

Use `%%--` to mark the end of the preamble (frontmatter and
other leading content only appropriate to the standalone case).

Extension of [Pf2eQuteBase](../Pf2eQuteBase.md)

## Attributes

[abilities](#abilities), [actions](#actions), [attacks](#attacks), [complexity](#complexity), [defenses](#defenses), [disable](#disable), [hasSections](#hassections), [labeledSource](#labeledsource), [level](#level), [name](#name), [perception](#perception), [reprintOf](#reprintof), [reset](#reset), [routine](#routine), [routineAdmonition](#routineadmonition), [source](#source), [sourceAndPage](#sourceandpage), [stealth](#stealth), [tags](#tags), [text](#text), [traits](#traits), [vaultPath](#vaultpath)


### abilities

The hazard's abilities, as a list of
[QuteAbility](../QuteAbility.md)

### actions

The hazard's actions, as a list of
[QuteAbilityOrAffliction](../QuteAbilityOrAffliction.md).

Using the elements directly will give a default rendering, but if you want more
control you can use `isAffliction` and `isAbility` to check whether it's an affliction or an
ability. Example:

```md
{#each resource.actions}
{#if it.isAffliction}

**Affliction** {it}
{#else if it.isAbility}

**Ability** {it}
{/if}
{/each}
```

### attacks

The attacks available to the hazard, as a list of
[QuteInlineAttack](../QuteInlineAttack/README.md)

### complexity


### defenses


### disable


### hasSections

True if the content (text) contains sections

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### level


### name

Note name

### perception

The hazard's perception, as a
[QuteDataGenericStat](../QuteDataGenericStat/README.md)

### reprintOf

List of content superceded by this note (as [Reprinted](../../Reprinted.md))

### reset


### routine


### routineAdmonition


### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../../SourceAndPage.md)

### stealth

The hazard's stealth, as a
[QuteHazardAttributes](QuteHazardStealth.md)

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### traits

Collection of traits (decorated links)

### vaultPath

Path to this note in the vault
