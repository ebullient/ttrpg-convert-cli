# QuteFeat

Pf2eTools Feat attributes (`feat2md.txt`)

Feats are rendered both standalone and inline (as an admonition block).
The default template can render both.
It uses special syntax to handle the inline case.

Use `%%--` to mark the end of the preamble (frontmatter and
other leading content only appropriate to the standalone case).

Extension of [Pf2eQuteBase](Pf2eQuteBase.md)

## Attributes

[access](#access), [activity](#activity), [aliases](#aliases), [cost](#cost), [embedded](#embedded), [frequency](#frequency), [hasSections](#hassections), [labeledSource](#labeledsource), [leadsTo](#leadsto), [level](#level), [name](#name), [note](#note), [prerequisites](#prerequisites), [reprintOf](#reprintof), [requirements](#requirements), [source](#source), [sourceAndPage](#sourceandpage), [special](#special), [tags](#tags), [text](#text), [traits](#traits), [trigger](#trigger), [vaultPath](#vaultpath)


### access


### activity

Activity/Activation cost (as [QuteDataActivity](QuteDataActivity.md))

### aliases

Aliases for this note

### cost


### embedded

True if this ability is embedded in another note (admonition block).
The default template uses this flag to include a `title:` prefix for the admonition block:  

`{#if resource.embedded }title: {#else}# {/if}{resource.name}` *

### frequency

[QuteDataFrequency](QuteDataFrequency.md).
How often this feat can be used/activated. Use directly to get a formatted string.

### hasSections

True if the content (text) contains sections

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### leadsTo


### level


### name

Note name

### note


### prerequisites


### reprintOf

List of content superceded by this note (as [Reprinted](../Reprinted.md))

### requirements


### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../SourceAndPage.md)

### special


### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### traits

Collection of traits (decorated links)

### trigger


### vaultPath

Path to this note in the vault
