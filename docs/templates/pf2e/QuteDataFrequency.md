# QuteDataFrequency

A description of a frequency e.g. "once", which may include an interval that this is repeated for.

Examples:

- once per day
- once per hour
- 3 times per day
- `recurs=true`: once every day
- `overcharge=true`: once per day, plus overcharge
- `interval=2`: once per 2 days

## Attributes

[interval](#interval), [notes](#notes), [overcharge](#overcharge), [recurs](#recurs), [unit](#unit), [value](#value)

### interval

The interval that the frequency is repeated for

### notes

Any notes associated with the frequency. May include a custom string, for frequencies which cannot be
represented using the normal parts. If this is present, then the other parameters will be null.

### overcharge

Whether there's an overcharge involved. Used for wands mostly. In the default representation, this
adds ", plus overcharge".

### recurs

Whether the unit recurs. In the default representation, this makes it render "every" instead of "per"

### unit

The unit the frequency is in, string. Required.

### value

The number represented by the frequency, integer
