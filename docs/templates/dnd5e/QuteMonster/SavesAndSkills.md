# SavesAndSkills

5eTools creature saving throws and skill attributes.

## Attributes

[saveMap](#savemap), [saves](#saves), [skillMap](#skillmap), [skills](#skills)

### saveMap

Creature saving throws as a map of key-value pairs.
Iterate over all map entries to display the values:  

`{#each resource.savesSkills.saveMap}**{it.key}** {it.value}{/each}`

### saves

Creature saving throws as a list: Constitution +6, Intelligence +8

### skillMap

Creature skills as a map of key-value pairs.
Iterate over all map entries to display the values:  

`{#each resource.savesSkills.skillMap}**{it.key}** {it.value}{/each}`

### skills

Creature skills as a list: History +12, Perception +12
