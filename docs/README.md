# Documentation

- [Other ways to run the CLI](alternateRun.md)
- [Create a config file](configuration.md)
    - [Configuration Examples][ex-config]
- [Template Reference](templates/README.md)
    - [Default templates][def-templates]
    - [Example templates][ex-templates]
- [Source Map](sourceMap.md)

[README.md]: https://github.com/ebullient/ttrpg-convert-cli
[ex-ad]: ../examples/admonitions
[ex-css]: ../examples/css-snippets
[ex-config]: ../examples/config
[ex-snippets]: ../examples/css-snippets
[ex-templates]: ../examples/templates
[def-templates]: ../src/main/resources/templates

## Recommended Obsidian plugin configuration

### Admonitions

Import one or more admonition json files in [examples/admonitions][ex-ad] to create the custom admonition types used for converted content:

- [admonitions-5e.json](../examples/admonitions/admonitions-5e.json) for 5e tools
- [admonitions-pf2e.json](../examples/admonitions/admonitions-pf2e-v3.json) for pf2e tools
- [other-admonitions.json](../examples/admonitions/other-admonitions.json) if they are interesting

### Force note view mode by front matter

Use this plugin to treat these generated notes as essentially read-only.  

Ensure the plugin has the following options enabled (for the most consistent and least invasive experience): 

- *"Ignore force view when not in front matter"*: the plugin will only change the view mode if `obsidianUIMode` is defined in the front matter.    
- *"Ignore open files"*: the plugin won't try to change the view mode if the file is already open.

## Fantasy Statblocks

Fantasy Statblocks ([git](https://github.com/javalent/fantasy-statblocks)/[obsidian](obsidian://show-plugin?id=obsidian-5e-statblocks)) maintains a bestiary of monsters. It has its own (handlebars-basted) templates for monster statblocks.

The CLI can generate notes that will populate the bestiary, see [5eTools example templates](../examples/templates/tools5e/README.md#5etools-alternate-monster-templates). 

To populate *Fantasy Statblocks* from your notes, use the following settings:

- General Settings:
    - *Optional*: Disable the 5e SRD
- Note Parsing:
    - Enable Parse Frontmatter for Creatures
    - *Optional*: Add bestiary folders to constrain where the plugin looks for monsters

## Initiative Tracker

Initiative Tracker ([git](https://github.com/javalent/initiative-tracker)/[obsidian](obsidian://show-plugin?id=initiative-tracker))
