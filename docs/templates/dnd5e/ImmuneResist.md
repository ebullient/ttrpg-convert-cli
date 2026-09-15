# ImmuneResist

5eTools vulnerabilities, resistances, immunities, and condition immunities

This data object provides a default mechanism for creating
a marked up string based on the attributes that are present.
The `conditionImmune` property contains the linkified string; use the
condition-immunity list accessor when plain, ordered values are needed.

## Attributes

[conditionImmune](#conditionimmune), [conditionImmuneList](#conditionimmunelist), [immune](#immune), [present](#present), [resist](#resist), [vulnerable](#vulnerable)

### conditionImmune

Comma-separated string of creature condition immunities (if present).

### conditionImmuneList

Ordered plain condition-immunity values, including non-standard values such as `special`.

### immune

Comma-separated string of creature damage immunities (if present).

### present

True if immunities or resistances are present (otherwise false)

### resist

Comma-separated string of creature damage resistances (if present).

### vulnerable

Comma-separated string of creature damage vulnerabilities (if present).
