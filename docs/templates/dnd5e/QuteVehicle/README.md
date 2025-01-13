# QuteVehicle

5eTools vehicle attributes (`vehicle2md.txt`)

Several different types of vehicle use this template, including:
Ship, spelljammer, infernal war manchie, objects and creatures.
They can have very different properties. Treat most as optional.

Extension of [Tools5eQuteBase](../Tools5eQuteBase.md).

## Attributes

[action](#action), [fluffImages](#fluffimages), [hasImages](#hasimages), [hasMoreImages](#hasmoreimages), [hasSections](#hassections), [immuneResist](#immuneresist), [isCreature](#iscreature), [isObject](#isobject), [isShip](#isship), [isSpelljammer](#isspelljammer), [isWarMachine](#iswarmachine), [labeledSource](#labeledsource), [name](#name), [reprintOf](#reprintof), [scores](#scores), [shipCrewCargoPace](#shipcrewcargopace), [shipSections](#shipsections), [showAllImages](#showallimages), [showMoreImages](#showmoreimages), [showPortraitImage](#showportraitimage), [sizeDimension](#sizedimension), [source](#source), [sourceAndPage](#sourceandpage), [tags](#tags), [terrain](#terrain), [text](#text), [token](#token), [vaultPath](#vaultpath), [vehicleType](#vehicletype)


### action

List of vehicle actions as a collection of [NamedText](../../NamedText.md)

### fluffImages

List of images as [ImageRef](../../ImageRef.md) (optional)

### hasImages

Return true if any images are present

### hasMoreImages

Return true if more than one image is present

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

### reprintOf

List of content superceded by this note (as [Reprinted](../../Reprinted.md))

### scores

Object ability scores as [AbilityScores](../AbilityScores.md)
Used by Ship, Infernal War Machine, Creature, Object

### shipCrewCargoPace

Ship capacity and pace attributes as [ShipCrewCargoPace](ShipCrewCargoPace.md).

### shipSections

Ship sections and traits as [ShipAcHp](ShipAcHp.md) (hull, sails,
oars, .. )

### showAllImages

Return embedded wikilinks for all images
If there is more than one, they will be displayed in a gallery.

### showMoreImages

Return embedded wikilinks for all but the first image
If there is more than one, they will be displayed in a gallery.

### showPortraitImage

Return an embedded wikilink to the first image
Will have the "right" anchor tag.

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
