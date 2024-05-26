# QuteAction

Pf2eTools Action attributes (`action2md.txt`)

Extension of [Pf2eQuteBase](../Pf2eQuteBase.md)

## Attributes

[actionType](#actiontype), [activity](#activity), [aliases](#aliases), [basic](#basic), [cost](#cost), [frequency](#frequency), [hasSections](#hassections), [item](#item), [labeledSource](#labeledsource), [name](#name), [prerequisites](#prerequisites), [requirements](#requirements), [source](#source), [sourceAndPage](#sourceandpage), [tags](#tags), [text](#text), [traits](#traits), [trigger](#trigger), [vaultPath](#vaultpath)


### actionType

Type of action (as [ActionType](ActionType.md))

### activity

Activity/Activation cost (as [QuteDataActivity](../QuteDataActivity.md))

### aliases

Aliases for this note

### basic

True if this is a basic action. Same as `{resource.actionType.basic}`.

### cost

The cost of using this action

### frequency

[QuteDataFrequency](../QuteDataFrequency.md). How often this action can be used/activated. Use directly to get a formatted string.

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

### requirements

Situational requirements for performing this action

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../../SourceAndPage.md)

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
