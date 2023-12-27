# QuteMonster

5eTools creature attributes (`monster2md.txt`)

Extension of [Tools5eQuteBase](../Tools5eQuteBase.md).

## Attributes

[5eInitiativeYaml](#5einitiativeyaml), [5eStatblockYaml](#5estatblockyaml), [ac](#ac), [acHp](#achp), [acText](#actext), [action](#action), [alignment](#alignment), [bonusAction](#bonusaction), [books](#books), [conditionImmune](#conditionimmune), [cr](#cr), [description](#description), [environment](#environment), [fluffImages](#fluffimages), [fullType](#fulltype), [hasSections](#hassections), [hitDice](#hitdice), [hp](#hp), [hpText](#hptext), [immune](#immune), [immuneResist](#immuneresist), [isNpc](#isnpc), [labeledSource](#labeledsource), [languages](#languages), [legendary](#legendary), [legendaryGroup](#legendarygroup), [legendaryGroupLink](#legendarygrouplink), [name](#name), [passive](#passive), [pb](#pb), [reaction](#reaction), [resist](#resist), [savesSkills](#savesskills), [savingThrows](#savingthrows), [scores](#scores), [senses](#senses), [size](#size), [skills](#skills), [source](#source), [sourceAndPage](#sourceandpage), [speed](#speed), [spellcasting](#spellcasting), [subtype](#subtype), [tags](#tags), [text](#text), [token](#token), [trait](#trait), [type](#type), [vaultPath](#vaultpath), [vulnerable](#vulnerable)


### 5eInitiativeYaml

A minimal YAML snippet containing monster attributes required by the Initiative Tracker plugin. Use this in frontmatter.

### 5eStatblockYaml

Complete monster attributes in the format required by the Fantasy statblock plugin. Uses double-quoted syntax to deal with a variety of characters occuring in trait descriptions. Usable in frontmatter or Fantasy Statblock code blocks.

### ac

See [AcHp#ac](../AcHp.md#ac)

### acHp

Creature AC and HP as [AcHp](../AcHp.md)

### acText

See [AcHp#acText](../AcHp.md#acText)

### action

Creature actions as a list of [NamedText](../../NamedText.md)

### alignment

Creature alignment

### bonusAction

Creature bonus actions as a list of [NamedText](../../NamedText.md)

### books

List of source books (abbreviated name). Fantasy statblock uses this list.

### conditionImmune

See [ImmuneResist#conditionImmune](../ImmuneResist.md#conditionImmune)

### cr

Challenge rating

### description

Formatted text containing the creature description. Same as `{resource.text}`

### environment

Formatted text describing the creature's environment. Usually a single word.

### fluffImages

List of [ImageRef](../../ImageRef.md) related to the creature

### fullType

Creature type (lowercase) and subtype if present: `{resource.type} ({resource.subtype})`

### hasSections

True if the content (text) contains sections

### hitDice

See [AcHp#hitDice](../AcHp.md#hitDice)

### hp

See [AcHp#hp](../AcHp.md#hp)

### hpText

See [AcHp#hpText](../AcHp.md#hpText)

### immune

See [ImmuneResist#immune](../ImmuneResist.md#immune)

### immuneResist

Creature immunities and resistances as [ImmuneResist](../ImmuneResist.md)

### isNpc

True if this is an NPC

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### languages

Comma-separated string of languages the creature understands.

### legendary

Creature legendary traits as a list of [NamedText](../../NamedText.md)

### legendaryGroup

Map of grouped legendary traits (Lair Actions, Regional Effects, etc.). The key the group name, and the value is a list of [NamedText](../../NamedText.md).

### legendaryGroupLink

Markdown link to legendary group (can be embedded).

### name

Note name

### passive

Passive perception as a numerical value

### pb

Proficiency bonus (modifier)

### reaction

Creature reactions as a list of [NamedText](../../NamedText.md)

### resist

See [ImmuneResist#resist](../ImmuneResist.md#resist)

### savesSkills

Creature saving throws and skill modifiers as [SavesAndSkills](SavesAndSkills.md)

### savingThrows

String representation of saving throws. Equivalent to `{resource.savesSkills.saves}`

### scores

Creature ability scores as [AbilityScores](../AbilityScores.md)

### senses

Comma-separated string of creature senses (if present).

### size

Creature size (capitalized)

### skills

String representation of saving throws. Equivalent to `{resource.savesSkills.skills}`

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../../SourceAndPage.md)

### speed

Creature speed as a comma-separated list

### spellcasting

Creature abilities as a list of [Spellcasting](Spellcasting.md) attributes

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
