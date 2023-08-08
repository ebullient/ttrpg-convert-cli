# QuteMonster

5eTools creature attributes (`monster2md.txt`)

Extension of [Tools5eQuteBase](Tools5eQuteBase.md).

## Attributes

[5eInitiativeYaml](#5einitiativeyaml), [5eStatblockYaml](#5estatblockyaml), [ac](#ac), [acText](#actext), [action](#action), [alignment](#alignment), [bonusAction](#bonusaction), [books](#books), [conditionImmune](#conditionimmune), [cr](#cr), [description](#description), [environment](#environment), [fluffImages](#fluffimages), [fullType](#fulltype), [hasSections](#hassections), [hitDice](#hitdice), [hp](#hp), [hpText](#hptext), [immune](#immune), [isNpc](#isnpc), [languages](#languages), [legendary](#legendary), [legendaryGroup](#legendarygroup), [name](#name), [passive](#passive), [pb](#pb), [reaction](#reaction), [resist](#resist), [savesSkills](#savesskills), [savingThrows](#savingthrows), [scores](#scores), [senses](#senses), [size](#size), [skills](#skills), [source](#source), [speed](#speed), [spellcasting](#spellcasting), [subtype](#subtype), [tags](#tags), [text](#text), [token](#token), [trait](#trait), [type](#type), [vaultPath](#vaultpath), [vulnerable](#vulnerable)


### 5eInitiativeYaml

A minimal YAML snippet containing monster attributes required by the Initiative Tracker plugin. Use this in frontmatter.

### 5eStatblockYaml

Complete monster attributes in the format required by the Fantasy statblock plugin. Uses double-quoted syntax to deal with a variety of characters occuring in trait descriptions. Usable in frontmatter or Fantasy Statblock code blocks.

### ac

Creature armor class (number)

### acText

Additional armor class text: natural armor. May link to related items.

### action

List of creature ([actions](../NamedText.md))

### alignment

Creature alignment

### bonusAction

List of creature ([bonus actions](../NamedText.md))

### books

List of source books (abbreviated name). Fantasy statblock uses this list.

### conditionImmune

Comma-separated string of creature condition immunities (if present).

### cr

Challenge rating

### description

Formatted text containing the creature description. Same as {resource.text}

### environment

Formatted text describing the creature's environment. Usually a single word.

### fluffImages

List of [ImageRef](../ImageRef.md) related to the creature

### fullType

Creature type (lowercase) and subtype if present: `{resource.type} ({resource.subtype})`

### hasSections

True if the content (text) contains sections

### hitDice

Hit dice formula as formatted string: `7d10 + 14`

### hp

Creature hit points. If using the dice roller plugin is enabled, this will be a dice roll formula.

### hpText

Additional hit point text. In the case of summoned creatures, this will contain notes for how hit points should be calculated relative to the player's modifiers.

### immune

Comma-separated string of creature damage immunities (if present).

### isNpc

True if this is an NPC

### languages

Comma-separated string of languages the creature understands.

### legendary

List of creature ([legendary traits](../NamedText.md))

### legendaryGroup

Map of grouped legendary traits. The key the group name, and the value is the list of associated ([traits](../NamedText.md)). Used for lair actions, as an example.

### name

Note name

### passive

Passive perception as a numerical value

### pb

Proficiency bonus (modifier)

### reaction

List of creature ([reactions](../NamedText.md))

### resist

Comma-separated string of creature damage resistances (if present).

### savesSkills

Creature saving throws and skill modifiers ([SavesAndSkills](SavesAndSkills.md))

### savingThrows

String representation of saving throws. Equivalent to `{resource.savesSkills.saves}`

### scores

Creature ability scores ([AbilityScores](AbilityScores.md))

### senses

Comma-separated string of creature senses (if present).

### size

Creature size (capitalized)

### skills

String representation of saving throws. Equivalent to `{resource.savesSkills.skills}`

### source

Formatted string describing the content's source(es)

### speed

Creature speed as a comma-separated list

### spellcasting

List of creature ([spellcasting abilities](Spellcasting.md))

### subtype

Creature subtype (lowercase)

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### token

Token image as [ImageRef](../ImageRef.md)

### trait

List of creature ([traits](../NamedText.md))

### type

Creature type (lowercase)

### vaultPath

Path to this note in the vault

### vulnerable

Comma-separated string of creature damage vulnerabilities (if present).
