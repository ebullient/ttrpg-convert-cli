# QuteDataNamedBonus

A Pathfinder 2e named bonus, potentially with other conditional bonuses. Example default representation: <blockquote> Stealth +36 (+42 in forests) (ignores tremorsense) </blockquote>

## Attributes

[name](#name), [notes](#notes), [otherBonuses](#otherbonuses), [value](#value)


### name

The name of the skill

### value

The standard bonus associated with this skill

### otherBonuses

Any additional bonuses, as a map of descriptions to bonuses. Iterate over all map entries to display the values, e.g.: `{#each resource.skills.otherBonuses}{it.key}: {it.value}{/each}`

### notes

Any notes associated with this skill bonus
