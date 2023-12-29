# Documentation

- [Other ways to run the CLI](alternateRun.md)
- [Create a config file](configuration.md)
    - [Configuration Examples][ex-config]
- [Template Reference](templates/README.md)
    - [Default templates][def-templates]
    - [Example templates][ex-templates]
- [Source Map](sourceMap.md)

[ex-ad]: ../examples/admonitions
[ex-config]: ../examples/config
[ex-templates]: ../examples/templates
[def-templates]: ../src/main/resources/templates

## Recommended Obsidian plugin configuration

### Admonitions

The Admonition plugin ([git](https://github.com/javalent/admonitions)/[obsidian](obsidian://show-plugin?id=obsidian-admonition)) provides additional support for codeblock-style admonitions in addition to callouts.

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

Fantasy Statblocks ([git](https://github.com/javalent/fantasy-statblocks)/[obsidian](obsidian://show-plugin?id=obsidian-5e-statblocks)) maintains a bestiary of monsters. It has its own (handlebars-based) templates for monster statblocks.

To populate *Fantasy Statblocks* from your notes, use the following settings:

- General Settings:
    - *Optional*: Disable the 5e SRD
- Note Parsing:
    - Enable Parse Frontmatter for Creatures
    - *Optional*: Add bestiary folders to constrain where the plugin looks for monsters

You also need to generate content using [templates][5eTools templates] that will populate the bestiary.

## Initiative Tracker

The Initiative Tracker ([git](https://github.com/javalent/initiative-tracker)/[obsidian](obsidian://show-plugin?id=initiative-tracker)) plugin for Obsidian allows you to keep track of initiative and turn order during combat encounters in tabletop role-playing games.

- Basic Settings
    - *Optional*: Embed statblock-link content in the creature view.  
        Enable this if you use markdown statblocks and want to see the statblock content in the creature view.
        `statblock-link` must be set in the frontmatter.
- Plugin Integrations
    - *Optional*: Sync monsters from TTRPG Statblocks  
        Enable this if you use the Fantasy Statblocks plugin and want to sync monsters from your notes to the Initiative Tracker.

        - See [Fantasy Statblocks](#fantasy-statblocks) section for recommended settings.
        - Make sure you're using compatible [templates][5eTools templates] for your monsters.

[5eTools templates]: ../examples/templates/tools5e/README.md#5etools-alternate-monster-templates
