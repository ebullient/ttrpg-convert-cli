# 5eTools Templates for Obsidian

This guide explains how to use 5eTools data with Obsidian plugins like Fantasy Statblocks and Initiative Tracker.

- [References](#references)
- [Fantasy Statblocks Integration](#fantasy-statblocks-integration)
    - [Minimal YAML for Bestiary and Initive Tracker](#minimal-yaml-for-bestiary-and-initive-tracker)
    - [Full statblock rendering](#full-statblock-rendering)
- [Template Types](#template-types)
    - [Default Templates](#default-templates)
    - [5eTools templates with images](#5etools-templates-with-images)
- [Display Variations](#display-variations)
    - [Alternate Ability Score Display](#alternate-ability-score-display)
    - [2024 Score Display (Table Format)](#2024-score-display-table-format)
- [Advanced Techniques](#advanced-techniques)
    - [Splitting Strings in Frontmatter](#splitting-strings-in-frontmatter)

## References

- [5eTools template reference](../../../docs/templates/dnd5e/README.md)
- [Monster template reference](../../../docs/templates/dnd5e/QuteMonster/README.md)

## Fantasy Statblocks Integration

Thethe *Fantasy Statblocks* plugin provides a bestiary that can be used with *Initiative Tracker* to create encounters.

**Monsters**, **Objects**, and **Vehicles** provide attributes that can be used with the *Fantasy Statblocks* bestiary.

- [Minimal YAML for Bestiary and Initive Tracker](#minimal-yaml-for-bestiary-and-initive-tracker)
- [Full statblock rendering](#full-statblock-rendering)

### Minimal YAML for Bestiary and Initive Tracker

Use this approach to make creatures available to the Initiative Tracker for encounters.

1. Add to your note's frontmatter:

    ```yaml
    statblock: true
    statblock-link: "#^statblock"
    {resource.5eInitiativeYaml}
    ```

    - `statblock: inline` tells *Fantasy Statblocks* that the note defines a creature.
    - `statblock-link` specifies content that should be linked to / embeded in the *Initiative Tracker* and *Combatant/Creature* views
    - `{resource.5eInitiativeYaml}` or `{resource.5eInitiativeYamlNoSource}` will add only the attributes *Initiative Tracker* needs

2. In your note body, create a block reference (`^statblock) after the content you want displayed in the creature view:

    ````md
    ```ad-statblock

    ...statblock content...

    ```
    ^statblock
    ````

Examples:

- [monster2md-yamlStatblock-header.txt](monster2md-yamlStatblock-header.txt)
- [object2md-yamlStatblock-header.txt](object2md-yamlStatblock-header.txt)

### Full statblock rendering

This approach will use *Fantasy Statblocks* rendering.

1. Add `statblock: inline` to the note's frontmatter to tell *Fantasy Statblocks* that the note defines a creature.

    ```yaml
    statblock: true
    ```

2. In your note body, use:

    ````markdown
    ```statblock
    {resource.5eStatblockYaml}
    ```

    Or, to hide the source suffix:

    ````markdown
    ```statblock
    {resource.5eStatblockYamlNoSource}
    ```
    ````

Examples:

- [monster2md-yamlStatblock-body.txt](monster2md-yamlStatblock-body.txt)
- [object2md-yamlStatblock-body.txt](object2md-yamlStatblock-body.txt)

> [!TIP]
> If you're using the *Fantasy Statblocks* plugin to render statblocks
> and you use the Dice Roller plugin, you'll want to set the following
> CLI config values:
>
> ```json
> "useDiceRoller" : true,
> "yamlStatblocks" : true,
> ```

## Template Types

### Default Templates

Available template types include:

- monster
- spell
- item
- background
- class
- object
- vehicle
- race
- and more

Full list: [5eTools default templates](../../../src/main/resources/templates/tools5e/)

### 5eTools templates with images

Some 5eTools data types have fluff images.  These templates include those images in the markdown.

- [images-background2md.txt](images-background2md.txt)
- [images-item2md.txt](images-item2md.txt)
- [images-monster2md.txt](images-monster2md.txt)
- [images-object2md.txt](images-object2md.txt)
- [images-race2md.txt](images-race2md.txt)
- [images-spell2md.txt](images-spell2md.txt)
- [images-vehicle2md.txt](images-vehicle2md.txt)

Full list: any template beginning with "images" [5eTools example templates](./)

## Display Variations

### Alternate Ability Score Display

````markdown
```ad-statblock
...
- STR: {resource.scores.str} `dice: 1d20 {resource.scores.strMod}`
- DEX: {resource.scores.dex} `dice: 1d20 {resource.scores.dexMod}`
- CON: {resource.scores.con} `dice: 1d20 {resource.scores.conMod}`
- INT: {resource.scores.int} `dice: 1d20 {resource.scores.intMod}`
- WIS: {resource.scores.wis} `dice: 1d20 {resource.scores.wisMod}`
- CHA: {resource.scores.cha} `dice: 1d20 {resource.scores.chaMod}`
...
```
^statblock
````

Example:

- [monster2md-scores.txt](monster2md-scores.txt) (similar will work for objects)

### 2024 Score Display (Table Format)

This pulls things apart a little differently.

- `resource.scores.*Stat` will show the raw score
- `resource.scores.*Mod` will show the modifier
- `resource.savesSkills.saveOrDefault.*` will render the saving throw (in bold) or the default modifier

```md
|   | STAT  |  MOD | SAVE |
|:--|:-:|:----:|:----:|
|Str| {resource.scores.strStat} | {resource.scores.strMod} | {resource.savesSkills.saveOrDefault.strength} |
|Dex| {resource.scores.dexStat} | {resource.scores.dexMod} | {resource.savesSkills.saveOrDefault.dexterity} |
|Con| {resource.scores.conStat} | {resource.scores.conMod} | {resource.savesSkills.saveOrDefault.constitution} |
|Int| {resource.scores.intStat} | {resource.scores.intMod} | {resource.savesSkills.saveOrDefault.intelligence} |
|Wis| {resource.scores.wisStat} | {resource.scores.wisMod} | {resource.savesSkills.saveOrDefault.wisdom} |
|Cha| {resource.scores.chaStat} | {resource.scores.chaMod} | {resource.savesSkills.saveOrDefault.charisma} |
```

Raw types are [here](../../../docs/templates/dnd5e/QuteMonster/SavesAndSkills.md)

## Advanced Techniques

### Splitting Strings in Frontmatter

```md
{#if resource.conditionImmune}
conditionImmunities:
{#for condition in resource.conditionImmune.split(", ?")}
- "{condition}"
{/for}
{/if}
```

### Display as JSON

To help debug what attributes are available, you can have it rendered as a JSON string.

Two examples:

```md
{resource.conditionImmune.jsonString}

{resource.savesSkills.jsonString}
```
