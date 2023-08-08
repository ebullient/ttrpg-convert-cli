# QuteAbility

Pf2eTools Ability attributes (`ability2md.txt` or `inline-ability2md.txt`).

Abilities are rendered both standalone and inline (as an admonition block). The default template can render both. It contains some special syntax to handle the inline case.

Use `%%--` to mark the end of the preamble (frontmatter and other leading content only appropriate to the standalone case).

Extension of [Pf2eQuteNote](Pf2eQuteNote.md)

## Attributes

[activity](#activity), [bareTraitList](#baretraitlist), [components](#components), [cost](#cost), [embedded](#embedded), [frequency](#frequency), [hasActivity](#hasactivity), [hasAttributes](#hasattributes), [hasDetails](#hasdetails), [hasEffect](#haseffect), [hasSections](#hassections), [name](#name), [note](#note), [requirements](#requirements), [source](#source), [special](#special), [tags](#tags), [text](#text), [traitList](#traitlist), [traits](#traits), [trigger](#trigger), [vaultPath](#vaultpath)


### activity

Ability ([activity/activation details](QuteDataActivity.md))

### bareTraitList

Return a comma-separated list of de-styled trait links (no title attributes)

### components

Formatted string. Components required to activate this ability (embedded/inline only)

### cost

The cost of using this ability

### embedded

True if this ability is embedded in another note (admonition block). When this is true, the `inline-ability` template is used.

### frequency

How often this ability can be used/activated

### hasActivity

True if an activity (with text), components, or traits are present.

### hasAttributes

True if hasActivity is true, hasEffect is true or cost is present. In other words, this is true if a list of attributes could have been rendered. Use this to test for the end of those attributes (add whitespace or a special character ahead of ability text)

### hasDetails

True if getHasAttributes is true or special is present. In other words, this is true if there is more than just a name and text. Use this to test to choose between a detailed or simple rendering.

### hasEffect

True if frequency, trigger, and requirements are present. In other words, this is true if the ability has an effect.

### hasSections

True if the content (text) contains sections

### name

Note name

### note

Caveats related to using this ability (embedded/inline only)

### requirements

Formatted string. Requirements for activating this ability

### source

Formatted string describing the content's source(es)

### special

Special characteristics of this ability (embedded/inline only)

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### traitList

Return a comma-separated list of trait links

### traits

Collection of trait links. Use `{#for}` or `{#each}` to iterate over the collection. See [traitList](#traitList) or [bareTraitList](#bareTraitList).

### trigger

Formatted string. Trigger to activate this ability

### vaultPath

Path to this note in the vault
