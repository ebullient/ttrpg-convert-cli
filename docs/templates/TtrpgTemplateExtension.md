# TtrpgTemplateExtension

Qute template extensions for TTRPG data.

Use these functions to help render TTRPG data in Qute templates.

## Attributes

[asBonus](#asbonus), [capitalized](#capitalized), [capitalizedList](#capitalizedlist), [first](#first), [join](#join), [joinConjunct](#joinconjunct), [jsonString](#jsonstring), [lowercase](#lowercase), [pluralizeLabel](#pluralizelabel), [prefixSpace](#prefixspace), [quotedEscaped](#quotedescaped), [size](#size), [skipFirst](#skipfirst), [uppercaseFirst](#uppercasefirst)

### asBonus

Return the value formatted with a bonus with a +/- prefix.

Usage: `{perception.asBonus}`

### capitalized

Return a Title Case form of this string, capitalizing the first word.
Does not transform the contents of parenthesis (like markdown URLs).

Usage: `{resource.languages.capitalized}`

### capitalizedList

Return a capitalized form of this string, capitalizing the first word of each clause.
Clauses are separated by commas or semicolons. Ignores conjunctions and parenthetical content.

Usage: `{resource.languages.capitalizedList}`

### first

First element in list

Usage: `{resource.components.first}`

### join

Return the given collection converted into a string and joined using the specified joiner.

Usage: `{resource.components.join(", ")}`

### joinConjunct

Return the given list joined into a single string, using a different delimiter for the last element.

Usage: `{resource.components.joinConjunct(", ", " or ")}`

### jsonString

Return the object as a JSON string

Usage: `{resource.components.getJsonString(resource)}`

### lowercase

Return the lowercase form of this string.
Does not transform the contents of parenthesis (like markdown URLs).

Usage: `{resource.name.lowercase}`

### pluralizeLabel

Return the string pluralized based on the size of the collection.

Usage: `{resource.name.pluralized(resource.components)}`

### prefixSpace

Return the given object as a string, with a space prepended if it's non-empty and non-null.

Usage: `{resource.name.prefixSpace}`

### quotedEscaped

Escape double quotes in a string (YAML/properties safe)

Usage: `{resource.components.quotedEscaped}`

### size

Return the size of a list

Usage: `{resource.components.size()}`

### skipFirst

Skip first element in list

Usage: `{resource.components.skipFirst}`

### uppercaseFirst

Return the text with a capitalized first letter (ignoring punctuation like '[')

Usage: `{resource.name.uppercaseFirst}`
