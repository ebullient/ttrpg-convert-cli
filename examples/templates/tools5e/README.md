# Templates for 5eTools

## 5eTools default templates

[5eTools default templates](https://github.com/ebullient/ttrpg-convert-cli/blob/main/src/main/resources/templates/tools5e/)

Valid template keys for 5etools:
[`background`](https://github.com/ebullient/ttrpg-convert-cli/blob/main/src/main/resources/templates/tools5e/background2md.txt),
[`class`](https://github.com/ebullient/ttrpg-convert-cli/blob/main/src/main/resources/templates/tools5e/class2md.txt),
[`deck`](https://github.com/ebullient/ttrpg-convert-cli/blob/main/src/main/resources/templates/tools5e/deck2md.txt),
[`deity`](https://github.com/ebullient/ttrpg-convert-cli/blob/main/src/main/resources/templates/tools5e/deity2md.txt),
[`feat`](https://github.com/ebullient/ttrpg-convert-cli/blob/main/src/main/resources/templates/tools5e/feat2md.txt),
[`hazard`](https://github.com/ebullient/ttrpg-convert-cli/blob/main/src/main/resources/templates/tools5e/hazard2md.txt),
[`index.txt`](https://github.com/ebullient/ttrpg-convert-cli/blob/main/src/main/resources/templates/tools5e/index.txt),
[`item`](https://github.com/ebullient/ttrpg-convert-cli/blob/main/src/main/resources/templates/tools5e/item2md.txt),
[`monster`](https://github.com/ebullient/ttrpg-convert-cli/blob/main/src/main/resources/templates/tools5e/monster2md.txt),
[`note`](https://github.com/ebullient/ttrpg-convert-cli/blob/main/src/main/resources/templates/tools5e/note2md.txt),
[`object`](https://github.com/ebullient/ttrpg-convert-cli/blob/main/src/main/resources/templates/tools5e/object2md.txt),
[`psionic`](https://github.com/ebullient/ttrpg-convert-cli/blob/main/src/main/resources/templates/tools5e/psionic2md.txt),
[`race`](https://github.com/ebullient/ttrpg-convert-cli/blob/main/src/main/resources/templates/tools5e/race2md.txt),
[`reward`](https://github.com/ebullient/ttrpg-convert-cli/blob/main/src/main/resources/templates/tools5e/reward2md.txt),
[`spell`](https://github.com/ebullient/ttrpg-convert-cli/blob/main/src/main/resources/templates/tools5e/spell2md.txt),
[`subclass`](https://github.com/ebullient/ttrpg-convert-cli/blob/main/src/main/resources/templates/tools5e/subclass2md.txt),
[`vehicle`](https://github.com/ebullient/ttrpg-convert-cli/blob/main/src/main/resources/templates/tools5e/vehicle2md.txt)

## 5eTools templates with images

Some 5eTools data types have fluff images.  These templates include those images in the markdown.

- [images-background2md.txt](images-background2md.txt)
- [images-item2md.txt](images-item2md.txt)
- [images-monster2md.txt](images-monster2md.txt)
- [images-object2md.txt](images-object2md.txt)
- [images-race2md.txt](images-race2md.txt)
- [images-spell2md.txt](images-spell2md.txt)
- [images-vehicle2md.txt](images-vehicle2md.txt)

## 5eTools alternate monster templates

- **Markdown statblock with minimal YAML frontmatter** for use with *Fantasy Statblock* and *Initiative Tracker* plugins
    - Monsters, objects, and vehicles that have `statblock: true` will populate the *Fantasy Statblock* bestiary.
    - *Initiative Tracker* only needs a few elements from the statblock for encounter building. `{resource.5eInitiativeYaml}` will emit only those elements.
    - The `statblock-link` is used by *Initiative Tracker* to embed the markdown statblock in the creature view.
    - [monster2md-yamlStatblock-header.txt](monster2md-yamlStatblock-header.txt)
    - [object2md-yamlStatblock-header.txt](object2md-yamlStatblock-header.txt)

    The important frontmatter elements of this example:

    ```yaml
    statblock: true
    statblock-link: "#^statblock"
    {resource.5eInitiativeYaml}
    ```

    In the body of the note:

    ````markdown
    ```ad-statblock
    ...statblock content...
    ```
    ^statblock
    ````

- ***Fantasy Statblock* `statblock` in the body**
    - Monsters, objects, and vehicles that have `statblock: inline` will populate the *Fantasy Statblock* bestiary.
    - [monster2md-yamlStatblock-body.txt](monster2md-yamlStatblock-body.txt)
    - [object2md-yamlStatblock-body.txt](object2md-yamlStatblock-body.txt)

    The important frontmatter elements of this example:

    ```yaml
    statblock: inline
    ```

    In the body of the note:

    ````markdown
    ```statblock
    {resource.5eStatblockYaml}
    ```
    ````

- **Markdown statblock, alternate score display** - [monster2md-scores.txt](monster2md-scores.txt) (similar will work for objects)

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
