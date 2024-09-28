# QuteDataTimedDuration

A duration of time, represented by a numerical value and a unit. Sometimes this includes a custom display string,
for durations which cannot be represented using the normal structure.

Examples:

- A duration of 3 minutes: `3 minutes`
- A duration of 1 turn: `until the end of your next turn`
- An unlimited duration: `unlimited`

## Attributes

[customDisplay](#customdisplay), [formattedNotes](#formattednotes), [notes](#notes), [unit](#unit), [value](#value)


### customDisplay

The custom display used for this duration.

### formattedNotes

Returns a comma delimited string containing all notes.

### notes


### unit

The unit that the quantity is measured in, as a [DurationUnit](DurationUnit.md)

### value

The quantity of time
