package dev.ebullient.convert.tools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.StringUtil;
import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.CompendiumConfig.DiceRoller;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.qute.QuteUtil;

import static dev.ebullient.convert.StringUtil.isPresent;

public interface JsonTextConverter<T extends IndexType> {
    public static String DICE_FORMULA = "[ +d\\d-‒]+";
    public static String DICE_TABLE_HEADER = "\\| dice: \\d*d\\d+ \\|.*";
    Pattern footnotePattern = Pattern.compile("\\{@footnote ([^}]+)}");

    void appendToText(List<String> inner, JsonNode target, String heading);

    Tui tui();

    CompendiumConfig cfg();

    default ObjectMapper mapper() {
        return Tui.MAPPER;
    }

    default ParseState parseState() {
        return cfg().parseState();
    }

    default JsonNode copyNode(JsonNode sourceNode) {
        try {
            return mapper().readTree(sourceNode.toString());
        } catch (JsonProcessingException ex) {
            tui().errorf(ex, "Unable to copy %s", sourceNode.toString());
            System.exit(5);
            return null;
        }
    }

    default JsonNode createNode(String source) {
        try {
            return mapper().readTree(source);
        } catch (JsonProcessingException ex) {
            tui().errorf(ex, "Unable to create node from %s", source);
            System.exit(5);
            return null;
        }
    }

    default boolean isArrayNode(JsonNode node) {
        return node != null && node.isArray();
    }

    default boolean isObjectNode(JsonNode node) {
        return node != null && node.isObject();
    }

    default JsonNode objectIntersect(JsonNode a, JsonNode b) {
        if (a.equals(b)) {
            return a;
        }
        ObjectNode x = Tui.MAPPER.createObjectNode();
        for (String k : iterableFieldNames(a)) {
            if (a.get(k).equals(b.get(k))) {
                x.set(k, a.get(k));
            } else if (isObjectNode(a.get(k)) && isObjectNode(b.get(k))) {
                x.set(k, objectIntersect(a.get(k), b.get(k)));
            }
        }
        return x;
    }

    default String formatDice(String diceRoll) {
        DiceRoller roller = cfg().useDiceRoller();
        boolean suppressInYaml = parseState().inTrait() && roller.useFantasyStatblocks();

        int pos = diceRoll.indexOf(";");
        if (pos >= 0) {
            diceRoll = diceRoll.substring(0, pos);
        }
        if (roller == DiceRoller.disabled) {
            return '`' + diceRoll + '`';
        } else if (suppressInYaml) {
            return diceRoll;
        }

        // needs to be escaped: \\ to escape the \\ so it is preserved in the output
        String avg = parseState().inMarkdownTable() ? "\\\\|avg\\\\|noform" : "|avg|noform";
        return diceRoll.matches(JsonTextConverter.DICE_FORMULA)
                ? "`dice: " + diceRoll + avg + "` (`" + diceRoll + "`)"
                : '`' + diceRoll + '`';
    }

    default String replaceWithDiceRoller(String text) {
        DiceRoller roller = cfg().useDiceRoller();
        boolean suppressInYaml = parseState().inTrait() && roller.useFantasyStatblocks();

        String posGroup = "\\+? ?(\\d+)";
        String negGroup = "(-\\d+)";

        String hitPos = "\\{@(hit|h) " + posGroup + "}";
        String hitNeg = "\\{@(hit|h) " + negGroup + "}";
        String d20Pos = "\\{@d20 " + posGroup + "}";
        String d20Neg = "\\{@d20 " + negGroup + "}";
        String modScorePos = "\\{@d20 " + posGroup + "\\|([^|}]+)}";
        String modScoreNeg = "\\{@d20 " + negGroup + "\\|([^|}]+)}";
        String altScorePos = "\\{@d20 " + posGroup + "\\|[^}|]*?\\|([^}]+)}";
        String altScoreNeg = "\\{@d20 " + negGroup + "\\|[^}|]*?\\|([^}]+)}";
        String altText = "\\{@hit ([^}|]+)\\|([^}]+)}";
        String nonFormula = "\\{@hit ([^}]+)}";

        if (roller == DiceRoller.disabled) {
            return text
                    .replaceAll(hitPos, "`+$2`")
                    .replaceAll(hitNeg, "`$2`")
                    .replaceAll(altScorePos, "$2 (`+$1`)")
                    .replaceAll(altScoreNeg, "$2 (`$1`)")
                    .replaceAll(modScorePos, "`+$1` (`$2`)")
                    .replaceAll(modScoreNeg, "`$1` (`$2`)")
                    .replaceAll(d20Pos, "`+$1`")
                    .replaceAll(d20Neg, "`$1`")
                    .replaceAll(altText, "$2")
                    .replaceAll(nonFormula, "`$1`");
        } else if (suppressInYaml) {
            // No backticks or formatting. Fantasy Statblocks will render
            return text
                    .replaceAll(hitPos, "+$2")
                    .replaceAll(hitNeg, "$2")
                    .replaceAll(d20Pos, "+$1")
                    .replaceAll(d20Neg, "$1")
                    .replaceAll(altScorePos, "$2 (+$1)")
                    .replaceAll(altScoreNeg, "$2 ($1)")
                    .replaceAll(modScorePos, "+$1 ($2)")
                    .replaceAll(modScoreNeg, "$1 ($2)")
                    .replaceAll(altText, "$2")
                    .replaceAll(nonFormula, "$1");
        }

        String dtxt = parseState().inMarkdownTable() ? "\\\\|text(" : "|text(";

        return text
                .replaceAll(hitPos, "`dice: d20+$2` (`+$2`)")
                .replaceAll(hitNeg, "`dice: d20$2` (`$2`)")
                .replaceAll(d20Pos, "`dice: d20+$1` (`+$1`)")
                .replaceAll(d20Neg, "`dice: d20$1` (`$1`)")
                .replaceAll(altScorePos, "$2 (`dice: d20+$1" + dtxt + "+$1)`)")
                .replaceAll(altScoreNeg, "$2 (`dice: d20$1" + dtxt + "$1)`)")
                .replaceAll(modScorePos, "`+$1` (`$2`)")
                .replaceAll(modScoreNeg, "`$1` (`$2`)")
                .replaceAll(altText, "$2")
                .replaceAll(nonFormula, "`$1`");
    }

    default String simplifyFormattedDiceText(String text) {
        // 7 (`dice: 2d6|avg|noform` (`2d6`)) --> `dice: 2d6|text(7)` (`2d6`)
        String dtxt = parseState().inMarkdownTable() ? "\\\\|text(" : "|text(";
        return text
                .replaceAll("` \\((" + DICE_FORMULA + ")\\) to hit", "` ($1 to hit)")
                .replaceAll(" (\\d+) \\(`dice:[^`]+` \\(`([^`]+)`\\)\\)",
                        " `dice:$2" + dtxt + "$1)` (`$2`)");
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
        if (!source.isArray()) {
            return List.of(source);
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
        if (!text.isEmpty() && !text.get(text.size() - 1).isBlank()) {
            text.add("");
        }
    }

    /**
     * Returns a string which contains the backticks required to create an
     * admonition around the given {@code content}. Internal / recursive parse.
     */
    default String nestedEmbed(List<String> content) {
        int embedDepth = content.stream()
                .map(String::trim)
                .filter(s -> s.matches("`+"))
                .map(String::length)
                .max(Integer::compare).orElse(2);
        char[] ticks = new char[embedDepth + 1];
        Arrays.fill(ticks, '`');
        return new String(ticks);
    }

    /** Internal / recursive parse */
    default boolean prependField(JsonNode entry, JsonNodeReader field, List<String> inner) {
        String n = field.replaceTextFrom(entry, this);
        return prependField(n, inner);
    }

    default boolean prependField(String name, List<String> inner) {
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
     * Return the rendered contents of the specified resource
     *
     * @param resource QuteBase containing required template resource data
     * @param admonition Type of embedded/encapsulating admonition
     */
    default String renderEmbeddedTemplate(QuteBase resource, String admonition) {
        List<String> inner = new ArrayList<>();
        renderEmbeddedTemplate(inner, resource, admonition, List.of());
        return String.join("\n", inner);
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
        boolean pushed = parseState().push(resource.sources());
        try {
            String rendered = tui().renderEmbedded(resource);
            List<String> inner = new ArrayList<>(prepend);
            inner.addAll(removePreamble(new ArrayList<>(List.of(rendered.split("\n")))));

            maybeAddBlankLine(text);
            if (admonition != null) {
                wrapAdmonition(inner, "embed-" + admonition);
            } else {
                balanceBackticks(inner);
            }
            text.addAll(inner);
        } finally {
            parseState().pop(pushed);
        }
    }

    /**
     * Return the rendered contents of an (always) inline template.
     *
     * @param resource QuteUtil containing required template resource data
     * @param admonition Type of inline admonition
     */
    default String renderInlineTemplate(QuteUtil resource, String admonition) {
        List<String> inner = new ArrayList<>();
        renderInlineTemplate(inner, resource, admonition);
        return String.join("\n", inner);
    }

    /**
     * Add rendered contents of an (always) inline template
     * to collected text
     *
     * @param text List of text content should be added to
     * @param resource QuteUtil containing required template resource data
     * @param admonition Type of inline admonition
     */
    default void renderInlineTemplate(List<String> text, QuteUtil resource, String admonition) {
        String rendered = tui().renderEmbedded(resource);
        List<String> inner = removePreamble(new ArrayList<>(List.of(rendered.split("\n"))));

        maybeAddBlankLine(text);
        if (admonition != null) {
            wrapAdmonition(inner, "inline-" + admonition);
        } else {
            balanceBackticks(inner);
        }
        text.addAll(inner);
    }

    /** Wrap {@code inner} in an admonition with the name {@code admonition}. */
    default void wrapAdmonition(List<String> inner, String admonition) {
        if (admonition == null || admonition.isEmpty() || inner == null || inner.isEmpty()) {
            return;
        }
        String backticks = nestedEmbed(inner);
        inner.add(0, backticks + "ad-" + admonition);
        inner.add(inner.size(), backticks);
    }

    /**
     * Add backticks to the outermost admonition in {@code inner} so that it
     * correctly wraps the rest of the text. Has no effect if the first non-empty
     * line of {@code inner} is not an admonition.
     */
    private void balanceBackticks(List<String> inner) {
        int[] indices = outerAdmonitionIndices(inner);
        if (indices == null) {
            // There was no outer admonition, so do nothing.
            return;
        }
        int adEndIdx = indices[1];
        int adStartIdx = indices[0];
        String firstLine = inner.get(adStartIdx);
        // Must be done in this order so the indices don't change
        inner.remove(adEndIdx);
        inner.remove(adStartIdx);
        String backticks = nestedEmbed(inner);
        inner.add(adStartIdx, backticks + firstLine.replaceFirst("^`+", ""));
        inner.add(adEndIdx, backticks);
    }

    /**
     * Return the indices of the start and end of the admonition which wraps {@code inner}, or null if there is no
     * admonition.
     */
    default int[] outerAdmonitionIndices(List<String> inner) {
        int[] presentIndices = IntStream.range(0, inner.size())
                .filter(idx -> isPresent(inner.get(idx)))
                .toArray();
        if (presentIndices.length < 2) {
            // We need at least two non-empty lines to have one each for the opening and closing
            // admonition lines.
            return null;
        }
        int firstLineIdx = presentIndices[0];

        String firstLine = inner.get(firstLineIdx);
        if (!firstLine.matches("```+ad[\\s\\S]+")) {
            return null;
        }
        int lastLineIdx = presentIndices[presentIndices.length - 1];
        String lastLine = inner.get(lastLineIdx);
        if (!lastLine.matches("```+")) {
            // we expect the last non-empty line to contain the closing set of backticks
            tui().debugf("Expected line %d to close backticks but was instead '%s'", lastLineIdx, lastLine);
            return null;
        }
        return new int[] { firstLineIdx, lastLineIdx };
    }

    String replaceText(String s);

    default String replaceText(JsonNode input) {
        if (input == null) {
            return null;
        }
        if (input.isObject() || input.isArray()) {
            throw new IllegalArgumentException("Can only replace text for textual nodes: " + input);
        }
        return replaceText(input.asText());
    }

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

    default Stream<Entry<String, JsonNode>> streamPropsExcluding(JsonNode source, JsonNodeReader... excludingKeys) {
        if (source == null || !source.isObject()) {
            return Stream.of();
        }
        return source.properties().stream()
                .filter(e -> Arrays.stream(excludingKeys).noneMatch(s -> e.getKey().equalsIgnoreCase(s.name())));
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
