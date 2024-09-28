# QuteAbility

Pf2eTools Ability attributes (`ability2md.txt` or `inline-ability2md.txt`).

Abilities are rendered both standalone and inline (as an admonition block).
The default template can render both. It contains some special syntax to handle
the inline case.

Use `%%--` to mark the end of the preamble (frontmatter and
other leading content only appropriate to the standalone case).

Extension of [Pf2eQuteNote](Pf2eQuteNote.md)

## Attributes

[activity](#activity), [bareTraitList](#baretraitlist), [components](#components), [cost](#cost), [embedded](#embedded), [frequency](#frequency), [hasActivity](#hasactivity), [hasAttributes](#hasattributes), [hasDetails](#hasdetails), [hasEffect](#haseffect), [hasSections](#hassections), [labeledSource](#labeledsource), [name](#name), [note](#note), [prerequisites](#prerequisites), [range](#range), [reference](#reference), [reprintOf](#reprintof), [requirements](#requirements), [source](#source), [sourceAndPage](#sourceandpage), [special](#special), [tags](#tags), [text](#text), [traits](#traits), [trigger](#trigger), [vaultPath](#vaultpath)


### activity

Ability ([activity/activation details](QuteDataActivity.md))

### bareTraitList

Return a comma-separated list of de-styled trait links (no title attributes)

### components

List of formatted strings. Activation components for this ability, e.g. command, envision

### cost

The cost of using this ability

### embedded

True if this ability is embedded in another note (admonition block).
When this is true, the `inline-ability` template is used.

### frequency

[QuteDataFrequency](QuteDataFrequency.md).
How often this ability can be used/activated. Use directly to get a formatted string.

### hasActivity

True if an activity (with text), components, or traits are present.

### hasAttributes

True if hasActivity is true, hasEffect is true or cost is present.
In other words, this is true if a list of attributes could have been rendered.

Use this to test for the end of those attributes (add whitespace or a special
character ahead of ability text)

### hasDetails

True if the ability is a short, one-line name and description.
Use this to test to choose between a detailed or simple rendering.

### hasEffect

True if frequency, trigger, and requirements are present. In other words, this is true if the ability has an effect.

### hasSections

True if the content (text) contains sections

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### name

Note name

### note

Any additional notes related to this ability that aren't included in the other fields.

### prerequisites

Formatted string. Prerequisites before this ability can be activated or taken.

### range

[QuteDataRange](QuteDataRange/README.md). The targeting range for this ability.

### reference

A formatted string which is a link to the base ability that this ability references. Embedded only.

### reprintOf

List of content superceded by this note (as [Reprinted](../Reprinted.md))

### requirements

Formatted string. Requirements for activating this ability

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../SourceAndPage.md)

### special

Special notes for this ability - usually requirements or caveats relating to its use.

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### traits

Collection of trait links. Use `{#for}` or `{#each}` to iterate over the collection.
See [traitList](#traitlist) or [bareTraitList](#baretraitlist).

### trigger

Formatted string. Trigger to activate this ability

### vaultPath

Path to this note in the vault
