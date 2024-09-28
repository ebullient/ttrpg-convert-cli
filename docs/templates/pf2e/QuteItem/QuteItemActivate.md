# QuteItemActivate

Pf2eTools item activation attributes.

This data object provides a default mechanism for creating
a marked up string based on the attributes that are present.
To use it, reference it directly: `{resource.activate}`.

## Attributes

[activity](#activity), [components](#components), [frequency](#frequency), [requirements](#requirements), [trigger](#trigger)


### activity

Item [activity/activation details](../QuteDataActivity.md)

### components

Formatted string. Components required to activate this item

### frequency

[QuteDataFrequency](../QuteDataFrequency.md).
How often this item can be used/activated. Use directly to get a formatted string.

### requirements

Formatted string. Requirements for activating this item

### trigger

Formatted string. Trigger to activate this item
