# QuteDataSkillBonus

A Pathfinder 2e skill and associated bonuses.

Using this directly provides a default representation, e.g. `Stealth +36 (+42 in forests) (some other note)`

## Attributes

[name](#name), [notes](#notes), [otherBonuses](#otherbonuses), [value](#value)


### name

The name of the skill

### value

The standard bonus associated with this skill

### otherBonuses

Any additional bonuses, as a map of descriptions to bonuses. Iterate over all map entries to display the values: `{#each resource.skills.otherBonuses}{it.key}: {it.value}{/each}`

### notes

Any notes associated with this skill bonus
