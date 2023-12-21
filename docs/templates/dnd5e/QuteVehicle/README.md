# QuteVehicle

5eTools vehicle attributes (`vehicle2md.txt`)

Several different types of vehicle use this template, including: Ship, spelljammer, infernal war manchie, objects and creatures. They can have very different properties. Treat most as optional.

Extension of [Tools5eQuteBase](../Tools5eQuteBase.md).

## Attributes

[action](#action), [fluffImages](#fluffimages), [hasSections](#hassections), [immuneResist](#immuneresist), [isCreature](#iscreature), [isObject](#isobject), [isShip](#isship), [isSpelljammer](#isspelljammer), [isWarMachine](#iswarmachine), [labeledSource](#labeledsource), [name](#name), [scores](#scores), [shipCrewCargoPace](#shipcrewcargopace), [shipSections](#shipsections), [sizeDimension](#sizedimension), [source](#source), [sourceAndPage](#sourceandpage), [tags](#tags), [terrain](#terrain), [text](#text), [token](#token), [vaultPath](#vaultpath), [vehicleType](#vehicletype)


### action

List of vehicle actions as a collection of [NamedText](../../NamedText.md)

### fluffImages

List of [ImageRef](../../ImageRef.md) related to the creature

### hasSections

True if the content (text) contains sections

### immuneResist

Vehicle immunities and resistances as [ImmuneResist](../ImmuneResist.md)

### isCreature

True if this vehicle is a Creature

### isObject

True if this vehicle is an Object

### isShip

True if this vehicle is a Ship

### isSpelljammer

True if this vehicle is a Spelljammer

### isWarMachine

True if this vehicle is an Infernal War Machine

### labeledSource

Formatted string describing the content's source(s): `_Source: <sources>_`

### name

Note name

### scores

Object ability scores as [AbilityScores](../AbilityScores.md) Used by Ship, Infernal War Machine, Creature, Object

### shipCrewCargoPace

Ship capacity and pace attributes as [ShipCrewCargoPace](ShipCrewCargoPace.md).

### shipSections

Ship sections and traits as [ShipAcHp](ShipAcHp.md) (hull, sails, oars, .. )

### sizeDimension

Ship size and dimensions. Used by Ship, Infernal War Machine

### source

String describing the content's source(s)

### sourceAndPage

Book sources as list of [SourceAndPage](../../SourceAndPage.md)

### tags

Collected tags for inclusion in frontmatter

### terrain

Vehicle terrain as a comma-separated list (all)

### text

Formatted text. For most templates, this is the bulk of the content.

### token

Token image as [ImageRef](../../ImageRef.md)

### vaultPath

Path to this note in the vault

### vehicleType

Vehicle type: Ship, Spelljammer, Infernal War Machine, Creature, Object
