package dev.ebullient.convert.tools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.QuteBase;

public interface JsonTextConverter<T extends IndexType> {
    public static String DICE_FORMULA = "[ +d\\d-‒]+";
    Pattern footnotePattern = Pattern.compile("\\{@footnote ([^}]+)}");

    void appendToText(List<String> inner, JsonNode target, String heading);

    CompendiumConfig cfg();

    default ParseState parseState() {
        return cfg().parseState();
    }

    default String formatDice(String diceRoll) {
        // needs to be escaped: \\ to escape the \\ so it is preserved in the output
        String avg = parseState().inMarkdownTable() ? "\\\\|avg" : "|avg";
        int pos = diceRoll.indexOf(";");
        if (pos >= 0) {
            diceRoll = diceRoll.substring(0, pos);
        }
        return cfg().alwaysUseDiceRoller() && diceRoll.matches(JsonTextConverter.DICE_FORMULA)
                ? "`dice: " + diceRoll + avg + "` (`" + diceRoll + "`)"
                : '`' + diceRoll + '`';
    }

    default String replaceWithDiceRoller(String text) {
        return text
                .replaceAll("\\{@hit (\\d+)}", "`dice: d20+$1` (+$1)")
                .replaceAll("\\{@hit (-\\d+)}", "`dice: d20-$1` (-$1)")
                .replaceAll("\\{@d20 (\\d+?)}", "`dice: d20+$1` (+$1)")
                .replaceAll("\\{@d20 (-\\d+?)}", "`dice: d20-$1` (-$1)");
    }

    default String simplifyFormattedDiceText(String text) {
        return text
                .replaceAll("` \\((" + JsonTextConverter.DICE_FORMULA + ")\\) to hit", "` ($1 to hit)")
                .replaceAll(" \\d+ \\((`dice:" + JsonTextConverter.DICE_FORMULA + "\\|avg` \\(`"
                        + JsonTextConverter.DICE_FORMULA + "`\\))\\)", " $1");
    }

    /** Tokenizer: use a stack of StringBuilders to deal with nested tags */
    default String replaceTokens(String input, BiFunction<String, Boolean, String> tokenResolver) {
        if (input == null || input.isBlank()) {
            return input;
        }

        StringBuilder out = new StringBuilder();
        ArrayDeque<StringBuilder> stack = new ArrayDeque<>();
        StringBuilder buffer = new StringBuilder();
        boolean foundDice = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            //char c2 = i + 1 < input.length() ? input.charAt(i + 1) : NUL;

            switch (c) {
                case '{':
                    stack.push(buffer);
                    buffer = new StringBuilder();
                    buffer.append(c);
                    break;
                case '}':
                    buffer.append(c);
                    String replace = tokenResolver.apply(buffer.toString(), stack.size() > 1);
                    foundDice |= replace.contains("`dice:");
                    if (stack.isEmpty()) {
                        tui().warnf("Mismatched braces? Found '}' with an empty stack. Input: %s", input);
                    } else {
                        buffer = stack.pop();
                    }
                    buffer.append(replace);
                    break;
                default:
                    buffer.append(c);
                    break;
            }
        }

        if (buffer.length() > 0) {
            out.append(buffer);
        }
        return foundDice
                ? simplifyFormattedDiceText(out.toString())
                : out.toString();
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
        return flattenToString(node, "\n");
    }

    default String flattenToString(JsonNode node, String join) {
        List<String> text = new ArrayList<>();
        appendToText(text, node, null);
        return String.join(join, text);
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
        String n = field.replaceTextFrom(entry, this);
        return prependField(entry, n, inner);
    }

    default boolean prependField(JsonNode entry, String name, List<String> inner) {
        if (name != null) {
            name = replaceText(name.trim());
            if (inner.isEmpty()) {
                inner.add(name);
            } else if (inner.get(0).startsWith("|") || inner.get(0).startsWith(">")) {
                // we have a table or a blockquote
                name = "**" + name + "** ";
                inner.add(0, "");
                inner.add(0, name);
                return true;
            } else {
                name = name.replace(":", "");
                name = "**" + name + ".** ";
                inner.set(0, name + inner.get(0));
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
        List<String> inner = removePreamble(new ArrayList<>(List.of(rendered.split("\n"))));

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

    default ArrayNode ensureArray(JsonNode source) {
        if (source == null || source.isNull()) {
            return Tui.MAPPER.createArrayNode();
        }
        if (source.isArray()) {
            return (ArrayNode) source;
        }
        return Tui.MAPPER.createArrayNode().add(source);
    }

    default ObjectNode ensureObjectNode(JsonNode source) {
        if (source == null || source.isNull()) {
            return Tui.MAPPER.createObjectNode();
        }
        if (source.isArray()) {
            throw new IllegalArgumentException("Can not make an ObjectNode from an ArrayNode");
        }
        return (ObjectNode) source;
    }

    default Stream<JsonNode> streamOf(JsonNode source) {
        if (source == null || source.isNull()) {
            return Stream.of();
        }
        if (source.isObject()) {
            return Stream.of(source);
        }
        return StreamSupport.stream(iterableElements(source).spliterator(), false);
    }

    default Stream<String> streamOfFieldNames(JsonNode source) {
        if (source == null) {
            return Stream.of();
        }
        return StreamSupport.stream(iterableFieldNames(source).spliterator(), false);
    }

    default String toAnchorTag(String x) {
        return Tui.toAnchorTag(x);
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
        _class_("class"),
        entry,
        entries,
        id,
        items,
        _meta,
        name,
        note,
        page,
        source,
        type;

        final String nodeName;

        SourceField() {
            this.nodeName = this.name();
        }

        SourceField(String nodeName) {
            this.nodeName = nodeName;
        }

        public String nodeName() {
            return nodeName;
        }

        @Override
        public String getTextOrDefault(JsonNode x, String value) {
            String text = JsonNodeReader.super.getTextOrDefault(x, value);
            return this == name
                    ? text.replace("\u00A0", "").trim()
                    : text;
        }

        @Override
        public String getTextOrEmpty(JsonNode x) {
            String text = JsonNodeReader.super.getTextOrEmpty(x);
            return this == name
                    ? text.replace("\u00A0", "").trim()
                    : text;
        }

        @Override
        public String getTextOrNull(JsonNode x) {
            String text = JsonNodeReader.super.getTextOrNull(x);
            return this == name && text != null
                    ? text.replace("\u00A0", "").trim()
                    : text;
        }

        @Override
        public String replaceTextFrom(JsonNode node, JsonTextConverter<?> replacer) {
            String text = JsonNodeReader.super.replaceTextFrom(node, replacer);
            return this == name && text != null
                    ? text.replace("\u00A0", "").trim()
                    : text;
        }
    }
}
