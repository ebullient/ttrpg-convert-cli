# QuteAction

Pf2eTools Action attributes (`action2md.txt`)

Extension of [Pf2eQuteBase](../Pf2eQuteBase.md)

## Attributes

[actionType](#actiontype), [activity](#activity), [altNames](#altnames), [basic](#basic), [books](#books), [cost](#cost), [frequency](#frequency), [getAliases](#getaliases), [hasSections](#hassections), [item](#item), [labeledSource](#labeledsource), [name](#name), [prerequisites](#prerequisites), [reprintOf](#reprintof), [requirements](#requirements), [source](#source), [sourceAndPage](#sourceandpage), [sourcesWithFootnote](#sourceswithfootnote), [tags](#tags), [text](#text), [traits](#traits), [trigger](#trigger), [vaultPath](#vaultpath)

### actionType

Type of action (as [ActionType](ActionType.md))

### activity

Activity/Activation cost (as [QuteDataActivity](../QuteDataActivity.md))

### altNames


### basic

True if this is a basic action. Same as `{resource.actionType.basic}`.

### books

List of source books using abbreviated name. Fantasy statblocks uses this list format, as an example.

### cost

The cost of using this action

### frequency

[QuteDataFrequency](../QuteDataFrequency.md).
How often this action can be used/activated. Use directly to get a formatted string.

### getAliases

Aliases for this note, including the note name, as quoted/escaped strings.

Example values:
- "+1 All-Purpose Tool"
- "Carl \"The Elder\" Frost"

In templates:
```md
aliases:
{#each resource.aliases}
- {it}
{/each}
```

### hasSections

True if the content (text) contains sections

### item

True if this action is an item action. Same as `{resource.actionType.item}`.

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### name

Note name

### prerequisites

Prerequisite trait or characteristic for performing this action

### reprintOf

List of content superceded by this note (as [Reprinted](../../Reprinted.md))

### requirements

Situational requirements for performing this action

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../../SourceAndPage.md)

### sourcesWithFootnote

Get Sources as a footnote.

Calling this method will return an italicised string with the primary source
followed by a footnote listing all other sources. Useful for types
that tend to have many sources.

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### traits

Collection of traits (decorated links)

### trigger

Trigger for this action

### vaultPath

Path to this note in the vault
