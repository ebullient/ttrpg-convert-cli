# QuteRitualCasting

Pf2eTools ritual casting attributes

This data object provides a default mechanism for creating
a marked up string based on the attributes that are present.
To use it, reference it directly: `{resource.casting}`.

## Attributes

[cost](#cost), [duration](#duration), [secondaryCasters](#secondarycasters)


### cost

Formatted string. Material cost of the spell

### duration

Duration to cast, as a [QuteDataDuration](../QuteDataDuration.md) which is either a [QuteDataActivity](../QuteDataActivity.md), or a
[QuteDataTimedDuration](../QuteDataTimedDuration/README.md).

### secondaryCasters

Minumum number of secondary casters required
