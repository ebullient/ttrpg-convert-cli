# Templates

This application uses the [Qute Templating Engine](https://quarkus.io/guides/qute). You can make simple customizations to markdown output by copying a template from `src/main/resources/templates`, making the desired modifications, and specifying that template in your configuration file under the `template` key:

``` json
{
  "from": [
    "DMG",
    "PHB",
    "MM"
  ],
  "paths": {
    "compendium": "z_compendium/",
    "rules": "z_compendium/rules"
  },
  "template": {
    "background": "examples/templates/tools5e/images-background2md.txt",
    "monster": "examples/templates/tools5e/monster2md-scores.txt"
  }
}
```

The flag used to specify a template corresponds to the type of template being used. You can find the list of valid template keys in the [source code](../../src/main/resources/convertData.json) (look for `templateKeys`).

- Valid template keys for 5etools: `background`, `class`, `deck`, `deity`, `feat`, `hazard`, `index.txt`, `item`, `monster`, `note`, `object`, `psionic`, `race`, `reward`, `spell`, `subclass`, `vehicle`.
- Valid template keys for Pf2eTools: `ability`, `action`, `affliction`, `archetype`, `background`, `book`, `deity`, `feat`, `hazard`, `inline-ability`, `inline-affliction`, `inline-attack`, `item`, `note`, `ritual`, `spell`, `trait`.

## Customizing templates

Not everything is customizable. In some cases, indenting, organizing, formatting, and linking text accurately is easier to do inline as a big blob.

[Documentation](../../docs/templates/) is generated for template-accessible attributes.

- [**5eTools Example Templates**](tools5e/).
