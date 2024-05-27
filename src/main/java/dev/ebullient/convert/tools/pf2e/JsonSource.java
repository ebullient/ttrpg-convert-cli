package dev.ebullient.convert.tools.pf2e;

import static dev.ebullient.convert.StringUtil.join;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.QuteUtil;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Json2QuteAbility.Pf2eAbility;
import dev.ebullient.convert.tools.pf2e.Json2QuteAffliction.Pf2eAffliction;
import dev.ebullient.convert.tools.pf2e.Json2QuteItem.Pf2eItem;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataActivity;

public interface JsonSource extends JsonTextReplacement {

    /**
     * Collect and linkify traits from the specified node.
     *
     * @param tags The tags to populate while collecting traits. If null, then don't populate any tags.
     *
     * @return an empty or sorted/linkified list of traits (never null)
     */
    default Set<String> collectTraitsFrom(JsonNode sourceNode, Tags tags) {
        return Field.traits.getListOfStrings(sourceNode, tui()).stream()
                .peek(tags == null ? t -> {
                } : t -> tags.add("trait", t))
                .sorted()
                .map(s -> linkify(Pf2eIndexType.trait, s))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * External (and recursive) entry point for content parsing.
     *
     * Parse attributes of the given node and add resulting lines
     * to the provided list.
     *
     * @param desc Parsed content is appended to this list
     * @param node Textual, Array, or Object node containing content to parse/render
     * @param heading The current header depth and/or if headings are allowed for this text element
     */
    @Override
    default void appendToText(List<String> text, JsonNode node, String heading) {
        boolean pushed = parseState().push(node); // store state
        try {
            if (node == null || node.isNull()) {
                // do nothing
            } else if (node.isTextual()) {
                text.add(replaceText(node.asText()));
            } else if (node.isArray()) {
                for (JsonNode f : iterableElements(node)) {
                    maybeAddBlankLine(text);
                    appendToText(text, f, heading);
                }
            } else if (node.isObject()) {
                appendObjectToText(text, node, heading);
            } else {
                tui().errorf("Unknown entry type in %s: %s", getSources(), node.toPrettyString());
            }
        } finally {
            parseState().pop(pushed); // restore state
        }
    }

    /** Internal */
    default void appendObjectToText(List<String> text, JsonNode node, String heading) {
        AppendTypeValue type = AppendTypeValue.valueFrom(node, SourceField.type);
        String source = SourceField.source.getTextOrEmpty(node);

        // entriesOtherSource handled here.
        if (!source.isEmpty() && !cfg().sourceIncluded(getSources())) {
            return;
        }

        boolean pushed = parseState().push(node);
        try {
            if (type != null) {
                switch (type) {
                    case section, pf2h1, pf2h2, pf2h3, pf2h4, pf2h5 -> appendTextHeaderBlock(text, node, heading);
                    case pf2h1flavor -> appendTextHeaderFlavorBlock(text, node);

                    // callout boxes
                    case pf2sidebar -> appendCallout(text, node, "pf2-sidebar");
                    case pf2inset -> appendCallout(text, node, "pf2-inset");
                    case pf2tipsBox -> appendCallout(text, node, "pf2-tip");
                    case pf2sampleBox -> appendCallout(text, node, "pf2-example");
                    case pf2beigeBox -> appendCallout(text, node, "pf2-beige");
                    case pf2redBox -> appendCallout(text, node, "pf2-red");
                    case pf2brownBox -> appendCallout(text, node, "pf2-brown");
                    case pf2keyAbility -> appendCallout(text, node, "pf2-key-ability");
                    case pf2keyBox -> appendCallout(text, node, "pf2-key-box");
                    case pf2title -> appendTextHeaderBlock(text, node,
                            heading == null ? null : heading + "#");

                    // lists & items

                    case pf2options, list -> appendList(text, SourceField.items.readArrayFrom(node));
                    case item -> appendListItem(text, node);
                    case entries -> appendToText(text, SourceField.entries.getFrom(node), heading);
                    case table -> appendTable(text, node);
                    case paper -> appendPaper(text, node, "pf2-paper");
                    case quote -> appendQuote(text, node);

                    // special inline types
                    case ability -> appendRenderable(text, Pf2eAbility.createEmbeddedAbility(node, this));
                    case affliction -> appendAffliction(text, node);
                    case attack -> appendRenderable(text, Pf2eJsonNodeReader.Pf2eAttack.createAttack(node, this));
                    case data -> embedData(text, node);
                    case lvlEffect -> appendLevelEffect(text, node);
                    case successDegree -> appendSuccessDegree(text, node);
                    default -> {
                        if (type != AppendTypeValue.entriesOtherSource) {
                            tui().errorf("TODO / How did I get here?: %s %s", type, node.toString());
                        }
                        appendToText(text, SourceField.entry.getFrom(node), heading);
                        appendToText(text, SourceField.entries.getFrom(node), heading);
                    }
                }
                // we had a type field! do nothing else
                return;
            }
            appendToText(text, SourceField.entry.getFrom(node), heading);
            appendToText(text, SourceField.entries.getFrom(node), heading);
        } catch (RuntimeException ex) {
            tui().errorf(ex, "Error [%s] occurred while parsing %s", ex.getMessage(), node.toString());
            throw ex;
        } finally {
            parseState().pop(pushed);
        }
    }

    /** Internal */
    default void appendTextHeaderBlock(List<String> text, JsonNode node, String heading) {
        String pageRef = parseState().sourcePageString("<sup>%s p. %s</sup>");

        if (heading == null) {
            List<String> inner = new ArrayList<>();
            appendToText(inner, SourceField.entry.getFrom(node), null);
            appendToText(inner, SourceField.entries.getFrom(node), null);
            if (prependField(node, SourceField.name, inner)) {
                maybeAddBlankLine(text);
            }
            text.addAll(inner);
        } else if (SourceField.name.existsIn(node)) {
            maybeAddBlankLine(text);
            // strip rendered links from the heading: linking to headings containing links is hard
            text.add(heading + " " + replaceText(SourceField.name.getTextOrEmpty(node))
                    .replaceAll("\\[(.*?)\\]\\(.*?\\)", "$1"));
            text.add(pageRef);
            appendToText(text, SourceField.entry.getFrom(node), "#" + heading);
            appendToText(text, SourceField.entries.getFrom(node), "#" + heading);
        } else {
            // headers always have names, but just in case..
            appendToText(text, SourceField.entries.getFrom(node), heading);
        }
    }

    /** Internal */
    default void appendTextHeaderFlavorBlock(List<String> text, JsonNode node) {
        List<String> inner = new ArrayList<>();
        inner.add("[!pf2-tip] " + SourceField.name.getTextOrEmpty(node));
        appendToText(inner, SourceField.entries.getFrom(node), null);
        inner.forEach(x -> text.add("> " + x));
        maybeAddBlankLine(text);
    }

    /** Internal */
    default void appendList(List<String> text, ArrayNode itemArray) {
        String indent = parseState().getListIndent();
        boolean pushed = parseState().indentList();
        try {
            maybeAddBlankLine(text);
            itemArray.forEach(e -> {
                List<String> item = new ArrayList<>();
                appendToText(item, e, null);
                if (item.size() > 0) {
                    prependText(indent + "- ", item);
                    text.add(String.join("  \n" + indent, item));
                }
            });
        } finally {
            parseState().pop(pushed);
        }
    }

    /** Internal */
    default void appendPaper(List<String> text, JsonNode paper, String callout) {
        List<String> paperText = new ArrayList<>();
        String title = Field.title.getTextOrDefault(paper, "A letter");

        paperText.add("[!" + callout + "] " + replaceText(title));

        appendToText(paperText, Field.head.getFrom(paper), null);
        maybeAddBlankLine(paperText);
        appendToText(paperText, SourceField.entry.getFrom(paper), null);
        maybeAddBlankLine(paperText);
        appendToText(paperText, Field.signature.getFrom(paper), null);

        maybeAddBlankLine(text);
        paperText.forEach(x -> text.add("> " + x));
    }

    /** Internal */
    default void appendQuote(List<String> text, JsonNode entry) {
        List<String> quoteText = new ArrayList<>();
        String by = Field.by.getTextOrEmpty(entry);
        if (by.isEmpty()) {
            quoteText.add("[!pf2-quote]-  ");
        } else {
            quoteText.add("[!pf2-quote]- A quote from " + replaceText(by) + "  ");
        }
        appendToText(quoteText, SourceField.entry.getFrom(entry), null);
        appendToText(quoteText, SourceField.entries.getFrom(entry), null);

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
        String name = SourceField.name.getTextOrEmpty(entry);

        insetText.add("[!" + callout + "] " + replaceText(name));

        // TODO
        JsonNode autoReference = Field.recurs.getFieldFrom(entry, Field.auto);
        if (Field.auto.booleanOrDefault(Field.reference.getFrom(entry), false)) {
            String page = SourceField.page.getTextOrEmpty(entry);
            insetText.add(String.format("See %s%s",
                    page == null ? "" : "page " + page + " of ",
                    TtrpgConfig.sourceToLongName(SourceField.source.getTextOrEmpty(entry))));
        } else {
            appendToText(insetText, SourceField.entry.getFrom(entry), "##");
            appendToText(insetText, SourceField.entries.getFrom(entry), "##");
        }

        maybeAddBlankLine(text);
        insetText.forEach(x -> text.add("> " + x));
    }

    /** Internal */
    default void appendLevelEffect(List<String> text, JsonNode node) {
        maybeAddBlankLine(text);

        SourceField.entries.streamFrom(node).forEach(e -> {
            String range = Field.range.getTextOrEmpty(e);
            prependTextMakeListItem(text, e, "**" + range + "** ", "    ");
        });
    }

    /** Internal */
    default void appendSuccessDegree(List<String> text, JsonNode node) {
        JsonNode entries = SourceField.entries.getFrom(node);
        String continuation = "   "; // properly 4, but we add space with >
        List<String> inner = new ArrayList<>();
        inner.add("[!success-degree] ");

        JsonNode field = SuccessDegree.criticalSuccess.getFrom(entries);
        if (field != null) {
            prependTextMakeListItem(inner, field, "**Critical Success** ", continuation);
        }
        field = SuccessDegree.success.getFrom(entries);
        if (field != null) {
            prependTextMakeListItem(inner, field, "**Success** ", continuation);
        }
        field = SuccessDegree.failure.getFrom(entries);
        if (field != null) {
            prependTextMakeListItem(inner, field, "**Failure** ", continuation);
        }
        field = SuccessDegree.criticalFailure.getFrom(entries);
        if (field != null) {
            prependTextMakeListItem(inner, field, "**Critical Failure** ", continuation);
        }

        maybeAddBlankLine(text);
        inner.forEach(x -> text.add(parseState().getListIndent()
                + (x.isBlank() ? ">" : "> ")
                + x));
    }

    /** Internal */
    default void appendAffliction(List<String> text, JsonNode node) {
        appendRenderable(text, Pf2eAffliction.createInlineAffliction(node, this));
    }

    /** Internal */
    private void appendRenderable(List<String> text, QuteUtil.Renderable renderable) {
        text.addAll(List.of(renderable.render().split("\n")));
    }

    /** Internal */
    default void appendTable(List<String> text, JsonNode tableNode) {
        boolean pushed = parseState().push(tableNode);
        try {
            List<String> table = new ArrayList<>();

            String name = SourceField.name.getTextOrEmpty(tableNode);
            String id = SourceField.id.getTextOrEmpty(tableNode);

            String blockid;
            if (TableField.spans.getFrom(tableNode) != null || tableNode.toString().contains("multiRow")) {
                blockid = appendHtmlTable(tableNode, table, id, name);
            } else {
                blockid = appendMarkdownTable(tableNode, table, id, name);
            }

            JsonNode intro = TableField.intro.getFrom(tableNode);
            if (intro != null) {
                maybeAddBlankLine(text);
                appendToText(text, intro, null);
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
                boolean pushFoot = parseState().pushFootnotes(true);
                try {
                    appendToText(text, footnotes, null);
                } finally {
                    parseState().pop(pushFoot);
                }
            }
            JsonNode outro = TableField.outro.getFrom(tableNode);
            if (outro != null) {
                maybeAddBlankLine(text);
                appendToText(text, outro, null);
            }
        } finally {
            parseState().pop(pushed);
        }
    }

    /** Internal */
    default String appendHtmlTable(JsonNode tableNode, List<String> table, String id, String name) {
        boolean pushed = parseState().pushHtmlTable(true);
        try {
            ArrayNode rows = TableField.rows.readArrayFrom(tableNode);
            JsonNode colStyles = TableField.colStyles.getFrom(tableNode);
            int numCols = colStyles != null
                    ? colStyles.size()
                    : TableField.rows.streamFrom(tableNode)
                            .map(JsonNode::size)
                            .max(Integer::compare).get();

            ArrayNode spans = TableField.spans.readArrayFrom(tableNode);
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
                JsonNode rowNode = rows.get(r);
                int cols = rowNode.size(); // varies by row

                if (FieldValue.multiRow.isValueOfField(rowNode, SourceField.type)) {
                    ArrayNode rows2 = TableField.rows.readArrayFrom(rowNode);
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
                                    .map(x -> String.join("<br />", x))
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
                    table.add("<tr>");
                    if (labelIdx.contains(r)) {
                        table.add("  <th>" + streamOf(rowNode)
                                .map(this::replaceHtmlText)
                                .collect(Collectors.joining("</th>\n  <th>"))
                                + "</th>");
                    } else {
                        table.add("  <td>" + streamOf(rowNode)
                                .map(this::replaceHtmlText)
                                .collect(Collectors.joining("</td>\n  <td>"))
                                + "</td>");
                    }
                    table.add("</tr>");
                }
            }
            table.add("</table>");

            return blockid;
        } finally {
            parseState().pop(pushed);
        }
    }

    /** Internal */
    default String replaceHtmlText(JsonNode cell) {
        return replaceText(cell.asText().trim()
                .replaceAll("\\n", "<br/>"));
    }

    /** Internal */
    default String replaceMarkdownTableText(JsonNode cell) {
        return replaceText(cell.asText().trim());
    }

    /** Internal */
    default String appendMarkdownTable(JsonNode tableNode, List<String> table, String id, String name) {
        boolean pushed = parseState().pushMarkdownTable(true);
        try {
            ArrayNode rows = TableField.rows.readArrayFrom(tableNode);
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
                    String header = streamOf(rowNode)
                            .map(x -> replaceMarkdownTableText(x))
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
                } else {
                    String row = "| " + streamOf(rowNode)
                            .map(x -> replaceMarkdownTableText(x))
                            .collect(Collectors.joining(" | "))
                            + " |";
                    table.add(row);
                }
            }
            return blockid;
        } finally {
            parseState().pop(pushed);
        }
    }

    /** Internal */
    default void embedData(List<String> text, JsonNode dataNode) {
        String tag = Field.tag.getTextOrEmpty(dataNode);
        JsonNode data = Field.data.getFrom(dataNode);
        Pf2eIndexType dataType = Pf2eIndexType.fromText(tag);

        if ("generic".equals(tag)) {
            List<String> inner = embedGenericData(tag, data);
            maybeAddBlankLine(text);
            wrapAdmonition(inner, "pf2-note");
            text.addAll(inner);
            return;
        } else if (dataType == null) {
            tui().errorf("Unknown data type %s from: %s", tag, dataNode.toString());
            return;
        }

        if (data == null) {
            String name = SourceField.name.getTextOrEmpty(dataNode);
            String source = SourceField.source.getTextOrEmpty(dataNode);
            String link = linkify(dataType, name + "|" + source);
            if (dataType == Pf2eIndexType.creature) {
                link = link.replace(".md)", ".md#^statblock)");
            }
            maybeAddBlankLine(text);
            text.add("!" + link);
            maybeAddBlankLine(text);
            return;
        }

        // If this is a self-renderable type, then the admonition may be already included.
        // (This might be the case anyway, but we know it probably is the case with these).
        // So try to get the renderable embedded object first, and then add the collapsed
        // tag to the outermost admonition.
        QuteUtil.Renderable renderable = switch (dataType) {
            case ability -> Pf2eAbility.createEmbeddedAbility(data, this);
            case affliction, curse, disease -> Pf2eAffliction.createInlineAffliction(data, this);
            default -> null;
        };
        if (renderable != null) {
            List<String> renderedData = new ArrayList<>();
            appendRenderable(renderedData, renderable);
            // Make the outermost admonition collapsed, if there is one
            int[] adIndices = outerAdmonitionIndices(renderedData);
            if (adIndices != null) {
                int adStartIdx = adIndices[0];
                renderedData.add(adStartIdx + 1, "collapse: closed");
            }
            text.addAll(renderedData);
            return;
        }

        // Otherwise, if it's not a self-renderable type, then we fall back to renderEmbeddedTemplate
        // and add the collapsible admonition ourselves
        Pf2eQuteBase converted = dataType.convertJson2QuteBase(index(), data);
        if (converted != null) {
            renderEmbeddedTemplate(text, converted, tag,
                    List.of(String.format("title: %s", converted.title()),
                            "collapse: closed"));
        } else {
            tui().errorf("Unable to process data for %s: %s", tag, dataNode.toString());
        }
    }

    default List<String> embedGenericData(String tag, JsonNode data) {
        List<String> text = new ArrayList<>();
        boolean pushed = parseState().push(data);
        try {
            QuteDataActivity activity = Pf2eItem.activity.getActivityFrom(data, this);

            String title = SourceField.name.getTextOrEmpty(data);
            if (activity != null) {
                title += " " + activity;
            }

            String category = Pf2eItem.category.getTextOrNull(data);
            String level = Pf2eItem.level.getTextOrNull(data);
            if (category != null || level != null) {
                title += String.format(" *%s%s%s*",
                        category == null ? "" : category,
                        (category != null && level == null) ? "" : " ",
                        level == null ? "" : level);
            }

            text.add("title: " + title);

            // Add traits
            Tags tags = new Tags();
            Collection<String> traits = collectTraitsFrom(data, tags);
            text.add(join("  ", traits) + "  ");
            maybeAddBlankLine(text);

            // Add rendered sections
            data.get("sections").forEach(section -> {
                boolean undefinedTypeText = streamOf(section)
                        .anyMatch(x -> !x.isTextual() && !SourceField.type.existsIn(x));
                if (undefinedTypeText) {
                    section.forEach(x -> {
                        if (x.isObject() && !SourceField.type.existsIn(x)) {
                            appendToText(text, x, null);
                        } else {
                            appendToText(text, x, "##");
                        }
                    });
                } else {
                    appendToText(text, section, "##");
                }
            });

            return text;
        } finally {
            parseState().pop(pushed);
        }
    }

    // Other context-constrained type values (not the big append loop)
    enum FieldValue implements Pf2eJsonNodeReader.FieldValue {
        multiRow;

        @Override
        public String value() {
            return this.name();
        }
    }

    enum SuccessDegree implements Pf2eJsonNodeReader {
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

    enum TableField implements Pf2eJsonNodeReader {
        colStyles,
        intro,
        labelRowIdx,
        outro,
        rows,
        spans,
    }

    enum AppendTypeValue implements Pf2eJsonNodeReader.FieldValue {
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

        static AppendTypeValue valueFrom(JsonNode source, JsonNodeReader field) {
            String textOrNull = field.getTextOrEmpty(source);
            if (textOrNull.isEmpty()) {
                return null;
            }
            return Stream.of(AppendTypeValue.values())
                    .filter((t) -> t.matches(textOrNull))
                    .findFirst().orElse(null);
        }
    }
    // enum Type

}
