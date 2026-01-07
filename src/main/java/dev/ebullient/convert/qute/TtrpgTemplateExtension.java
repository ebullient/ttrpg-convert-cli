package dev.ebullient.convert.qute;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import dev.ebullient.convert.StringUtil;
import dev.ebullient.convert.io.JavadocVerbatim;
import dev.ebullient.convert.io.Tui;
import io.quarkus.qute.TemplateExtension;

/**
 * Qute template extensions for TTRPG data.
 *
 * Use these functions to help render TTRPG data in Qute templates.
 */
@TemplateExtension
public class TtrpgTemplateExtension {

    /**
     * Return the value formatted with a bonus with a +/- prefix.
     *
     * Usage: `{perception.asBonus}`
     */
    @JavadocVerbatim
    static String asBonus(Integer value) {
        return String.format("%+d", value);
    }

    /**
     * Return a Title Case form of this string, capitalizing the first word.
     * Does not transform the contents of parenthesis (like markdown URLs).
     *
     * Usage: `{resource.languages.capitalized}`
     */
    @JavadocVerbatim
    static String capitalized(String s) {
        return transformText(s, StringUtil::toTitleCase, false);
    }

    /**
     * Return a capitalized form of this string, capitalizing the first word of each clause.
     * Clauses are separated by commas or semicolons. Ignores conjunctions and parenthetical content.
     *
     * Usage: `{resource.languages.capitalizedList}`
     */
    @JavadocVerbatim
    static String capitalizedList(String s) {
        return transformText(s, StringUtil::uppercaseFirst, true);
    }

    /**
     * Return the text with a capitalized first letter (ignoring punctuation like '[')
     *
     * Usage: `{resource.name.uppercaseFirst}`
     */
    @JavadocVerbatim
    static String uppercaseFirst(String s) {
        return StringUtil.uppercaseFirst(s);
    }

    /**
     * Return the lowercase form of this string.
     * Does not transform the contents of parenthesis (like markdown URLs).
     *
     * Usage: `{resource.name.lowercase}`
     */
    @JavadocVerbatim
    static String lowercase(String s) {
        return transformText(s, String::toLowerCase, false);
    }

    /**
     * Transform the input text by applying the transformer function,
     * while respecting parentheses (both markdown URL and other parenthetical content)
     * and optionally handling clauses separated by commas or semicolons.
     *
     * @param input
     * @param transformer
     * @param byClauses
     * @return text transformed according to the specified rules
     */
    private static String transformText(String input, Function<String, String> transformer, boolean byClauses) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder out = new StringBuilder();
        StringBuilder buffer = new StringBuilder();
        int parenDepth = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '(' -> {
                    if (parenDepth == 0) {
                        // Flush current buffer before entering parens (like the existing tokenizer)
                        if (buffer.length() > 0) {
                            out.append(transformer.apply(buffer.toString()));
                            buffer.setLength(0);
                        }
                    }
                    parenDepth++;
                    out.append(c);
                }
                case ')' -> {
                    parenDepth--;
                    out.append(c);
                }
                case ',', ';' -> {
                    if (parenDepth == 0 && byClauses) {
                        // End of clause - transform and flush
                        if (buffer.length() > 0) {
                            String clause = buffer.toString();
                            String prefix = clause.startsWith("and ")
                                    ? "and "
                                    : clause.startsWith("or ")
                                            ? "or "
                                            : "";
                            out.append(prefix + transformer.apply(clause.substring(prefix.length())));
                            buffer.setLength(0);
                        }
                        out.append(c);

                        // Handle following whitespace
                        if (i + 1 < input.length() && Character.isWhitespace(input.charAt(i + 1))) {
                            i++; // Skip the next character (space)
                            out.append(input.charAt(i)); // Add the space
                        }
                    } else if (parenDepth > 0) {
                        out.append(c); // Inside parentheses, pass through
                    } else {
                        buffer.append(c); // Inside parentheses or not by clauses
                    }
                }
                default -> {
                    if (parenDepth > 0) {
                        out.append(c); // Pass through parenthetical content unchanged
                    } else {
                        buffer.append(c); // Accumulate transformable content
                    }
                }
            }
        }

        // Process any remaining buffer content (like the existing tokenizer)
        if (buffer.length() > 0) {
            out.append(transformer.apply(buffer.toString()));
        }

        return out.toString();
    }

    /**
     * Return the string pluralized based on the size of the collection.
     *
     * Usage: `{resource.name.pluralized(resource.components)}`
     */
    @JavadocVerbatim
    public static String pluralizeLabel(Collection<?> collection, String s) {
        return StringUtil.pluralize(s, collection.size(), true);
    }

    /**
     * Return the given object as a string, with a space prepended if it's non-empty and non-null.
     *
     * Usage: `{resource.name.prefixSpace}`
     */
    @JavadocVerbatim
    public static String prefixSpace(Object obj) {
        if (obj == null) {
            return "";
        }
        String s = obj.toString();
        return s.isEmpty() ? "" : (" " + s);
    }

    /**
     * Return the given collection converted into a string and joined using the specified joiner.
     *
     * Usage: `{resource.components.join(", ")}`
     */
    @JavadocVerbatim
    public static String join(Collection<?> collection, String joiner) {
        return StringUtil.join(joiner, collection);
    }

    /**
     * Return the given list joined into a single string, using a different delimiter for the last element.
     *
     * Usage: `{resource.components.joinConjunct(", ", " or ")}`
     */
    @JavadocVerbatim
    public static String joinConjunct(Collection<?> collection, String joiner, String lastjoiner) {
        return StringUtil.joinConjunct(joiner, lastjoiner, collection.stream().map(o -> o.toString()).toList());
    }

    /**
     * Return the object as a JSON string
     *
     * Usage: `{resource.components.getJsonString(resource)}`
     */
    @JavadocVerbatim
    public static String jsonString(Object o) {
        return Tui.jsonStringify(o);
    }

    /**
     * Skip first element in list
     *
     * Usage: `{resource.components.skipFirst}`
     */
    @JavadocVerbatim
    public static List<?> skipFirst(List<?> list) {
        return list.subList(1, list.size());
    }

    /**
     * First element in list
     *
     * Usage: `{resource.components.first}`
     */
    @JavadocVerbatim
    public static <T> T first(List<T> list) {
        return list.get(0);
    }

    /**
     * Return the size of a list
     *
     * Usage: `{resource.components.size()}`
     */
    @JavadocVerbatim
    public static int size(List<?> list) {
        return list.size();
    }

    /**
     * Escape double quotes in a string (YAML/properties safe)
     *
     * Usage: `{resource.components.quotedEscaped}`
     */
    @JavadocVerbatim
    public static String quotedEscaped(String s) {
        return dev.ebullient.convert.StringUtil.quotedEscaped(s);
    }

    /**
     * Escape double quotes in a string (YAML/properties safe)
     *
     * Usage: `{resource.components.quotedEscaped}`
     */
    @JavadocVerbatim
    public static String quotedEscaped(Optional<String> s) {
        return s.map(str -> dev.ebullient.convert.StringUtil.quotedEscaped(str)).orElse("");
    }
}
