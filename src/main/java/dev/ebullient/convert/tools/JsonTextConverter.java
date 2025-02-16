package dev.ebullient.convert.tools;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.toAnchorTag;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.qute.QuteUtil;
import dev.ebullient.convert.tools.ParseState.DiceFormulaState;

public interface JsonTextConverter<T extends IndexType> {
    public static String DICE_FORMULA = "[ +d\\d-]+";
    public static String DICE_TABLE_HEADER = "\\| dice: \\d*d\\d+ \\|.*";

    static final Pattern dicePattern = Pattern.compile("\\{@("
            + "dice|autodice|damage|h |hit|d20|initiative|scaledice|scaledamage"
            + ") ?([^}]+)}");
    static final Pattern dicePatternWithSpan = Pattern.compile("(.+)(<span[^>]+>)(.+)(</span>)");
    static final Pattern footnotePattern = Pattern.compile("\\{@footnote ([^}]+)}");
    static final Pattern textAverageRoll = Pattern.compile(" (\\d+) \\((`dice:[^`]+text\\(([^)]+)\\)`)\\)");
    static final Pattern averageRoll = Pattern.compile(" (\\d+) \\(`(dice:[^`]+)` (\\([^)]+\\))\\)");

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

    default boolean isEmpty(JsonNode node) {
        return node == null || node.isNull()
                || (node.isTextual() && node.asText().isBlank()
                        || (node.isArray() && node.size() == 0)
                        || (node.isObject() && node.size() == 0));
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

    default String replaceWithDiceRoller(String input) {
        Matcher m = dicePattern.matcher(input);
        if (!m.find()) {
            return input;
        }
        if (m.groupCount() < 2) {
            tui().warnf(Msg.UNKNOWN, "Unknown/Invalid dice formula Input: %s", input);
            return input;
        }

        DiceFormulaState formulaState = parseState().diceFormulaState();

        String tag = m.group(1);
        String[] parts = m.group(2).split("\\|");

        String rollString = parts[0].trim();
        String displayText = parts.length > 1 ? parts[1].trim() : null;
        String scaleSkillName = parts.length > 2 ? parts[2].trim() : null;

        return switch (tag) {
            case "d20", "h", "hit", "initiative" -> {
                // {@d20 -4}, {@d20 -2 + PB},
                // {@d20 0|10}, {@d20 2|+2|Perception}, {@d20 -1|\u22121|Father Belderone}
                // {@hit +7}, {@hit 6|+6|Slam}, {@hit 6|+6 bonus}, {@hit +3|+3 to hit}
                // @initiative -- like @hit
                String posGroup = "(?<!-)\\+? ?(\\d+)";
                String mod = rollString.replaceAll(posGroup, "+$1");
                String mod20 = "1d20" + mod;

                if (scaleSkillName != null) {
                    //  Perception (`+2`), Father Belderone (`-1`), Slam (`+6`)
                    yield scaleSkillName + " ("
                            + formatDice(mod20, codeString(mod, formulaState), formulaState, false, false)
                            + ")";
                }

                String formattedRoll = formatDice(mod20,
                        codeString(mod, formulaState),
                        formulaState, false, false);

                if (displayText != null) {
                    if (tag.equals("hit")) {
                        // +6 bonus, +3 to hit
                        yield displayText;
                    }
                    // non-standard order: `+0` (`10`)
                    yield formattedRoll + " (" + codeString(displayText, formulaState) + ")";
                }
                yield formattedRoll;
            }
            case "scaledamage", "scaledice" -> {
                // damage of 2d6 or 3d6 at level 1: {@scaledamage 2d6;3d6|2-9|1d6} for each level beyond 2nd;
                // roll 2d6 when using 1 psi point: {@scaledice 2d6|1,3,5,7,9|1d6|psi|extra amount} for each additional psi point spent
                // format: {@scaledice 2d6;3d6|2-8,9|1d6|psi|display text} (or @scaledamage)
                // [baseRoll, progression, addPerProgress, renderMode, displayText]
                yield parts.length > 4
                        ? formatDice(scaleSkillName, parts[4].trim(), formulaState, true, true)
                        : formatDice(scaleSkillName, codeString(scaleSkillName, formulaState), formulaState, true, false);
            }
            // {@dice 1d2-2+2d3+5} for regular dice rolls
            // {@dice 1d6;2d6} for multiple options;
            // {@dice 1d6 + #$prompt_number:min=1,title=Enter a Number!,default=123$#} for input prompts
            // --> prompts will have been replaced with default value: {@dice 1d6 + <span...>lotsofstuff</span>}
            // {@dice 1d20+2|display text}
            // {@dice 1d20+2|display text|rolled by name}
            // {@damage 1d12+3}
            // @autodice -- like @dice
            default -> {
                String[] alternatives = rollString.split(";");
                if (displayText == null && alternatives.length > 1) {
                    for (int i = 0; i < alternatives.length; i++) {
                        String coded = codeString(alternatives[i], formulaState);
                        alternatives[i] = formatDice(alternatives[i], coded, formulaState, true, false);
                    }
                    displayText = String.join(" or ", alternatives);
                }
                yield formatDice(rollString, displayText, formulaState, true, true);
            }
        };
    }

    default String formatDice(String diceRoll, String displayText, DiceFormulaState formulaState, boolean useAverage,
            boolean appendFormula) {
        if (diceRoll.contains(";")) {
            return displayText;
        }
        if (diceRoll.contains("<span")) {
            // There is (or was) an input prompt here.
            Matcher m = dicePatternWithSpan.matcher(diceRoll);
            if (m.matches()) {
                displayText = "%s%s%s".formatted(m.group(2), codeString(m.group(1) + m.group(3), formulaState), m.group(4));
            }
        }
        if (displayText == null && diceRoll.contains("summonSpellLevel")) {
            displayText = codeString(diceRoll.replace(" + summonSpellLevel", ""), formulaState)
                    + " + the spell's level";
        } else if (displayText != null && displayText.contains("summonSpellLevel")) {
            displayText = displayText.replace("summonSpellLevel", "the spell's level");
        }
        if (displayText == null && diceRoll.contains("summonClassLevel")) {
            displayText = codeString(diceRoll.replace(" + summonClassLevel", ""), formulaState)
                    + " + your class level";
        } else if (displayText != null && displayText.contains("summonClassLevel")) {
            displayText = displayText.replace("summonClassLevel", "your class level");
        }

        String dice = codeString(diceRoll.replace("1d20", ""), formulaState);

        if (diceRoll.matches(JsonTextConverter.DICE_FORMULA)) {
            String postText = appendFormula ? " (" + dice + ")" : "";
            if (formulaState.noRoller()) {
                return displayText == null
                        ? dice
                        : displayText + postText;
            }
            return diceFormula(diceRoll.replace(" ", ""), displayText, useAverage) + postText;
        } else {
            // Most likely have display text here. (Prompt, spell level, class level most likely cause)
            return displayText == null
                    ? dice
                    : displayText;
        }
    }

    default String diceFormula(String diceRoll) {
        // Only a dice formula in the roll part. May also have display text.
        return "`dice: " + diceRoll + "`";
    }

    default String diceFormula(String diceRoll, String displayText, boolean average) {
        // don't escape the dice formula here.
        // see simplifyFormattedDiceText (called consistently from replaceText)
        String noform = "|noform";
        String avg = "|avg";
        String dtxt = "|text(";
        String textValue = displayText == null ? "" : displayText.replace("`", "");

        // Only a dice formula in the roll part. May also have display text.
        return "`dice:" + diceRoll + noform +
                (average ? avg : "") +
                (displayText == null ? "`" : dtxt + textValue + ")`");
    }

    default String codeString(String text, DiceFormulaState formulaState) {
        text = text.replace("1d20", "");
        return formulaState.plainText() ? text : "`" + text + "`";
    }

    // reduce dice strings.. when parsing tags, we can't see leadng average
    default String simplifyFormattedDiceText(String text) {
        DiceFormulaState formulaState = parseState().diceFormulaState();
        String dtxt = "|text(";

        // 26 (`dice:1d20+8|noform|text(+8)`) --> `dice:1d20+8|noform|text(26)` (`+8`)
        text = textAverageRoll.matcher(text).replaceAll((match) -> {
            String replaceText = "(" + match.group(3) + ")";
            String avgValue = "(" + match.group(1) + ")";
            return " " + match.group(2).replace(replaceText, avgValue)
                    + " (" + codeString(match.group(3), formulaState) + ")";
        });

        if (text.contains("reach levels")) {
            // don't look for averages here. This is spell progression
        } else {
            // otherwise look for average rolls
            // 7 (`dice:1d6+4|noform|avg` (`1d6 + 4`)) --> `dice:1d6+4|noform|avg|text(7)` (`1d6 + 4`)
            // 7 (`dice:2d6|noform|avg` (`2d6`)) --> `dice:2d6|noform|avg|text(7)` (`2d6`)
            text = averageRoll.matcher(text).replaceAll((match) -> {
                String dice = match.group(2) + dtxt + match.group(1) + ")";
                return " `" + dice + "` " + match.group(3);
            });
        }

        return parseState().inMarkdownTable()
                ? text.replace("|", "\\|")
                : text;
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
        if (isPresent(name)) {
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

    default Stream<Entry<String, JsonNode>> streamProps(JsonNode source) {
        return streamPropsExcluding(source, (JsonNodeReader[]) null);
    }

    default Stream<Entry<String, JsonNode>> streamPropsExcluding(JsonNode source, JsonNodeReader... excludingKeys) {
        if (source == null || !source.isObject()) {
            return Stream.of();
        }
        if (excludingKeys == null || excludingKeys.length == 0) {
            return source.properties().stream();
        }
        return source.properties().stream()
                .filter(e -> Arrays.stream(excludingKeys).noneMatch(s -> e.getKey().equalsIgnoreCase(s.name())));
    }

    /** {@link #createLink(String, Path, String, String)} with an empty title */
    default String createLink(String displayText, Path target, String anchor) {
        return createLink(displayText, target, anchor, null);
    }

    /**
     * Return a string with a markdown formatted link from the given components.
     *
     * @param displayText The display text to use for the link
     * @param target The target path that the link should point to
     * @param anchor An anchor to add to the link
     * @param title A title to use for the link
     */
    default String createLink(String displayText, Path target, String anchor, String title) {
        if (target == null) {
            throw new IllegalArgumentException("Can't create link with null path");
        }
        title = title != null ? "\"%s\"".formatted(title) : null;
        String targetPath = join("#", target.endsWith(".md") ? target : (target + ".md"), toAnchorTag(anchor));
        return "[%s](%s)".formatted(displayText, join(" ", targetPath, title))
                // Get rid of Windows-style path separators - they break links in Obsidian
                .replace('\\', '/');
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
        isReprinted,
        name,
        note,
        page,
        reprintedAs,
        source,
        tag,
        type,
        uid;

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
