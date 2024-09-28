# ImageRef

Create links to referenced images.

The general form of a markdown image link is: `![alt text](vaultPath "title")`.
You can also use anchors to position the image within the page,
which creates links that look like this: `![alt text](vaultPath#anchor "title")`.

## Anchor Tags

Anchor tags are used to position images within a page and are styled with CSS. Examples:

- `center` centers the image and constrains its height.
- `gallery` constrains images within a gallery callout.
- `portrait` floats an image to the right.
- `symbol` floats Deity symbols to the right.
- `token` is a smaller image, also floated to the right. Used in statblocks.

## Attributes

[embeddedLink](#embeddedlink), [shortTitle](#shorttitle), [title](#title), [vaultPath](#vaultpath)


### embeddedLink

Return an embedded markdown link to the image, using an optional
anchor tag to position the image in the page.
For example: `{resource.image.getEmbeddedLink("symbol")}`

If the title is longer than 50 characters:
`![{resource.shortTitle}]({resource.vaultPath}#anchor "{resource.title}")`,

If the title is 50 characters or less:
`![{resource.title}]({resource.vaultPath}#anchor)`,

Links will be generated using "center" as the anchor by default.

### shortTitle

A shortened image title (max 50 characters) for use in markdown links.

### title

Descriptive title (or caption) for the image. This can be long.

### vaultPath

Path of the image in the vault or url for external images.
