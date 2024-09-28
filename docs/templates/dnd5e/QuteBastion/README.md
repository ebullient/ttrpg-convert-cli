# QuteBastion

5eTools background attributes (`bastion2md.txt`).

Extension of [Tools5eQuteBase](../Tools5eQuteBase.md).

## Attributes

[fluffImages](#fluffimages), [hasSections](#hassections), [hirelingDescription](#hirelingdescription), [hirelings](#hirelings), [labeledSource](#labeledsource), [level](#level), [name](#name), [orders](#orders), [prerequisite](#prerequisite), [reprintOf](#reprintof), [source](#source), [sourceAndPage](#sourceandpage), [space](#space), [spaceDescription](#spacedescription), [tags](#tags), [text](#text), [type](#type), [vaultPath](#vaultpath)


### fluffImages

List of images for this bastion (as [ImageRef](../../ImageRef.md), optional)

### hasSections

True if the content (text) contains sections

### hirelingDescription

Hirelings as a descriptive string (if hirelings is present)

### hirelings

List of possible hirelings this bastion can have (as [Hireling](Hireling.md),
optional)

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### level

Bastion level (optional)

### name

Note name

### orders

Bastion orders (optional)

### prerequisite

Formatted text listing other prerequisite conditions (optional)

### reprintOf

List of content superceded by this note (as [Reprinted](../../Reprinted.md))

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../../SourceAndPage.md)

### space

List of possible spaces this bastion can occupy (as [Space](Space.md),
optional)

### spaceDescription

Space as a descriptive string (if space is present)

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### type

Type

### vaultPath

Path to this note in the vault
