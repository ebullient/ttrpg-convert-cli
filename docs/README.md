# Documentation

- [Other ways to run the CLI](alternateRun.md)
- [Create a config file](configuration.md)
    - [Configuration Examples][ex-config]
- [Template Reference](templates/README.md)
    - [Default templates][def-templates]
    - [Example templates][ex-templates]
- [Source Map](sourceMap.md)

[README.md]: https://github.com/ebullient/ttrpg-convert-cli
[ex-ad]: https://github.com/ebullient/ttrpg-convert-cli/tree/main/examples/admonitions
[ex-css]: https://github.com/ebullient/ttrpg-convert-cli/tree/main/examples/css-snippets
[ex-config]: https://github.com/ebullient/ttrpg-convert-cli/tree/main/examples/config
[ex-snippets]: https://github.com/ebullient/ttrpg-convert-cli/tree/main/examples/css-snippets
[ex-templates]: https://github.com/ebullient/ttrpg-convert-cli/tree/main/examples/templates
[def-templates]: https://github.com/ebullient/ttrpg-convert-cli/tree/main/src/main/resources/templates

## Recommended plugin configuration

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

## Optional CSS Snippets

Within the [examples/css-snippets][ex-css] folder, you will find some CSS snippets that have been created to further customize the look of the generated content. They include:

- Functionality to float token images to side of a statblock.
- Further enhancement of the admonition styles.
- _PF2 Only_: More realistic looking Statblocks
- _PF2 Only_: Link Test to display action icons.
- _PF2 Only_: Light styling of pages as defined through css-classes.
- And much more.

### Statblocks

Compendium (`*-compendium`) snippets include styles for statblocks.

If you aren't using a `*-compendium` snippet, you may want to download either `dnd5e-only-statblocks.css` or `pf2-only-statblocks.css` to style your statblocks.

> ⚠️ Do not use an `*-only-statblock.css` snippet and a `*-compendium.css` snippet together.
