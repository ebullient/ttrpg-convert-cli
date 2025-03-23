# QuteDataDuration

A duration of time. This may be either a [QuteDataTimedDuration](QuteDataTimedDuration/README.md), which represents a period of time longer
than an activity, or a [QuteDataActivity](QuteDataActivity.md). Use [QuteDataDuration#Activity](#activity) to check whether this
duration is an activity.

Using this directly will give the default representation for either object.

## Attributes

[activity](#activity)

### activity

Returns true if this duration is a [QuteDataActivity](QuteDataActivity.md).
