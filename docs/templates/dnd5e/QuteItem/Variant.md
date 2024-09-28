# Variant


## Attributes

[age](#age), [ammo](#ammo), [armorClass](#armorclass), [attunement](#attunement), [baseItem](#baseitem), [cost](#cost), [costCp](#costcp), [cursed](#cursed), [damage](#damage), [damage2h](#damage2h), [detail](#detail), [firearm](#firearm), [focus](#focus), [focusType](#focustype), [mastery](#mastery), [masteryList](#masterylist), [name](#name), [poison](#poison), [poisonTypes](#poisontypes), [prerequisite](#prerequisite), [properties](#properties), [propertiesList](#propertieslist), [range](#range), [rarity](#rarity), [staff](#staff), [stealthPenalty](#stealthpenalty), [strengthRequirement](#strengthrequirement), [subtypeString](#subtypestring), [tattoo](#tattoo), [tier](#tier), [type](#type), [typeAlt](#typealt), [weaponCategory](#weaponcategory), [weight](#weight), [wondrous](#wondrous)


### age

Age/Era of item. Optional. Known values: futuristic, industrial, modern, renaissance, victorian.

### ammo

True if this is ammunition

### armorClass

Changes to armor class provided by the item. Optional.

### attunement

Attunement requirements. Optional. One of: required, optional, prerequisites/conditions (implies
required).

### baseItem

Markdown link to base item. Optional.

### cost

Cost of the item (gp, sp, cp). Usually missing for magic items.

### costCp

Cost of the item (cp) as number. Usually missing for magic items.

### cursed

True if this is a cursed item

### damage

One-handed Damage string. Contains dice formula and damage type. Optional.

### damage2h

Two-handed Damage string. Contains dice formula and damage type. Optional.

### detail

Formatted string of item details. Will include some combination of tier, rarity, category, and attunement

### firearm

True if this is a firearm

### focus

True if this is a spellcasting focus.

### focusType

Spellcasting focus type. Optional. One of: "arcane", "druid", "holy", and/or a list of required classes.

### mastery

Formatted string listing applicable item mastery (with links to rules if the source is present)

### masteryList

List of item mastery that apply to this item.

### name

Name of the variant.

### poison

True if this is a poison.

### poisonTypes

Poison type(s). Optional.

### prerequisite

Formatted text listing other prerequisite conditions. Optional.

### properties

Formatted string listing item's properties (with links to rules if the source is present)

### propertiesList

List of item's properties (with links to rules if the source is present).

### range

Item's range. Optional.

### rarity

Item rarity. Optional. One of: "none": mundane items; "unknown (magic)": miscellaneous magical items;
"unknown": miscellaneous mundane items; "varies": item groups or magic variants.

### staff

True if this is a staff

### stealthPenalty

True if the item imposes a stealth penalty. Optional.

### strengthRequirement

Strength requirement as a numerical value. Optional.

### subtypeString

Item subtype string. Optional.

### tattoo

True if this is a tattoo

### tier

Item tier. Optional. One of: "minor", "major".

### type

Item type

### typeAlt

Alternate item type. Optional.

### weaponCategory

Weapon category. Optional. One of: "simple", "martial".

### weight

Weight of the item (pounds) as a decimal value.

### wondrous

True if this is a wondrous item
