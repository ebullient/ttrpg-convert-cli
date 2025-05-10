# TtrpgTemplateExtension

Qute template extensions for TTRPG data.

Use these functions to help render TTRPG data in Qute templates.

## Attributes

[asBonus](#asbonus), [capitalized](#capitalized), [join](#join), [joinConjunct](#joinconjunct), [jsonString](#jsonstring), [pluralizeLabel](#pluralizelabel), [prefixSpace](#prefixspace)

### asBonus

Return the value formatted with a bonus with a +/- prefix. Example: `{perception.asBonus}`

### capitalized

Return the string capitalized. Example: `{resource.name.capitalized}`

### join

Return the given collection converted into a string and joined using the specified joiner.

Example: `{resource.components.join(", ")}`

### joinConjunct

Return the given list joined into a single string, using a different delimiter for the last element.

Example: `{resource.components.joinConjunct(", ", " or ")}`

### jsonString

Return the object as a JSON string

Example: `{resource.components.getJsonString(resource)}`

### pluralizeLabel

Return the string pluralized based on the size of the collection.

Example: `{resource.name.pluralized(resource.components)}`

### prefixSpace

Return the given object as a string, with a space prepended if it's non-empty and non-null.

Example: `{resource.name.prefixSpace}`
