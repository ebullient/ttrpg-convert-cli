# QuteObject

5eTools object attributes (`object2md.txt`)

Extension of [Tools5eQuteBase](Tools5eQuteBase.md).

## Attributes

[5eInitiativeYaml](#5einitiativeyaml), [5eStatblockYaml](#5estatblockyaml), [ac](#ac), [acHp](#achp), [acText](#actext), [action](#action), [books](#books), [conditionImmune](#conditionimmune), [creatureType](#creaturetype), [fluffImages](#fluffimages), [getAliases](#getaliases), [hasImages](#hasimages), [hasMoreImages](#hasmoreimages), [hasSections](#hassections), [hitDice](#hitdice), [hp](#hp), [hpText](#hptext), [immune](#immune), [immuneResist](#immuneresist), [isNpc](#isnpc), [labeledSource](#labeledsource), [name](#name), [objectType](#objecttype), [reprintOf](#reprintof), [resist](#resist), [scores](#scores), [senses](#senses), [showAllImages](#showallimages), [showMoreImages](#showmoreimages), [showPortraitImage](#showportraitimage), [size](#size), [source](#source), [sourceAndPage](#sourceandpage), [sourcesWithFootnote](#sourceswithfootnote), [speed](#speed), [tags](#tags), [text](#text), [token](#token), [vaultPath](#vaultpath), [vulnerable](#vulnerable)

### 5eInitiativeYaml

A minimal YAML snippet containing object attributes required by the
Initiative Tracker plugin. Use this in frontmatter.

### 5eStatblockYaml

Complete object attributes in the format required by the Fantasy statblock plugin.
Uses double-quoted syntax to deal with a variety of characters occuring in
trait descriptions. Usable in frontmatter or Fantasy Statblock code blocks.

### ac

See [AcHp#ac](AcHp.md#ac)

### acHp

Object AC and HP as [AcHp](AcHp.md)

### acText

See [AcHp#acText](AcHp.md#actext)

### action

Object actions as a list of [NamedText](../NamedText.md)

### books

List of source books using abbreviated name. Fantasy statblocks uses this list format, as an example.

### conditionImmune

See [ImmuneResist#conditionImmune](ImmuneResist.md#conditionimmune)

### creatureType

Creature type (lowercase); optional

### fluffImages

List of images as [ImageRef](../ImageRef.md) (optional)

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

See [AcHp#hitDice](AcHp.md#hitdice)

### hp

See [AcHp#hp](AcHp.md#hp)

### hpText

See [AcHp#hpText](AcHp.md#hptext)

### immune

See [ImmuneResist#immune](ImmuneResist.md#immune)

### immuneResist

Object immunities and resistances as [ImmuneResist](ImmuneResist.md)

### isNpc

True if this is an NPC

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### name

Note name

### objectType

Object type

### reprintOf

List of content superceded by this note (as [Reprinted](../Reprinted.md))

### resist

See [ImmuneResist#resist](ImmuneResist.md#resist)

### scores

Object ability scores as [AbilityScores](AbilityScores/README.md))

### senses

Comma-separated string of object senses (if present).

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

Object size (capitalized)

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../SourceAndPage.md)

### sourcesWithFootnote

Get Sources as a footnote.

Calling this method will return an italicised string with the primary source
followed by a footnote listing all other sources. Useful for types
that tend to have many sources.

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

See [ImmuneResist#vulnerable](ImmuneResist.md#vulnerable)
