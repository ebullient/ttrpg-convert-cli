# QuteItem

Pf2eTools Item attributes

Extension of [Pf2eQuteBase](Pf2eQuteBase.md)

## Attributes

[access](#access), [activate](#activate), [aliases](#aliases), [ammunition](#ammunition), [armor](#armor), [category](#category), [contract](#contract), [craftReq](#craftreq), [duration](#duration), [group](#group), [hands](#hands), [hasSections](#hassections), [labeledSource](#labeledsource), [level](#level), [name](#name), [onset](#onset), [price](#price), [shield](#shield), [source](#source), [sourceAndPage](#sourceandpage), [tags](#tags), [text](#text), [traits](#traits), [usage](#usage), [variants](#variants), [vaultPath](#vaultpath), [weapons](#weapons)


### access

Formatted string. Item access attributes

### activate

Item activation attributes as [QuteItemActivate](QuteItem/QuteItemActivate.md)

### aliases

Aliases for this note

### ammunition

Formatted string. Ammunition required

### armor

Item armor attributes as [QuteItemArmorData](QuteItem/QuteItemArmorData.md)

### category

Formatted string. Item category

### contract

Item contract attributes as a list of [NamedText](../NamedText.md)

### craftReq

Formatted string. Crafting requirements

### duration

Formatted string. How long will the item remain active

### group

Formatted string. Item group

### hands

Formatted string. How many hands does this item require to use

### hasSections

True if the content (text) contains sections

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### level

Formatted string. Item power level

### name

Note name

### onset

Formatted string. Onset attributes

### price

Formatted string. Item price (pp, gp, sp, cp)

### shield

Item shield attributes as [QuteItemShieldData](QuteItem/QuteItemShieldData.md)

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../SourceAndPage.md)

### tags

Collected tags for inclusion in frontmatter

### text

Formatted text. For most templates, this is the bulk of the content.

### traits

Collection of traits (decorated links)

### usage

Item use attributes as a list of [NamedText](../NamedText.md)

### variants

Item variants as list of [QuteItemVariant](QuteItem/QuteItemVariant.md)

### vaultPath

Path to this note in the vault

### weapons

Item weapon attributes as list of [QuteItemWeaponData](QuteItem/QuteItemWeaponData.md)
