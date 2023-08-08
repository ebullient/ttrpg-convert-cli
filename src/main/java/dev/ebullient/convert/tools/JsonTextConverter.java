package dev.ebullient.convert.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.QuteBase;

public interface JsonTextConverter<T extends IndexType> {
    String DICE_FORMULA = "[ +d\\d-‒]+";
    Pattern footnotePattern = Pattern.compile("\\{@footnote ([^}]+)}");

    void appendToText(List<String> inner, JsonNode target, String join);

    /**
     * Find and format footnotes referenced in the provided content
     *
     * @param text List of text lines (joined or not) that may contain footnotes
     * @param count The number of footnotes found previously (to avoid duplicates)
     * @return The number of footnotes found.
     */
    default int appendFootnotes(List<String> text, int count) {
        boolean pushed = parseState().push(true);
        try {
            List<String> footnotes = new ArrayList<>();
            text.replaceAll(input -> {
                // "Footnote tags; allows a footnote to be embedded
                // {@footnote directly in text|This is primarily for homebrew purposes, as the official texts (so far) avoid using footnotes},
                // {@footnote optional reference information|This is the footnote. References are free text.|Footnote 1, page 20}.",
                return footnotePattern.matcher(input)
                        .replaceAll((match) -> {
                            int index = count + footnotes.size() + 1;
                            String footnote = replaceText(match.group(1));
                            String[] parts = footnote.split("\\|");
                            footnotes.add(String.format("[^%s]: %s%s", index, parts[1],
                                    parts.length > 2 ? " (" + parts[2] + ")" : ""));

                            return String.format("%s[^%s]", parts[0], index);
                        });
            });

            if (footnotes.size() > 0) {
                maybeAddBlankLine(text);
                footnotes.forEach(f -> text.add(replaceText(f)));

            }
            return count + footnotes.size();
        } finally {
            parseState().pop(pushed);
        }
    }

    CompendiumConfig cfg();

    default ParseState parseState() {
        return cfg().parseState();
    }

    default String formatDice(String diceRoll) {
        int pos = diceRoll.indexOf(";");
        if (pos >= 0) {
            diceRoll = diceRoll.substring(0, pos);
        }
        return cfg().alwaysUseDiceRoller() && diceRoll.matches(DICE_FORMULA)
                ? "`dice: " + diceRoll + "|avg` (`" + diceRoll + "`)"
                : '`' + diceRoll + '`';
    }

    default String replaceWithDiceRoller(String text) {
        return text.replaceAll("\\{@h}([ \\d]+) \\(\\{@damage (" + DICE_FORMULA + ")}\\)",
                "Hit: `dice: $2|avg` (`$2`)")
                .replaceAll("plus ([\\d]+) \\(\\{@damage (" + DICE_FORMULA + ")}\\)",
                        "plus `dice: $2|avg` (`$2`)")
                .replaceAll("(takes?) [\\d]+ \\(\\{@damage (" + DICE_FORMULA + ")}\\)",
                        "$1 `dice: $2|avg` (`$2`)")
                .replaceAll("(takes?) [\\d]+ \\(\\{@dice (" + DICE_FORMULA + ")}\\)",
                        "$1 `dice: $2|avg` (`$2`)")
                .replaceAll("\\{@hit (\\d+)} to hit", "`dice: d20+$1` (+$1 to hit)")
                .replaceAll("\\{@hit (-\\d+)} to hit", "`dice: d20-$1` (-$1 to hit)")
                .replaceAll("\\{@hit (\\d+)}", "`dice: d20+$1` (+$1)")
                .replaceAll("\\{@hit (-\\d+)}", "`dice: d20-$1` (-$1)")
                .replaceAll("\\{@d20 (\\d+?)}", "`dice: d20+$1` (+$1)")
                .replaceAll("\\{@d20 (-\\d+?)}", "`dice: d20-$1` (-$1)");
    }

    default Iterable<JsonNode> iterableElements(JsonNode source) {
        if (source == null) {
            return List.of();
        }
        return source::elements;
    }

    default Iterable<JsonNode> iterableEntries(JsonNode source) {
        JsonNode entries = source.get("entries");
        if (entries == null) {
            return List.of();
        }
        return entries::elements;
    }

    default Iterable<Entry<String, JsonNode>> iterableFields(JsonNode source) {
        if (source == null) {
            return List.of();
        }
        return source::fields;
    }

    default Iterable<String> iterableFieldNames(JsonNode source) {
        if (source == null) {
            return List.of();
        }
        return source::fieldNames;
    }

    default String join(String joiner, Collection<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(joiner, list).trim();
    }

    default String joinConjunct(String lastJoiner, List<String> list) {
        return joinConjunct(list, ", ", lastJoiner, false);
    }

    default String joinConjunct(List<String> list, String joiner, String lastJoiner, boolean nonOxford) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        if (list.size() == 2) {
            return String.join(lastJoiner, list);
        }

        int pause = list.size() - 2;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < list.size(); ++i) {
            out.append(list.get(i));

            if (i < pause) {
                out.append(joiner);
            } else if (i == pause) {
                if (!nonOxford) {
                    out.append(joiner.trim());
                }
                out.append(lastJoiner);
            }
        }
        return out.toString();
    }

    String linkify(T type, String s);

    /** Internal / recursive parse */
    default void appendListItem(List<String> text, JsonNode itemNode) {
        List<String> inner = new ArrayList<>();
        appendToText(inner, SourceField.entry.getFrom(itemNode), null);
        appendToText(inner, SourceField.entries.getFrom(itemNode), null);
        if (prependField(itemNode, SourceField.name, inner)) {
            maybeAddBlankLine(text);
        }
        text.addAll(inner);
    }

    default String flattenToString(JsonNode node) {
        List<String> text = new ArrayList<>();
        appendToText(text, node, null);
        return String.join("\n", text);
    }

    /**
     * Maybe add a blank line to the list containing parsed text.
     * Imperfect, but only add a blank line if the previous line is
     * not already blank.
     *
     * @param text Text to analyze and maybe add a blank line to
     */
    default void maybeAddBlankLine(List<String> text) {
        if (text.size() > 0 && !text.get(text.size() - 1).isBlank()) {
            text.add("");
        }
    }

    /** Internal / recursive parse */
    default String nestedEmbed(List<String> content) {
        int embedDepth = content.stream()
                .filter(s -> s.matches("^`+$"))
                .map(String::length)
                .max(Integer::compare).orElse(2);
        char[] ticks = new char[embedDepth + 1];
        Arrays.fill(ticks, '`');
        return new String(ticks);
    }

    /** Internal / recursive parse */
    default boolean prependField(JsonNode entry, JsonNodeReader field, List<String> inner) {
        String n = field.getTextOrNull(entry);
        if (n != null) {
            n = replaceText(n.trim());
            if (inner.isEmpty()) {
                inner.add(n);
            } else {
                n = n.replace(":", "");
                n = "**" + n + "** ";
                inner.set(0, n + inner.get(0));
                return true;
            }
        }
        return false;
    }

    /** Internal / recursive parse */
    default void prependText(String prefix, List<String> inner) {
        if (inner.isEmpty()) {
            inner.add(prefix);
        } else if (inner.get(0).isEmpty() && inner.size() > 1) {
            // leading blank line
            inner.set(1, prependText(prefix, inner.get(1)));
        } else {
            // update first line
            inner.set(0, prependText(prefix, inner.get(0)));
        }
    }

    /** Internal / recursive parse */
    default String prependText(String prefix, String text) {
        return text.startsWith(prefix) ? text : prefix + text;
    }

    /** Internal / recursive parse */
    default void prependTextMakeListItem(List<String> text, JsonNode e, String prepend, String continuation) {
        List<String> inner = new ArrayList<>();
        appendToText(inner, e, null);
        if (inner.size() > 0) {
            prependText("- " + prepend, inner);
            inner.forEach(i -> text.add(i.startsWith("-") || i.isBlank() ? i : continuation + i));
        }
    }

    /** Internal / recursive parse */
    default List<String> removePreamble(List<String> content) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        boolean hasYaml = content.get(0).equals("---");
        int endYaml = -1;
        int start = -1;
        for (int i = 0; i < content.size(); i++) {
            String line = content.get(i);
            if (line.equals("---") && hasYaml && i > 0 && endYaml < 0) {
                endYaml = i;
            } else if (line.startsWith("%%--")) {
                start = i;
                break;
            }
        }
        if (start < 0 && endYaml > 0) {
            start = endYaml; // if no other marker present, lop off the yaml header
        }
        if (start >= 0) {
            // remove until start
            content.subList(0, start + 1).clear();
        }
        return content;
    }

    /**
     * Embed rendered contents of the specified resource
     *
     * @param text List of text content should be added to
     * @param resource QuteBase containing required template resource data
     * @param admonition Type of embedded/encapsulating admonition
     * @param prepend Text to prepend at beginning of admonition (e.g. title)
     */
    default void renderEmbeddedTemplate(List<String> text, QuteBase resource, String admonition, List<String> prepend) {
        prepend = prepend == null ? List.of() : prepend; // ensure non-null
        boolean pushed = parseState().push(resource.sources());
        try {
            String rendered = tui().renderEmbedded(resource);
            List<String> inner = removePreamble(new ArrayList<>(List.of(rendered.split("\n"))));

            String backticks = nestedEmbed(inner);
            maybeAddBlankLine(text);
            text.add(backticks + "ad-embed-" + admonition);
            text.addAll(prepend);
            text.addAll(inner);
            text.add(backticks);
        } finally {
            parseState().pop(pushed);
        }
    }

    /**
     * Add rendered contents of an (always) inline template
     * to collected text
     *
     * @param text List of text content should be added to
     * @param resource QuteBase containing required template resource data
     * @param admonition Type of inline admonition
     */
    default void renderInlineTemplate(List<String> text, QuteBase resource, String admonition) {
        String rendered = tui().renderEmbedded(resource);
        List<String> inner = List.of(rendered.split("\n"));

        maybeAddBlankLine(text);
        if (admonition == null) {
            text.addAll(inner);
        } else {
            String backticks = nestedEmbed(inner);
            text.add(backticks + "ad-inline-" + admonition);
            text.addAll(inner);
            text.add(backticks);
        }
    }

    String replaceText(String s);

    default String slugify(String s) {
        return Tui.slugify(s);
    }

    default Stream<JsonNode> streamOf(JsonNode source) {
        if (source == null) {
            return Stream.of();
        }
        return StreamSupport.stream(iterableElements(source).spliterator(), false);
    }

    default String toAnchorTag(String x) {
        return x.replace(" ", "%20")
                .replace(":", "")
                .replace(".", "")
                .replace('‑', '-');
    }

    default List<String> toListOfStrings(JsonNode source) {
        if (source == null) {
            return List.of();
        } else if (source.isTextual()) {
            return List.of(source.asText());
        }
        List<String> list = tui().readJsonValue(source, Tui.LIST_STRING);
        return list == null ? List.of() : list;
    }

    default String toTitleCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Arrays
                .stream(text.split(" "))
                .map(word -> word.isEmpty()
                        ? word
                        : Character.toTitleCase(word.charAt(0)) + word
                                .substring(1)
                                .toLowerCase())
                .collect(Collectors.joining(" "));
    }

    Tui tui();

    enum SourceField implements JsonNodeReader {
        abbreviation,
        entry,
        entries,
        id,
        items,
        name,
        page,
        source,
        type,
    }
}
