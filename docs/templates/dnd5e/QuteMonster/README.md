# QuteMonster

5eTools creature attributes (`monster2md.txt`)

Extension of [Tools5eQuteBase](../Tools5eQuteBase.md).

## Attributes

[5eInitiativeYaml](#5einitiativeyaml), [5eInitiativeYamlNoSource](#5einitiativeyamlnosource), [5eStatblockYaml](#5estatblockyaml), [5eStatblockYamlNoSource](#5estatblockyamlnosource), [ac](#ac), [acHp](#achp), [acText](#actext), [action](#action), [alignment](#alignment), [allTraits](#alltraits), [bonusAction](#bonusaction), [books](#books), [conditionImmune](#conditionimmune), [cr](#cr), [description](#description), [environment](#environment), [fluffImages](#fluffimages), [fullType](#fulltype), [gear](#gear), [getAliases](#getaliases), [hasImages](#hasimages), [hasMoreImages](#hasmoreimages), [hasSections](#hassections), [hitDice](#hitdice), [hp](#hp), [hpText](#hptext), [immune](#immune), [immuneResist](#immuneresist), [initiative](#initiative), [isNpc](#isnpc), [labeledSource](#labeledsource), [languages](#languages), [legendary](#legendary), [legendaryGroup](#legendarygroup), [legendaryGroupLink](#legendarygrouplink), [name](#name), [passive](#passive), [pb](#pb), [rawSpellcasting](#rawspellcasting), [reaction](#reaction), [reprintOf](#reprintof), [resist](#resist), [savesSkills](#savesskills), [savingThrows](#savingthrows), [scores](#scores), [senses](#senses), [showAllImages](#showallimages), [showMoreImages](#showmoreimages), [showPortraitImage](#showportraitimage), [size](#size), [skills](#skills), [source](#source), [sourceAndPage](#sourceandpage), [sourcesWithFootnote](#sourceswithfootnote), [speed](#speed), [spellcasting](#spellcasting), [subtype](#subtype), [tags](#tags), [text](#text), [token](#token), [trait](#trait), [type](#type), [vaultPath](#vaultpath), [vulnerable](#vulnerable)

### 5eInitiativeYaml

A minimal YAML snippet containing monster attributes required by the
Initiative Tracker plugin. Use this in frontmatter.

The source book will be included in the name if it isn't the default monster source ("MM").

### 5eInitiativeYamlNoSource

A minimal YAML snippet containing monster attributes required by the
Initiative Tracker plugin. Use this in frontmatter.

The source book will not be included in the monster name.

### 5eStatblockYaml

Complete monster attributes in the format required by the Fantasy statblock plugin.
Uses double-quoted syntax to deal with a variety of characters occuring in
trait descriptions. Usable in frontmatter or Fantasy Statblock code blocks.

The source book will be included in the name if it isn't the default monster source ("MM").

### 5eStatblockYamlNoSource

Complete monster attributes in the format required by the Fantasy statblock plugin.
Uses double-quoted syntax to deal with a variety of characters occuring in
trait descriptions. Usable in frontmatter or Fantasy Statblock code blocks.

The source book will not be included in the monster name.

### ac

See [AcHp#ac](../AcHp.md#ac)

### acHp

Creature AC and HP as [AcHp](../AcHp.md)

### acText

See [AcHp#acText](../AcHp.md#actext)

### action

Creature actions as a list of [NamedText](../../NamedText.md)

### alignment

Creature alignment

### allTraits

Creature traits as [Traits](Traits.md)

### bonusAction

Creature bonus actions as a list of [NamedText](../../NamedText.md)

### books

List of source books using abbreviated name. Fantasy statblocks uses this list format, as an example.

### conditionImmune

See [ImmuneResist#conditionImmune](../ImmuneResist.md#conditionimmune)

### cr

Challenge rating

### description

Formatted text containing the creature description. Same as `{resource.text}`

### environment

Formatted text describing the creature's environment. Usually a single word.

### fluffImages

List of images as [ImageRef](../../ImageRef.md) (optional)

### fullType

Creature type (lowercase) and subtype if present: `{resource.type} ({resource.subtype})`

### gear

Creature gear as list of item links

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

### hasImages

Return true if any images are present

### hasMoreImages

Return true if more than one image is present

### hasSections

True if the content (text) contains sections

### hitDice

See [AcHp#hitDice](../AcHp.md#hitdice)

### hp

See [AcHp#hp](../AcHp.md#hp)

### hpText

See [AcHp#hpText](../AcHp.md#hptext)

### immune

See [ImmuneResist#immune](../ImmuneResist.md#immune)

### immuneResist

Creature immunities and resistances as [ImmuneResist](../ImmuneResist.md)

### initiative

Initiative bonus as [Initiative](Initiative.md)

### isNpc

True if this is an NPC

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### languages

Comma-separated string of languages the creature understands.

### legendary

Creature legendary traits as a list of [NamedText](../../NamedText.md)

### legendaryGroup

Map of grouped legendary traits (Lair Actions, Regional Effects, etc.).
The key the group name, and the value is a list of [NamedText](../../NamedText.md).

### legendaryGroupLink

Markdown link to legendary group (can be embedded).

### name

Note name

### passive

Passive perception as a numerical value

### pb

Proficiency bonus (modifier)

### rawSpellcasting

Creature spellcasting abilities as a list of [Spellcasting](Spellcasting.md)
attributes

### reaction

Creature reactions as a list of [NamedText](../../NamedText.md)

### reprintOf

List of content superceded by this note (as [Reprinted](../../Reprinted.md))

### resist

See [ImmuneResist#resist](../ImmuneResist.md#resist)

### savesSkills

Creature saving throws and skill modifiers as [SavesAndSkills](SavesAndSkills.md)

### savingThrows

String representation of saving throws.
Equivalent to `{resource.savesSkills.saves}`

### scores

Creature ability scores as [AbilityScores](../AbilityScores/README.md)

### senses

Comma-separated string of creature senses (if present).

### showAllImages

Return embedded wikilinks for all images
If there is more than one, they will be displayed in a gallery.

### showMoreImages

Return embedded wikilinks for all but the first image
If there is more than one, they will be displayed in a gallery.

### showPortraitImage

Return an embedded wikilink to the first image
Will have the "right" anchor tag.

### size

Creature size (capitalized)

### skills

String representation of saving throws.
Equivalent to `{resource.savesSkills.skills}`

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../../SourceAndPage.md)

### sourcesWithFootnote

Get Sources as a footnote.

Calling this method will return an italicised string with the primary source
followed by a footnote listing all other sources. Useful for types
that tend to have many sources.

### speed

Creature speed as a comma-separated list

### spellcasting

Always returns null/empty to suppress previous default behavior that
rendered spellcasting as part of traits.

2024 rules interleave spellcasting with traits, actions, bonus actions, etc.

### subtype

Creature subtype (lowercase)

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### token

Token image as [ImageRef](../../ImageRef.md)

### trait

Creature traits as a list of [NamedText](../../NamedText.md)

### type

Creature type (lowercase)

### vaultPath

Path to this note in the vault

### vulnerable

See [ImmuneResist#vulnerable](../ImmuneResist.md#vulnerable)
