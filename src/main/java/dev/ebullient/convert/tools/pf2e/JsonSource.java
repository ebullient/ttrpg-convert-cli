package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.NodeReader;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteNote;
import dev.ebullient.convert.tools.pf2e.qute.QuteInlineAbility;
import dev.ebullient.convert.tools.pf2e.qute.QuteInlineAffliction;
import dev.ebullient.convert.tools.pf2e.qute.QuteInlineAffliction.QuteAfflictionStage;

public interface JsonSource extends JsonTextReplacement {
    Pattern footnotePattern = Pattern.compile("\\{@footnote ([^}]+)}");

    /**
     * Collect and linkify traits from the specified node.
     *
     * @return an empty or sorted/linkified list of traits (never null)
     */
    default List<String> collectTraitsFrom(JsonNode sourceNode) {
        return Field.traits.getListOfStrings(sourceNode, tui()).stream()
                .sorted()
                .map(s -> linkify(Pf2eIndexType.trait, s))
                .collect(Collectors.toList());
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

    /**
     * Find and format footnotes referenced in the provided content
     *
     * @param text List of text lines (joined or not) that may contain footnotes
     * @param count The number of footnotes found previously (to avoid duplicates)
     * @return The number of footnotes found.
     */
    default int appendFootnotes(List<String> text, int count) {
        boolean pushed = parseState.push(true);
        try {
            List<String> footnotes = new ArrayList<>();
            for (int i = 0; i < text.size(); i++) {
                // "Footnote tags; allows a footnote to be embedded
                // {@footnote directly in text|This is primarily for homebrew purposes, as the official texts (so far) avoid using footnotes},
                // {@footnote optional reference information|This is the footnote. References are free text.|Footnote 1, page 20}.",
                text.set(i, footnotePattern.matcher(text.get(i))
                        .replaceAll((match) -> {
                            int index = count + footnotes.size() + 1;
                            String footnote = replaceText(match.group(1));
                            String[] parts = footnote.split("\\|");
                            footnotes.add(String.format("[^%s]: %s%s", index, parts[1],
                                    parts.length > 2 ? " (" + parts[2] + ")" : ""));

                            return String.format("%s[^%s]", parts[0], index);
                        }));
            }

            if (footnotes.size() > 0) {
                maybeAddBlankLine(text);
                footnotes.forEach(f -> text.add(replaceText(f)));

            }
            return count + footnotes.size();
        } finally {
            parseState.pop(pushed);
        }
    }

    /**
     * External (and recursive) entry point for content parsing.
     *
     * Parse attributes of the given node and add resulting lines
     * to the provided list.
     *
     * @param text Parsed content is appended to this list
     * @param node Textual, Array, or Object node containing content to parse/render
     * @param heading The current header depth and/or if headings are allowed for this text element
     */
    default void appendEntryToText(List<String> text, JsonNode node, String heading) {
        boolean pushed = parseState.push(node); // store state
        try {
            if (node == null || node.isNull()) {
                // do nothing
            } else if (node.isTextual()) {
                text.add(replaceText(node.asText()));
            } else if (node.isArray()) {
                node.elements().forEachRemaining(f -> {
                    maybeAddBlankLine(text);
                    appendEntryToText(text, f, heading);
                });
            } else if (node.isObject()) {
                appendEntryObjectToText(text, node, heading);
            } else {
                tui().errorf("Unknown entry type in %s: %s", getSources(), node.toPrettyString());
            }
        } finally {
            parseState.pop(pushed); // restore state
        }
    }

    /**
     * Embed rendered contents of the specified resource
     *
     * @param text List of text content should be added to
     * @param resource Pf2eQuteBase containing required template resource data
     * @param admonition Type of embedded/encapsulating admonition
     * @param prepend Text to prepend at beginning of admonition (e.g. title)
     */
    default void renderEmbeddedTemplate(List<String> text, Pf2eQuteBase resource, String admonition, List<String> prepend) {
        prepend = prepend == null ? List.of() : prepend; // ensure non-null
        boolean pushed = parseState.push((Pf2eSources) resource.sources());
        try {
            String rendered = tui().renderEmbedded(resource);
            List<String> inner = removePreamble(new ArrayList<>(
                    List.of(rendered.split("\n"))));

            String backticks = nestedEmbed(inner);
            maybeAddBlankLine(text);
            text.add(backticks + "ad-embed-" + admonition);
            prepend.forEach(l -> text.add(l));
            text.addAll(inner);
            text.add(backticks);
        } finally {
            parseState.pop(pushed);
        }
    }

    /** Internal */
    default void appendEntryObjectToText(List<String> text, JsonNode node, String heading) {
        AppendTypeValue type = AppendTypeValue.valueFrom(node, Field.type);
        String source = Field.source.getTextOrEmpty(node);

        // entriesOtherSource handled here.
        if (!source.isEmpty() && !cfg().sourceIncluded(source)) {
            if (!cfg().sourceIncluded(getSources().alternateSource())) {
                return;
            }
        }

        boolean pushed = parseState.push(node);
        try {
            if (type != null) {
                switch (type) {
                    case section:
                    case pf2h1:
                    case pf2h2:
                    case pf2h3:
                    case pf2h4:
                    case pf2h5:
                        appendTextHeaderBlock(text, node, heading);
                        break;
                    case pf2h1flavor:
                        appendTextHeaderFlavorBlock(text, node);
                        break;

                    // callout boxes
                    case pf2sidebar:
                        appendCallout(text, node, "pf2-sidebar");
                        break;
                    case pf2inset:
                        appendCallout(text, node, "pf2-inset");
                        break;
                    case pf2tipsBox:
                        appendCallout(text, node, "tip");
                        break;
                    case pf2sampleBox:
                        appendCallout(text, node, "example");
                        break;
                    case pf2beigeBox:
                        appendCallout(text, node, "pf2-beige");
                        break;
                    case pf2redBox:
                        appendCallout(text, node, "pf2-red");
                        break;
                    case pf2brownBox:
                        appendCallout(text, node, "pf2-brown");
                        break;
                    case pf2keyAbility:
                        appendCallout(text, node, "pf2-key-ability");
                        break;
                    case pf2keyBox:
                        appendCallout(text, node, "pf2-key-box");
                        break;

                    case pf2title:
                        appendTextHeaderBlock(text, node,
                                heading == null ? null : heading + "#");
                        break;

                    // lists & items

                    case pf2options:
                    case list:
                        appendList(text, Field.items.withArrayFrom(node));
                        break;
                    case item:
                        appendListItem(text, node);
                        break;
                    case entries:
                        appendEntryToText(text, Field.entries.getFrom(node), heading);
                        break;
                    case table:
                        appendTable(text, node);
                        break;
                    case paper:
                        appendPaper(text, node, "pf2-paper");
                        break;
                    case quote:
                        appendQuote(text, node);
                        break;

                    // special inline types
                    case ability:
                        appendAbility(text, node);
                        break;
                    case affliction:
                        appendAffliction(text, node);
                        break;
                    case data:
                        embedData(text, node);
                        break;
                    case lvlEffect:
                        appendLevelEffect(text, node);
                        break;
                    case successDegree:
                        appendSuccessDegree(text, node);
                        break;

                    default:
                        if (type != AppendTypeValue.entriesOtherSource) {
                            tui().errorf("TODO / How did I get here?: %s %s", type, node.toString());
                        }
                        appendEntryToText(text, Field.entry.getFrom(node), heading);
                        appendEntryToText(text, Field.entries.getFrom(node), heading);
                }
                // we had a type field! do nothing else
                return;
            }
            appendEntryToText(text, Field.entry.getFrom(node), heading);
            appendEntryToText(text, Field.entries.getFrom(node), heading);
        } finally {
            parseState.pop(pushed);
        }
    }

    /** Internal */
    default void appendTextHeaderBlock(List<String> text, JsonNode node, String heading) {
        String pageRef = parseState.sourceAndPage();

        if (heading == null) {
            List<String> inner = new ArrayList<>();
            appendEntryToText(inner, Field.entry.getFrom(node), null);
            appendEntryToText(inner, Field.entries.getFrom(node), null);
            if (prependField(node, Field.name, inner)) {
                maybeAddBlankLine(text);
            }
            text.addAll(inner);
        } else if (Field.name.existsIn(node)) {
            maybeAddBlankLine(text);
            text.add(heading + " " + Field.name.getTextOrEmpty(node) + pageRef);
            text.add("");
            appendEntryToText(text, Field.entry.getFrom(node), "#" + heading);
            appendEntryToText(text, Field.entries.getFrom(node), "#" + heading);
        } else {
            // headers always have names, but just in case..
            appendEntryToText(text, node.get("entries"), heading);
        }
    }

    /** Internal */
    default void appendTextHeaderFlavorBlock(List<String> text, JsonNode node) {
        List<String> inner = new ArrayList<>();
        inner.add("[!tip] " + Field.name.getTextOrEmpty(node));
        appendEntryToText(inner, Field.entries.getFrom(node), null);
        inner.forEach(x -> text.add("> " + x));
        maybeAddBlankLine(text);
    }

    /** Internal */
    default void appendList(List<String> text, ArrayNode itemArray) {
        maybeAddBlankLine(text);
        itemArray.forEach(e -> {
            List<String> item = new ArrayList<>();
            appendEntryToText(item, e, null);
            if (item.size() > 0) {
                prependText("- ", item);
                text.add(String.join("  \n    ", item)); // preserve line items
            }
        });
    }

    /** Internal */
    default void appendListItem(List<String> text, JsonNode itemNode) {
        List<String> inner = new ArrayList<>();
        appendEntryToText(inner, Field.entry.getFrom(itemNode), null);
        appendEntryToText(inner, Field.entries.getFrom(itemNode), null);
        if (prependField(itemNode, Field.name, inner)) {
            maybeAddBlankLine(text);
        }
        text.addAll(inner);
    }

    /** Internal */
    default void appendPaper(List<String> text, JsonNode paper, String callout) {
        List<String> paperText = new ArrayList<>();
        String title = Field.title.getTextOrDefault(paper, "A letter");

        paperText.add("[!" + callout + "] " + replaceText(title));

        appendEntryToText(paperText, Field.head.getFrom(paper), null);
        maybeAddBlankLine(paperText);
        appendEntryToText(paperText, Field.entry.getFrom(paper), null);
        maybeAddBlankLine(paperText);
        appendEntryToText(paperText, Field.signature.getFrom(paper), null);

        maybeAddBlankLine(text);
        paperText.forEach(x -> text.add("> " + x));
    }

    /** Internal */
    default void appendQuote(List<String> text, JsonNode entry) {
        List<String> quoteText = new ArrayList<>();
        String by = Field.by.getTextOrEmpty(entry);
        if (by.isEmpty()) {
            quoteText.add("[!quote]-  ");
        } else {
            quoteText.add("[!quote]- A quote from " + replaceText(by) + "  ");
        }
        appendEntryToText(quoteText, Field.entry.getFrom(entry), null);
        appendEntryToText(quoteText, Field.entries.getFrom(entry), null);

        String from = Field.by.getTextOrEmpty(entry);
        if (!from.isEmpty()) {
            maybeAddBlankLine(quoteText);
            quoteText.add("-- " + from);
        }

        maybeAddBlankLine(text);
        quoteText.forEach(x -> text.add("> " + x));
        maybeAddBlankLine(text);
    }

    /** Internal */
    default void appendCallout(List<String> text, JsonNode entry, String callout) {
        List<String> insetText = new ArrayList<>();
        String name = Field.name.getTextOrEmpty(entry);

        insetText.add("[!" + callout + "] " + replaceText(name));

        JsonNode autoReference = Field.recurs.getFieldFrom(entry, Field.auto);
        if (Field.auto.booleanOrDefault(Field.reference.getFrom(entry), false)) {
            String page = Field.page.getTextOrNull(entry);
            insetText.add(String.format("See %s%s",
                    page == null ? "" : "page " + page + " of ",
                    TtrpgConfig.sourceToLongName(Field.source.getTextOrEmpty(entry))));
        } else {
            appendEntryToText(insetText, Field.entry.getFrom(entry), null);
            appendEntryToText(insetText, Field.entries.getFrom(entry), null);
        }

        maybeAddBlankLine(text);
        insetText.forEach(x -> text.add("> " + x));
    }

    /** Internal */
    default void appendLevelEffect(List<String> text, JsonNode node) {
        maybeAddBlankLine(text);

        Field.entries.streamOf(node).forEach(e -> {
            String range = Field.range.getTextOrEmpty(e);
            prependTextMakeListItem(text, e, "**" + range + "** ");
        });
    }

    /** Internal */
    default void appendSuccessDegree(List<String> text, JsonNode node) {
        JsonNode entries = Field.entries.getFrom(node);

        List<String> inner = new ArrayList<>();
        inner.add("[!success-degree] ");

        JsonNode field = SuccessDegree.criticalSuccess.getFrom(entries);
        if (field != null) {
            prependTextMakeListItem(inner, field, "**Critical Success** ");
        }
        field = SuccessDegree.success.getFrom(entries);
        if (field != null) {
            prependTextMakeListItem(inner, field, "**Success** ");
        }
        field = SuccessDegree.failure.getFrom(entries);
        if (field != null) {
            prependTextMakeListItem(inner, field, "**Failure** ");
        }
        field = SuccessDegree.criticalFailure.getFrom(entries);
        if (field != null) {
            prependTextMakeListItem(inner, field, "**Critical Failure** ");
        }

        maybeAddBlankLine(text);
        inner.forEach(x -> text.add("> " + x));
    }

    /** Internal */
    default void appendAbility(List<String> text, JsonNode node) {
        String name = Field.name.getTextOrDefault(node, "Activate");
        Pf2eTypeReader.NumberUnitEntry jsonActivity = Pf2eTypeReader.Pf2eFeat.activity.fieldFromTo(node,
                Pf2eTypeReader.NumberUnitEntry.class, tui());

        List<String> abilityText = new ArrayList<>();
        appendEntryToText(abilityText, Field.entries.getFrom(node), null);

        AbilityField.note.debugIfExists(node, tui());
        AbilityField.range.debugIfExists(node, tui());

        QuteInlineAbility inlineAbility = new QuteInlineAbility(
                name, abilityText, List.of(), collectTraitsFrom(node),
                jsonActivity == null ? null : jsonActivity.toQuteActivity(this),
                AbilityField.components.replaceTextFrom(node, this),
                AbilityField.requirements.replaceTextFrom(node, this),
                AbilityField.cost.replaceTextFrom(node, this),
                AbilityField.trigger.replaceTextFrom(node, this),
                index().getFrequency(AbilityField.frequency.getFrom(node)),
                AbilityField.special.replaceTextFrom(node, this));

        renderInlineTemplate(text, inlineAbility, "ability");
    }

    /** Internal */
    default void appendAffliction(List<String> text, JsonNode node) {
        String name = Field.name.getTextOrNull(node);
        String level = null;

        String savingThrow = toTitleCase(AfflictionField.savingThrow.getTextOrEmpty(node));
        String dc = AfflictionField.DC.getTextOrNull(node);
        String savingThrowString = replaceText((dc == null ? "" : "DC " + dc + " ") + savingThrow);

        List<String> traits = collectTraitsFrom(node);
        List<String> tags = new ArrayList<>();

        JsonNode field = AfflictionField.level.getFrom(node);
        if (field != null) {
            level = "Level " + replaceText(field.asText());
            tags.add(cfg().tagOf("affliction", "level", level));
        }

        List<String> note = new ArrayList<>();
        appendEntryToText(note, AfflictionField.note.getFrom(node), null);

        List<String> effect = new ArrayList<>();
        appendEntryToText(effect, Field.entries.getFrom(node), null);

        Map<String, QuteAfflictionStage> stages = new LinkedHashMap<>();
        AfflictionField.stages.withArrayFrom(node).forEach(stageNode -> {
            String title = String.format("Stage %s",
                    AfflictionField.stage.getTextOrDefault(stageNode, "1"));

            List<String> stageInner = new ArrayList<>();
            appendEntryToText(stageInner, Field.entry.getFrom(stageNode), title);

            QuteAfflictionStage stage = new QuteAfflictionStage();
            stage.duration = replaceText(AfflictionField.duration.getTextOrNull(stageNode));
            stage.text = join("\n", stageInner);

            stages.put(title, stage);
        });

        QuteInlineAffliction inlineAffliction = new QuteInlineAffliction(
                name, note, tags, traits, level,
                replaceText(AfflictionField.maxDuration.getTextOrNull(node)),
                replaceText(AfflictionField.onset.getTextOrNull(node)),
                savingThrowString,
                join("\n", effect),
                stages);

        renderInlineTemplate(text, inlineAffliction, "affliction");
    }

    /** Internal */
    default void appendTable(List<String> text, JsonNode tableNode) {

        List<String> table = new ArrayList<>();

        String name = Field.name.getTextOrEmpty(tableNode);
        String id = Field.id.getTextOrEmpty(tableNode);

        String blockid = "";
        if (TableField.spans.getFrom(tableNode) != null) {
            blockid = appendHtmlTable(tableNode, table, id, name);
        } else {
            blockid = appendMarkdownTable(tableNode, table, id, name);
        }

        JsonNode intro = TableField.intro.getFrom(tableNode);
        if (intro != null) {
            maybeAddBlankLine(text);
            appendEntryToText(text, intro, null);
        }
        maybeAddBlankLine(text);

        text.addAll(table);
        if (!blockid.isEmpty()) {
            table.add("^" + blockid);
        }
        maybeAddBlankLine(text);
        JsonNode footnotes = Field.footnotes.getFrom(tableNode);
        if (footnotes != null) {
            maybeAddBlankLine(text);
            boolean pushed = parseState.push(true);
            appendEntryToText(text, footnotes, null);
            parseState.pop(pushed);
        }
        JsonNode outro = TableField.outro.getFrom(tableNode);
        if (outro != null) {
            maybeAddBlankLine(text);
            appendEntryToText(text, outro, null);
        }
    }

    /** Internal */
    default String appendHtmlTable(JsonNode tableNode, List<String> table, String id, String name) {
        ArrayNode rows = TableField.rows.withArrayFrom(tableNode);
        JsonNode colStyles = TableField.colStyles.getFrom(tableNode);
        int numCols = colStyles != null
                ? colStyles.size()
                : TableField.rows.streamOf(tableNode)
                        .map(x -> x.size())
                        .max(Integer::compare).get();

        ArrayNode spans = TableField.spans.withArrayFrom(tableNode);
        int spanIdx = 0;

        List<Integer> labelIdx = TableField.labelRowIdx.fieldFromTo(tableNode, Tui.LIST_INT, tui());
        if (labelIdx == null) {
            labelIdx = List.of(0);
        }

        String blockid = slugify(id);
        if (!name.isEmpty()) {
            blockid = slugify(name + " " + id);
        }

        table.add("<table>");
        for (int r = 0; r < rows.size(); r++) {
            ArrayNode rowNode = (ArrayNode) rows.get(r);
            int cols = rowNode.size(); // varies by row

            if (FieldValue.multiRow.isValueOfField(rowNode, Field.type)) {
                ArrayNode rows2 = TableField.rows.withArrayFrom(rowNode);
                List<List<String>> multicol = new ArrayList<>();
                for (int r2 = 0; r2 < rows2.size(); r2++) {
                    ArrayNode row = (ArrayNode) rows2.get(r2);
                    for (int c = 0; c < row.size(); c++) {
                        if (multicol.size() <= c) {
                            multicol.add(new ArrayList<>());
                        }
                        multicol.get(c).add(replaceHtmlText(row.get(c)));
                    }
                }
                table.add("<tr>");
                table.add("  <td>"
                        + multicol.stream()
                                .map(x -> String.join("</br>", x))
                                .collect(Collectors.joining("</td>\n  <td>"))
                        + "</td>");
                table.add("</tr>");
            } else if (cols != numCols) {
                String cellFormat = labelIdx.contains(r)
                        ? "  <th colspan=\"%s\">%s</th>"
                        : "  <td colspan=\"%s\">%s</td>";

                ArrayNode spanSizes = (ArrayNode) spans.get(spanIdx);
                int last = 0;
                table.add("<tr>");

                for (int i = 0; i < cols; i++) {
                    ArrayNode colSpan = (ArrayNode) spanSizes.get(i);
                    JsonNode cell = rowNode.get(i);

                    int start = colSpan.get(0).asInt();
                    int end = colSpan.get(1).asInt();

                    if (i == 0 && start > 1) {
                        table.add(String.format(cellFormat, start, ""));
                    } else {
                        table.add(String.format(cellFormat,
                                end - last, replaceHtmlText(cell)));
                    }
                    last = end;
                }
                table.add("</tr>");
                spanIdx++;
            } else {
                final String row;
                table.add("<tr>");
                if (labelIdx.contains(r)) {
                    table.add("  <th>"
                            + StreamSupport.stream(rowNode.spliterator(), false)
                                    .map(x -> replaceHtmlText(x))
                                    .collect(Collectors.joining("</th>\n  <th>"))
                            + "</th>");
                } else {
                    table.add("  <td>"
                            + StreamSupport.stream(rowNode.spliterator(), false)
                                    .map(x -> replaceHtmlText(x))
                                    .collect(Collectors.joining("</td>\n  <td>"))
                            + "</td>");
                }
                table.add("</tr>");
            }
        }
        table.add("</table>");

        return blockid;
    }

    /** Internal */
    default String replaceHtmlText(JsonNode cell) {
        return replaceText(cell.asText().trim())
                .replaceAll("\\n", "<br/>");
    }

    /** Internal */
    default String appendMarkdownTable(JsonNode tableNode, List<String> table, String id, String name) {
        ArrayNode rows = TableField.rows.withArrayFrom(tableNode);
        List<Integer> labelIdx = TableField.labelRowIdx.fieldFromTo(tableNode, Tui.LIST_INT, tui());

        String blockid = slugify(id);
        if (!name.isEmpty()) {
            blockid = slugify(name + " " + id);
        }

        if (labelIdx == null) {
            labelIdx = List.of(0);
        }

        for (int r = 0; r < rows.size(); r++) {
            JsonNode rowNode = rows.get(r);

            if (labelIdx.contains(r)) {
                String header = StreamSupport.stream(rowNode.spliterator(), false)
                        .map(x -> replaceText(x.asText()))
                        .collect(Collectors.joining(" | "));

                // make rollable dice headers
                header = "| " + header.replaceAll("^(d\\d+.*)", "dice: $1") + " |";

                if (r == 0 && blockid.isBlank()) {
                    blockid = slugify(header.replaceAll("d\\d+", "")
                            .replace("|", "")
                            .replaceAll("\\s+", " ")
                            .trim());
                } else if (r != 0) {
                    if (!blockid.isEmpty()) {
                        table.add("^" + blockid + "-" + r);
                    }
                    table.add("");
                }
                table.add(header);
                table.add(header.replaceAll("[^|]", "-"));
            } else if (FieldValue.multiRow.isValueOfField(rowNode, Field.type)) {
                TableField.rows.withArrayFrom(rowNode).forEach(mr -> {
                    String row = "| " +
                            StreamSupport.stream(mr.spliterator(), false)
                                    .map(x -> replaceText(x.asText()))
                                    .collect(Collectors.joining(" | "))
                            +
                            " |";
                    table.add(row);
                });
            } else {
                String row = "| " +
                        StreamSupport.stream(rowNode.spliterator(), false)
                                .map(x -> x.asText())
                                .map(x -> x.replaceAll("trait sweep$", "trait sweep}"))
                                .map(x -> x.replaceAll("group Knife]", "group Knife}"))
                                .map(x -> replaceText(x))
                                .collect(Collectors.joining(" | "))
                        +
                        " |";
                table.add(row);
            }
        }
        return blockid;
    }

    /** Internal */
    default void embedData(List<String> text, JsonNode dataNode) {
        String tag = Field.tag.getTextOrNull(dataNode);
        Pf2eIndexType dataType = Pf2eIndexType.fromText(tag);
        if (dataType == null) {
            tui().errorf("Unknown data type %s from: %s", tag, dataNode.toString());
            return;
        }

        JsonNode data = Field.data.getFrom(dataNode);
        if (data == null) {
            String name = Field.name.getTextOrNull(dataNode);
            String source = Field.source.getTextOrNull(dataNode);
            String link = linkify(dataType, name + "|" + source);
            if (dataType == Pf2eIndexType.creature) {
                link = link.replace(".md)", ".md#^statblock)");
            }
            maybeAddBlankLine(text);
            text.add("!" + link);
            maybeAddBlankLine(text);
        } else {
            Pf2eQuteBase converted = dataType.convertJson2QuteBase(index(), data);
            if (converted != null) {
                renderEmbeddedTemplate(text, converted, tag,
                        List.of(String.format("title: %s", converted.title()),
                                "collapse: closed"));
            } else {
                tui().errorf("Unable to process data for %s: %s", tag, dataNode.toString());
            }
        }
    }

    /** Internal */
    default void renderInlineTemplate(List<String> text, Pf2eQuteNote resource, String admonition) {
        String rendered = tui().renderEmbedded(resource);
        List<String> inner = List.of(rendered.split("\n"));

        String backticks = nestedEmbed(inner);
        maybeAddBlankLine(text);
        text.add(backticks + "ad-inline-" + admonition);
        text.addAll(inner);
        text.add(backticks);
    }

    /** Internal */
    default String nestedEmbed(List<String> content) {
        int embedDepth = content.stream()
                .filter(s -> s.matches("^`+$"))
                .map(String::length)
                .max(Integer::compare).orElse(2);
        char[] ticks = new char[embedDepth + 1];
        Arrays.fill(ticks, '`');
        return new String(ticks);
    }

    /** Internal */
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

    /** Internal */
    default void prependText(String prefix, List<String> inner) {
        if (inner.isEmpty()) {
            inner.add(prefix);
        } else {
            if (inner.get(0).isEmpty() && inner.size() > 1) {
                inner.set(1, prependText(prefix, inner.get(1)));
            } else {
                inner.set(0, prependText(prefix, inner.get(0)));
            }
        }
    }

    /** Internal */
    default String prependText(String prefix, String text) {
        return text.startsWith(prefix) ? text : prefix + text;
    }

    /** Internal */
    default boolean prependField(JsonNode entry, Field field, List<String> inner) {
        String n = field.getTextOrNull(entry);
        if (n != null) {
            if (inner.isEmpty()) {
                inner.add(n);
            } else {
                n = replaceText(n.trim().replace(":", ""));
                n = "**" + n + ".** ";
                inner.set(0, n + inner.get(0));
                return true;
            }
        }
        return false;
    }

    /** Internal */
    default void prependTextMakeListItem(List<String> text, JsonNode e, String prepend) {
        List<String> inner = new ArrayList<>();
        appendEntryToText(inner, e, null);
        if (inner.size() > 0) {
            prependText("- " + prepend, inner);
            text.add(String.join("  \n    ", inner).trim()); // preserve line items
        }
    }

    // Other context-constrained type values (not the big append loop)
    enum FieldValue implements NodeReader.FieldValue {
        multiRow;

        @Override
        public String value() {
            return this.name();
        }
    }

    enum SuccessDegree implements NodeReader {
        criticalSuccess("Critical Success"),
        success("Success"),
        failure("Failure"),
        criticalFailure("Critical Failure");

        final String nodeName;

        SuccessDegree(String nodeName) {
            this.nodeName = nodeName;
        }

        public String nodeName() {
            return nodeName;
        }
    }

    enum AbilityField implements NodeReader {
        activity,
        components,
        cost,
        creature,
        frequency,
        note,
        range,
        requirements,
        trigger,
        special;

        void debugIfExists(JsonNode node, Tui tui) {
            if (existsIn(node)) {
                tui.errorf(this.name() + " is defined in " + node.toPrettyString());
            }
        }
    }

    enum AfflictionField implements NodeReader {
        DC,
        duration,
        level,
        maxDuration,
        note,
        onset,
        savingThrow,
        stage,
        stages,
        temptedCurse,
        type,
    }

    enum TableField implements NodeReader {
        colStyles,
        intro,
        labelRowIdx,
        outro,
        rows,
        spans,
    }

    enum Field implements NodeReader {
        alias,
        auto,
        by,
        categories, // trait categories for indexing
        customUnit,
        data, // embedded data
        entry,
        entries,
        footnotes,
        freq, // inside frequency
        frequency,
        group,
        head,
        id,
        interval,
        items,
        name,
        number,
        overcharge,
        page,
        range, // level effect
        recurs,
        reference,
        requirements,
        signature,
        source,
        special,
        style,
        tag, // embedded data
        title,
        traits,
        type,
        unit,
        add_hash;
    }

    enum AppendTypeValue implements NodeReader.FieldValue {
        ability,
        affliction,
        attack,
        data,
        entries,
        entriesOtherSource,
        item,
        list,
        lvlEffect,
        paper,
        pf2beigeBox("pf2-beige-box"),
        pf2brownBox("pf2-brown-box"),
        pf2h1("pf2-h1"),
        pf2h1flavor("pf2-h1-flavor"),
        pf2h2("pf2-h2"),
        pf2h3("pf2-h3"),
        pf2h4("pf2-h4"),
        pf2h5("pf2-h5"),
        pf2inset("pf2-inset"),
        pf2keyBox("pf2-key-box"),
        pf2keyAbility("pf2-key-ability"),
        pf2options("pf2-options"),
        pf2redBox("pf2-red-box"),
        pf2sampleBox("pf2-sample-box"),
        pf2sidebar("pf2-sidebar"),
        pf2tipsBox("pf2-tips-box"),
        pf2title("pf2-title"),
        quote,
        section,
        successDegree,
        table;

        final String nodeValue;

        AppendTypeValue() {
            nodeValue = this.name();
        }

        AppendTypeValue(String nodeValue) {
            this.nodeValue = nodeValue;
        }

        public String value() {
            return this.nodeValue;
        }

        @Override
        public boolean matches(String value) {
            return this.value().equals(value) || this.name().equalsIgnoreCase(value);
        }

        static AppendTypeValue valueFrom(JsonNode source, Field field) {
            String textOrNull = field.getTextOrNull(source);
            if (textOrNull == null) {
                return null;
            }
            return Stream.of(AppendTypeValue.values())
                    .filter((t) -> t.matches(textOrNull))
                    .findFirst().orElse(null);
        }
    }
    // enum Type

}
