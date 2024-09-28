# ShipCrewCargoPace

5eTools Ship crew, cargo, and pace attributes

This data object provides a default mechanism for creating
a marked up string based on the attributes that are present.

To use it, reference it directly:

```md
{#if resource.shipCrewCargoPace}
{resource.shipCrewCargoPace}
{/if}
```

## Attributes

[acHp](#achp), [cargo](#cargo), [crew](#crew), [crewText](#crewtext), [keelBeam](#keelbeam), [passenger](#passenger), [shipPace](#shippace), [speedPace](#speedpace)


### acHp

Spelljammer or Infernal War Machine HP/AC

### cargo

Cargo capacity (string)

### crew

Crew capacity (number)

### crewText

Additional crew notes

### keelBeam

Spelljammer Keel/Beam

### passenger

Passenger capacity (number)

### shipPace

Ship pace (number, mph)
Ship speed is pace * 10 (*Special Travel Pace*, DMG p242).

### speedPace

Spelljammer speed and pace (preformatted string)
