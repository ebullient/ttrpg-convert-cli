# QuteObject

5eTools object attributes (`object2md.txt`)

Extension of [Tools5eQuteBase](Tools5eQuteBase.md).

## Attributes

[5eInitiativeYaml](#5einitiativeyaml), [5eStatblockYaml](#5estatblockyaml), [ac](#ac), [acText](#actext), [action](#action), [books](#books), [conditionImmune](#conditionimmune), [creatureType](#creaturetype), [fluffImages](#fluffimages), [hasSections](#hassections), [hp](#hp), [hpText](#hptext), [immune](#immune), [isNpc](#isnpc), [labeledSource](#labeledsource), [name](#name), [objectType](#objecttype), [resist](#resist), [scores](#scores), [senses](#senses), [size](#size), [source](#source), [sourceAndPage](#sourceandpage), [speed](#speed), [tags](#tags), [text](#text), [token](#token), [vaultPath](#vaultpath), [vulnerable](#vulnerable)


### 5eInitiativeYaml

A minimal YAML snippet containing object attributes required by the Initiative Tracker plugin. Use this in frontmatter.

### 5eStatblockYaml

Complete object attributes in the format required by the Fantasy statblock plugin. Uses double-quoted syntax to deal with a variety of characters occuring in trait descriptions. Usable in frontmatter or Fantasy Statblock code blocks.

### ac

Armor class (number)

### acText

Additional armor class text: natural armor. May link to related items.

### action

List of object ([actions](../NamedText.md))

### books

List of source books (abbreviated name). Fantasy statblock uses this list.

### conditionImmune

Comma-separated string of creature condition immunities (if present).

### creatureType

Creature type (lowercase); optional

### fluffImages

List of [ImageRef](../ImageRef.md) related to the creature

### hasSections

True if the content (text) contains sections

### hp

Hit points (number); optional

### hpText

Additional hit point text. May link to related items.

### immune

Comma-separated string of creature damage immunities (if present).

### isNpc

True if this is an NPC

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### name

Note name

### objectType

Object type

### resist

Comma-separated string of creature damage resistances (if present).

### scores

Object ability scores ([AbilityScores](AbilityScores.md))

### senses

Comma-separated string of object senses (if present).

### size

Object size (capitalized)

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../SourceAndPage.md)

### speed

Object speed as a comma-separated list

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### token

Token image as [ImageRef](../ImageRef.md)

### vaultPath

Path to this note in the vault

### vulnerable

Comma-separated string of creature damage vulnerabilities (if present).
